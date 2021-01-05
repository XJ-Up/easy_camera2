package com.personal.xj.easycamera2

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.personal.xj.easycamera2.view.Camera2Activity

/**
 *
 * @ClassName:      EasyCamera2
 * @Description:     创建EasyCamera2
 * @Author:         xj
 */
private val PERMISSIONS_REQUIRED = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

object EasyCamera2 {
    private const val CAMERA_ACTIVITY = 3

    /** 用于检查是否已授予此应用所需的所有权限的便捷方法 */
    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** image地址获取*/
    private var imageBlock: ((String) -> Unit)? = null

    /** video地址获取*/
    private var videoBlock: ((String) -> Unit)? = null

    /**
     * 设置图片拍摄完成后path
     */
    fun setImageOutMonitor(block: (String) -> Unit) {
        imageBlock = block
    }

    /**
     * 设置视频拍摄完成后path
     */
    fun setVideoOutMonitor(block: (String) -> Unit) {
        videoBlock = block
    }

    fun getImageOutMonitor(): ((String) -> Unit)? {
        return imageBlock
    }

    fun getVideoOutMonitor(): ((String) -> Unit)? {
        return videoBlock
    }

    fun jumpActivity(activity: Activity) {
        if (hasPermissions(activity)) {
            val intent = Intent(activity, Camera2Activity::class.java)
            activity.startActivityForResult(intent, CAMERA_ACTIVITY)
        } else {
            Toast.makeText(activity, "权限未给予", Toast.LENGTH_SHORT).show()
        }
    }

}