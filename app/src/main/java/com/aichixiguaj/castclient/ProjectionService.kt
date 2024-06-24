package com.aichixiguaj.castclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import com.aichixiguaj.castclient.projection.ProjectionSender
import com.aichixiguaj.castclient.projection.media_audio.AudioSender
import com.aichixiguaj.castclient.projection.media_video.VideoSender


class ProjectionService : Service() {

    companion object {

        private const val NOTIFICATION_CHANNEL_ID = "projection_service"

        private const val VIDEO_HEIGHT = 1080
        private const val VIDEO_WIDTH = 1920

        var isStarted = false
            private set

        var mediaProjection: MediaProjection? = null
    }

    private val NOTIFICATION_ID_ICON = 1

    private var projectionSender: ProjectionSender? = null
    private var videoSender: VideoSender? = null
    private var audioSender: AudioSender? = null

    private var videoPort = 52225
    private var audioPort = 51115

    private lateinit var mMediaProjectionManager: MediaProjectionManager

    override fun onCreate() {
        super.onCreate()
        mMediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val extras = intent?.extras
        if (extras != null) {
            val resultCode = (extras.get("code") ?: -1) as Int
            val resultData = intent.getParcelableExtra<Intent>("data")
            if (resultData != null) {
                mediaProjection =
                    mMediaProjectionManager.getMediaProjection(resultCode, resultData)
                isStarted = true
                startSendServer()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        stopProjectionServer()
        super.onDestroy()
        Log.e("TAG", "服务被销毁")
    }

    private fun startSendServer() {
        projectionSender = ProjectionSender()
        projectionSender?.startJob(videoPort, audioPort)

        try {
            videoSender = VideoSender(
                projectionSender,
                mediaProjection,
                VIDEO_WIDTH,
                VIDEO_HEIGHT
            )

            audioSender = AudioSender(
                projectionSender,
                mediaProjection
            )
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            return
        }
    }

    private fun stopProjectionServer() {
        projectionSender?.close()
        videoSender?.close()
        audioSender?.close()
        mediaProjection?.stop()
        deleteNotification()
        isStarted = false
    }

    private fun deleteNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID_ICON)
    }

    private fun createNotificationChannel() {
        val builder = Notification.Builder(this.applicationContext) //获取一个Notification构造器
        val nfIntent = Intent(this, MainActivity::class.java) //点击后跳转的界面，可以设置跳转数据
        builder.setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                nfIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        ).setLargeIcon(
            BitmapFactory.decodeResource(
                this.resources,
                R.mipmap.ic_launcher
            )
        ) // 设置下拉列表中的图标(大图标)
            .setContentTitle(getString(R.string.app_name)) // 设置下拉列表里的标题
            .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
            .setContentText("投屏运行中") // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_CHANNEL_ID)

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "投屏服务",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = builder.build() // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND //设置为默认的声音
        startForeground(110, notification)
    }
}