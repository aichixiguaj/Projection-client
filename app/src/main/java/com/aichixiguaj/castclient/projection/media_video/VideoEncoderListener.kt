package com.aichixiguaj.castclient.projection.media_video


interface VideoEncoderListener {

    fun onH264(buffer: ByteArray)

    fun onError(t:Throwable)

    fun close()

}