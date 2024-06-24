package com.aichixiguaj.castclient.projection.media_audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection

@Suppress("MemberVisibilityCanBePrivate")
@SuppressLint("MissingPermission")
open class AudioRecorder(
    private val mediaProjection: MediaProjection?,
) {

    protected var audioCapture: AudioPlaybackCaptureConfiguration? = null

    protected var audioRecord: AudioRecord? = null
        private set

    protected var bufferSize = 0

    val sampleRate = 44100

    val channelConfig = AudioFormat.CHANNEL_IN_STEREO

    val encoding = AudioFormat.ENCODING_PCM_16BIT

    protected var audioFormat: AudioFormat? = AudioFormat.Builder().apply {
        setEncoding(encoding)
        setSampleRate(sampleRate)
        setChannelMask(channelConfig)
    }.build()

    init {
        initAudioRecord()
    }

    @SuppressLint("NewApi")
    private fun initAudioRecord() {
        if (bufferSize == 0) {
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        }

        if (audioCapture == null) {
            mediaProjection?.let {
                audioCapture = AudioPlaybackCaptureConfiguration.Builder(it).apply {
                    addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    addMatchingUsage(AudioAttributes.USAGE_GAME)
                    addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                }.build()

                if (audioRecord == null) {
                    audioRecord = AudioRecord.Builder().apply {
                        setAudioPlaybackCaptureConfig(audioCapture!!)
                        setAudioFormat(audioFormat!!)
                    }.build()
                }
            }
        }
    }

    fun stopRecord() {
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

}