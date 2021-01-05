package com.personal.xj.easycamera2.monitor

import android.view.MotionEvent

/**
 *
 * @ClassName:      MySurfaceTouchEvent
 * @Description:     监听 Surface触摸事件
 * @Author:         xj
 */
interface MySurfaceTouchEvent {
    fun onAreaTouchEvent(event: MotionEvent?): Boolean
}