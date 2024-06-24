package com.aichixiguaj.castclient.projection.media_video

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.util.Log
import android.view.Surface

/**
 * Created by Lesa on 2018/12/03.
 */
class VideoReader(//方向宽度
    protected var mWidth: Int, //方向高度
    protected var mHeight: Int,
    videoEncoderListener: VideoEncoderListener?,
    private val mMediaProjection: MediaProjection?
) : VideoEncoder(mWidth, mHeight,  videoEncoderListener) {

    companion object {
        private const val TAG = "MediaReader"
    }

    private val mDpi = 1
    private var mSurface: Surface? = null
    private var mVirtualDisplay: VirtualDisplay? = null

    override fun init() {
        super.init()
        initVirtualDisplay()
    }

    private fun initVirtualDisplay() {
        mSurface = super.getSurface()
        mVirtualDisplay = mMediaProjection?.createVirtualDisplay(
            "$TAG-display",
            mWidth, mHeight, mDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
            mSurface, null, null
        )
        Log.d(TAG, "created virtual display: $mVirtualDisplay")
    }

    override fun close() {
        super.close()
        mVirtualDisplay?.release()
        mSurface?.release()
    }
}