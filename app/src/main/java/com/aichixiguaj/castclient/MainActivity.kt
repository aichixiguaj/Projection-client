package com.aichixiguaj.castclient

import android.animation.ValueAnimator
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

class MainActivity : AppCompatActivity() {

    companion object {
        var SCREEN_WIDTH = 1920
        var SCREEN_HEIGHT = 1080
        var SCREEN_DPI = 1
    }

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val contract = ActivityResultContracts.StartActivityForResult()

    private lateinit var screenServiceIntent: Intent

    private var port = 50001

    private val callback = ActivityResultCallback<ActivityResult>() {
        if (it.resultCode == RESULT_OK) {
            screenServiceIntent.apply {
                putExtra("code", it.resultCode)
                putExtra("data", it.data)
                putExtra("port", port)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(screenServiceIntent)
            } else {
                startService(screenServiceIntent)
            }
        }
    }

    private val mActivityBLauncher = registerForActivityResult(contract, callback)

    private var animTV: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        animTV = findViewById(R.id.animTV)
        screenServiceIntent = Intent(this, ProjectionService::class.java)
        init()
        startAnim()
    }

    private fun stopProjection() {
        stopService(screenServiceIntent)
    }

    private fun cancelAnim() {
        isStart = false
        valueAnimator?.cancel()
    }

    private var valueAnimator: ValueAnimator? = null

    private fun startAnim() {
        valueAnimator?.cancel()
        valueAnimator = ValueAnimator.ofFloat(1f, 0f)
        // 无限循环
        valueAnimator?.apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                Log.e("TAG", "值${value}")
                animTV?.text = "$value"
            }

            addListener(
                onEnd = {
                    if (isStart) {
                        it.start()
                    }
                }
            )

            repeatMode = ValueAnimator.RESTART
            duration = 10000
            start()
        }
    }

    private fun init() {
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private var isStart = false

    // 请求开始录屏
    private fun startProjection() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        mActivityBLauncher.launch(intent)
        isStart = true
    }

    override fun onResume() {
        super.onResume()
        val wm1 = this.windowManager
        SCREEN_WIDTH = wm1.defaultDisplay.width
        SCREEN_HEIGHT = wm1.defaultDisplay.height
        SCREEN_DPI = resources.displayMetrics.densityDpi
    }

    override fun onDestroy() {
        cancelAnim()
        super.onDestroy()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    fun onClick(view: View) {
        if (view.id == R.id.start) {
            if (ProjectionService.isStarted) {
                Toast.makeText(this, "投屏已开始", Toast.LENGTH_SHORT).show()
            } else {
                startProjectionEvent()
            }
        } else {
            stopProjection()
        }
    }

    private fun startProjectionEvent() {
        XXPermissions.with(this)
            // 申请单个权限
            .permission(Permission.RECORD_AUDIO)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    startProjection()
                }

                override fun onDenied(
                    permissions: MutableList<String>,
                    doNotAskAgain: Boolean
                ) {
                    if (doNotAskAgain) {
                        Toast.makeText(
                            this@MainActivity,
                            "被永久拒绝授权，请手动授予录音权限",
                            Toast.LENGTH_SHORT
                        ).show()
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "获取录音权限失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

}