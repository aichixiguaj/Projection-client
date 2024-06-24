package com.aichixiguaj.castclient.projection.media_video

import android.media.projection.MediaProjection
import android.util.Log
import com.aichixiguaj.castclient.projection.ProjectionSender

class VideoSender(
    var projectionSender: ProjectionSender?, mp: MediaProjection?, width: Int, height: Int,
) : VideoEncoderListener {

    private var videoReader: VideoReader = VideoReader(width, height, this, mp)

    init {
        videoReader.init()
        videoReader.startEncoderJob()
    }

    override fun onH264(buffer: ByteArray) {
        projectionSender?.putVideoPack(buffer)
    }

    override fun onError(t: Throwable) {

    }

    override fun close() {
        videoReader.close()
    }

}