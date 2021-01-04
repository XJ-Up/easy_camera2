package com.personal.xj.example


import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.personal.xj.easycamera2.EasyCamera2
import com.personal.xj.easycamera2.monitor.IEasyCamera2OutImagePath
import com.personal.xj.easycamera2.monitor.IEasyCamera2OutVideoPath
import kotlinx.android.synthetic.main.activity_main.*

/** 初始化配置 */
private val test by lazy {
    EasyCamera2()
}

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test.setImageOutMonitor(object :IEasyCamera2OutImagePath{
            override fun outPath(path: String) {
                info.text = path
            }

        })
        test.setVideoOutMonitor(object :IEasyCamera2OutVideoPath{
            override fun outPath(path: String) {
                info1.text = path
            }
        })
        button.setOnClickListener {
            test.jumpActivity(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        test.setData(requestCode,resultCode,data)
    }

}