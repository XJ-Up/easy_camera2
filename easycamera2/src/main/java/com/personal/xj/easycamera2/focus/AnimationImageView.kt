package com.personal.xj.easycamera2.focus

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import com.personal.xj.easycamera2.R
import com.personal.xj.easycamera2.utils.SleepThread

/**
 *
 * @ClassName:      AnimationImageView
 * @Description:     聚焦时显示的动画图像
 * @Author:         xj
 */
class AnimationImageView : androidx.appcompat.widget.AppCompatImageView {
    private var mMainHandler: Handler? = null
    private var mAnimation: Animation? = null
    private var mContext: Context


    var mTimes = 0

    constructor(context: Context) : super(context) {
        mContext = context
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        mContext = context
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        mContext = context
    }


    fun setmMainHandler(mMainHandler: Handler?) {
        this.mMainHandler = mMainHandler
    }

    fun setmAnimation(mAnimation: Animation?) {
        this.mAnimation = mAnimation
    }

    fun initFocus() {
        this.visibility = View.VISIBLE
        mMainHandler?.let {
            Thread(SleepThread(it, 100, 1000, null))
                .start()
        }

    }

    fun startFocusing() {
        mTimes++
        this.visibility = View.VISIBLE
        startAnimation(mAnimation)
        this.setBackgroundResource(R.mipmap.focus)
        mMainHandler?.let {
            Thread(
                SleepThread(
                    it,
                    100,
                    1000,
                    Integer.valueOf(mTimes)
                )
            ).start()
        }

    }

    fun focusFailed() {
        mTimes++
        this.setBackgroundResource(R.mipmap.focus_failed)
        mMainHandler?.let {
            Thread(
                SleepThread(
                    it,
                    100,
                    800,
                    Integer.valueOf(mTimes)
                )
            ).start()
        }

    }

    fun focusSuccess() {
        mTimes++
        this.visibility = View.VISIBLE
        this.setBackgroundResource(R.mipmap.focus_succeed)
        mMainHandler?.let {
            Thread(
                SleepThread(
                    it,
                    100,
                    800,
                    Integer.valueOf(mTimes)
                )
            ).start()
        }
    }

    fun stopFocus() {
        this.visibility = View.INVISIBLE
    }
}