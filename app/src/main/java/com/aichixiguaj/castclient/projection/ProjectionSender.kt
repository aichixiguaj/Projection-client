package com.aichixiguaj.castclient.projection

import android.util.Log
import com.aichixiguaj.castclient.SocketServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

@Suppress("DeferredResultUnused")
class ProjectionSender {

    private var videoSocketServer: SocketServer? = null
    private var audioSocketServer: SocketServer? = null

    private var socketJob: Job? = null

    fun startJob(videoPort: Int, audioPort: Int) {
        socketJob?.cancel()
        videoSocketServer = initSocketServer(videoPort)
        audioSocketServer = initSocketServer(audioPort)

        socketJob = CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                async {
                    startSocket(videoSocketServer!!)
                }
                async {
                    startSocket(audioSocketServer!!)
                }
            }.onFailure {
                Log.e("TAG", "发送数据出现错误${it.message}")
            }
        }
    }

    private fun initSocketServer(port: Int): SocketServer {
        return SocketServer(InetSocketAddress(port))
    }

    private fun startSocket(socketServer: SocketServer) {
        socketServer.apply {
            isReuseAddr = true
            run()
        }
    }

    fun putAudioPack(audioArray: ByteArray) {
        Log.e("TAG", "Has send audio data：${audioArray.size}")
        audioSocketServer?.sendData(audioArray)
    }

    fun putVideoPack(videoArray: ByteArray) {
        videoSocketServer?.sendData(videoArray)
    }

    fun close() {
        socketJob?.cancel()
        socketJob = null

        CoroutineScope(Dispatchers.IO).launch {
            async {
                videoSocketServer = null
            }
            async {
                videoSocketServer = null
            }
        }

    }

}