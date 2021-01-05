package com.personal.xj.easycamera2.monitor

import android.view.MotionEvent

/**
 *
 * @ClassName:      FocusPositionTouchEvent
 * @Description:     焦点位置触摸事件
 * @Author:         xj
 */
interface FocusPositionTouchEvent {
    fun getPosition(event: MotionEvent?)
}