package com.personal.xj.easycamera2

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.personal.xj.easycamera2.monitor.IEasyCamera2OutImagePath
import com.personal.xj.easycamera2.monitor.IEasyCamera2OutVideoPath
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

class EasyCamera2 {

         /** image监听接口*/
     private  var  iEasyCamera2OutImagePath:IEasyCamera2OutImagePath?=null
     /** video监听接口*/
     private  var  iEasyCamera2OutVideoPath:IEasyCamera2OutVideoPath?=null
     /**
      * 设置图片拍摄完成后path监听回调
      */
     fun  setImageOutMonitor(iEasyCamera2OutImagePath: IEasyCamera2OutImagePath){
            this.iEasyCamera2OutImagePath=iEasyCamera2OutImagePath
     }
     /**
      * 设置视频拍摄完成后path监听回调
      */
     fun  setVideoOutMonitor(iEasyCamera2OutVideoPath: IEasyCamera2OutVideoPath){
         this.iEasyCamera2OutVideoPath=iEasyCamera2OutVideoPath
     }
    fun setData(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode== CAMERA_ACTIVITY&&resultCode==Activity.RESULT_OK){
            data?.let {
                val videoPath = data.getStringExtra("videoPath")
                if (!videoPath.isNullOrEmpty()){
                    iEasyCamera2OutVideoPath?.outPath(videoPath)
                }
                val imagePath=data.getStringExtra("imagePath")
                if (!imagePath.isNullOrEmpty()){
                    iEasyCamera2OutImagePath?.outPath(imagePath)
                }
            }
        }
    }

    fun jumpActivity(activity: Activity) {
        if (hasPermissions(activity)) {
            val intent = Intent(activity, Camera2Activity::class.java)
            activity.startActivityForResult(intent, CAMERA_ACTIVITY)
        } else {
            Toast.makeText(activity, "权限未给予", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val CAMERA_ACTIVITY = 3

        /** 用于检查是否已授予此应用所需的所有权限的便捷方法 */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

}