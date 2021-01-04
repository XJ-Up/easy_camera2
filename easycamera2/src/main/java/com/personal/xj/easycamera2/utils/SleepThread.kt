package com.personal.xj.easycamera2.utils

import android.os.Handler

/**
 *
 * @ClassName:      SleepThread
 * @Description:     休眠线程工具类
 * @Author:         xj
 */
class SleepThread(
    mainHandler: Handler,
    what: Int,
    mTime: Long,
    mObject: Any?
) :
    Runnable {
    private val mMainHandler: Handler = mainHandler
    private val what: Int = what
    private val mTime: Long = mTime
    private val mObject: Any? = mObject
    override fun run() {
        try {
            Thread.sleep(mTime)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val message = mMainHandler.obtainMessage()
        message.what = what
        message.obj = mObject
        mMainHandler.sendMessage(message)
    }

}