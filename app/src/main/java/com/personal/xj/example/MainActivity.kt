package com.personal.xj.example


import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.personal.xj.easycamera2.EasyCamera2
import kotlinx.android.synthetic.main.activity_main.*

/** 初始化配置 */
private val test by lazy {
    EasyCamera2
}

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        test.setImageOutMonitor {
            info.text = it
        }
        test.setVideoOutMonitor {
            info1.text = it
        }

        button.setOnClickListener {
            test.jumpActivity(this)
        }
    }


}