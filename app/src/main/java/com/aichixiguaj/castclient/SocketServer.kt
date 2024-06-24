package com.aichixiguaj.castclient

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

/**
 *    @author   ： AiChiXiGuaJ
 *    @date      ： 2023/8/22 14:38
 *    @email    ： aichixiguaj@qq.com
 *    @desc     :
 */
class SocketServer(add: InetSocketAddress) : WebSocketServer(add) {

    companion object {

        private const val TAG = "SocketServer"

    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        Log.e(
            TAG,
            "onOpen ${conn?.remoteSocketAddress?.address}:${conn?.remoteSocketAddress?.port}"
        )
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        Log.e(TAG, "onClose：${conn?.remoteSocketAddress?.address} $code  $reason $remote")
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        Log.e(TAG, "onMessage: ${conn?.remoteSocketAddress?.address} : $message")
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.e(TAG, "onError = ${conn?.remoteSocketAddress?.address} :" + ex.toString())
    }

    override fun onStart() {
        Log.e(TAG, "onStart")
    }

    fun sendData(array: ByteArray) {
        Log.e(TAG, "发送了数据${array.size}")
        broadcast(array)
    }

}