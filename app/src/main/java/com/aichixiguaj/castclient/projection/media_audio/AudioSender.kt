package com.aichixiguaj.castclient.projection.media_audio

import android.media.projection.MediaProjection
import com.aichixiguaj.castclient.projection.ProjectionSender

class AudioSender(
    var projectionSender: ProjectionSender?,
    var mediaProjection: MediaProjection?
) {

    private val audioEncoder = AudioEncoder(::encodeListener, mediaProjection)

    private fun encodeListener(bytes: ByteArray) {
        projectionSender?.putAudioPack(bytes)
    }

    fun close() {
        audioEncoder.stopEncode()
    }
}