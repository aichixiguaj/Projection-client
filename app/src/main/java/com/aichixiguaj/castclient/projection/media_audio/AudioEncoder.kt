package com.aichixiguaj.castclient.projection.media_audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean

class AudioEncoder(
    private var encodeListener: ((bytes: ByteArray) -> Unit)?,
    mediaProjection: MediaProjection?
) : AudioRecorder(mediaProjection) {

    private var mediaFormat: MediaFormat
    private var audioCodec: MediaCodec? = null

    private val audioType = MediaFormat.MIMETYPE_AUDIO_AAC

    private var encodeJob: Job? = null

    private val bufferInfo = MediaCodec.BufferInfo()

    private var currentTime = SystemClock.elapsedRealtime()

    private val isStop = AtomicBoolean(false)

    private val channel = 2
    private val bitRate = 192000

    init {
        mediaFormat = MediaFormat.createAudioFormat(audioType, sampleRate, channel).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        }

        audioCodec = MediaCodec.createEncoderByType(audioType).apply {
            configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }

        startEncode()
    }

    private fun startEncode() {
        encodeJob?.cancel()
        encodeJob = CoroutineScope(Dispatchers.Default).launch {

            audioCodec?.start()
            audioRecord?.startRecording()
            currentTime = SystemClock.elapsedRealtime()

            while (!isStop.get()) {
                try {
                    audioCodec?.let {
                        val inputBufferId = it.dequeueInputBuffer(10000L)
                        if (inputBufferId >= 0) {
                            val inputBuffer = it.getInputBuffer(inputBufferId)!!
                            val byteArray = ByteArray(inputBuffer.capacity())
                            val readByteSize = audioRecord?.read(byteArray, 0, byteArray.size)

                            if (readByteSize != null && readByteSize > 0) {
                                inputBuffer.put(byteArray, 0, readByteSize)
                                it.queueInputBuffer(
                                    inputBufferId, 0, readByteSize, System.nanoTime() / 1000, 0
                                )
                                encodeListener?.invoke(byteArray)
                            }
                        }

                        val outputBufferId = it.dequeueOutputBuffer(bufferInfo, 10000L)
                        if (outputBufferId >= 0) {
                            val outputBuffer = it.getOutputBuffer(outputBufferId)
                            if (bufferInfo.size > 1) {
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                    outputBuffer?.let { buffer ->
//                                        val data =
//                                            ByteArray(buffer.remaining()).also { byteArray ->
//                                                buffer.get(byteArray)
//                                            }

//                                            val list = data.toMutableList().apply {
//                                                add(0, 'a'.code.toByte())
//                                            }
//                                            socketManager.sendData(list.toByteArray())
                                    }
                                }
                            }
                            it.releaseOutputBuffer(outputBufferId, false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TAG", "处理音频帧数据异常：${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopEncode() {
        isStop.set(true)
        encodeJob?.cancel()
        stopRecord()
        audioCodec?.apply {
            stop()
            release()
        }
        encodeListener = null
        audioCodec = null
    }
}