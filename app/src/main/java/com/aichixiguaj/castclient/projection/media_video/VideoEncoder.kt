package com.aichixiguaj.castclient.projection.media_video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Lesa on 2018/12/03.
 */
open class VideoEncoder(
    private val videoW: Int,
    private val videoH: Int,
    private var videoEncoderListener: VideoEncoderListener?
) {

    companion object {
        private const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC

        // 比特率
        private const val VIDEO_BITRATE = 4000 * 1000

        // 帧率
        private const val VIDEO_FRAME_RATE = 30
    }

    private var mediaCodec: MediaCodec? = null
    private var mSurface: Surface? = null
    private val mBufferInfo = MediaCodec.BufferInfo()

    private var mPpsSps: ByteArray? = null

    private var lastKeyFrameTime: Long = 0L

    private val mKeyFrameTimeMs = 1 * 1000

    private var encoderJob: Job? = null

    private val isStop = AtomicBoolean(false)

    open fun init() {
        initMediaCodec()
    }

    private fun initMediaCodec() {
        val format = MediaFormat.createVideoFormat(MIME, videoW, videoH).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            ) //颜色格式
            setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_FRAME_RATE * 3)
            setInteger(
                MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
            )
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
            setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000 / VIDEO_FRAME_RATE)
        }

        lastKeyFrameTime = System.currentTimeMillis()

        mediaCodec = MediaCodec.createEncoderByType(MIME)
        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mSurface = mediaCodec?.createInputSurface()
        mediaCodec?.start()
    }

    fun startEncoderJob() {
        encoderJob?.cancel()
        encoderJob = CoroutineScope(Dispatchers.IO).launch {
            while (!isStop.get()) {
                try {
                    encodeEvent(System.currentTimeMillis())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun encodeEvent(systemTimeNow: Long) {
        val outputBufferIndex = mediaCodec?.dequeueOutputBuffer(mBufferInfo, 10000L)
        if (outputBufferIndex == null || outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // 无数据，或者请求超时
        } else if (outputBufferIndex >= 0) {
            // 有效输出
            val outputBuffer = mediaCodec?.getOutputBuffer(outputBufferIndex)
            var outData = ByteArray(mBufferInfo.size)
            outputBuffer?.get(outData)

            //记录pps和sps
            val type = outData[4].toInt() and 0x07
            if (type in 7..8) {
                mPpsSps = ByteArray(outData.size)
                mPpsSps = outData
            } else if (type == 5) {
                //在关键帧前面加上pps和sps数据
                if (mPpsSps != null) {
                    val iframeData = ByteArray(mPpsSps!!.size + outData.size)
                    System.arraycopy(mPpsSps!!, 0, iframeData, 0, mPpsSps!!.size)
                    System.arraycopy(outData, 0, iframeData, mPpsSps!!.size, outData.size)
                    outData = iframeData
                    videoEncoderListener?.onH264(outData)
                }
                //收到一个关键帧，重置关键帧时间
                lastKeyFrameTime = systemTimeNow
            } else if (type == 1) {
                //将当前视频帧与之前存储的PPS和SPS数据合并为一个I帧数据
                if (mPpsSps != null) {
                    val iframeData = ByteArray(mPpsSps!!.size + outData.size)
                    System.arraycopy(mPpsSps!!, 0, iframeData, 0, mPpsSps!!.size)
                    System.arraycopy(outData, 0, iframeData, mPpsSps!!.size, outData.size)
                    outData = iframeData
                    videoEncoderListener?.onH264(outData)
                }
            }

            // 定时请求关键帧
            if (System.currentTimeMillis() - lastKeyFrameTime > mKeyFrameTimeMs) {
                requestKeyFrame()
                lastKeyFrameTime = systemTimeNow
            }

            mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
        }
    }

    private fun requestKeyFrame() {
        mediaCodec?.let {
            val param = Bundle()
            param.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            it.setParameters(param)
        }
    }

    fun getSurface(): Surface? {
        return mSurface
    }

    open fun close() {
        isStop.set(true)
        runCatching {
            mediaCodec?.stop()
        }.onFailure {
            Log.d("TAG", "mediaCodec 已经关闭")
        }
        mediaCodec?.release()
        encoderJob?.cancel()
        videoEncoderListener = null
    }


}