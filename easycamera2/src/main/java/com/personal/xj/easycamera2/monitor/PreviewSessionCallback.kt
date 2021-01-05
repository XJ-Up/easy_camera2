package com.personal.xj.easycamera2.monitor

import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.personal.xj.easycamera2.focus.AnimationImageView
import com.personal.xj.easycamera2.view.SquareCameraPreview

/**
 *
 * @ClassName:      PreviewSessionCallback
 * @Description:     预览回调监听，用来完成点击聚焦
 * @Author:         xj
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class PreviewSessionCallback(
    private val mFocusImage: AnimationImageView,
    private val mMainHandler: Handler,
    squareCameraPreview: SquareCameraPreview
) :
    CaptureCallback(), FocusPositionTouchEvent {
    private var mAfState = CameraMetadata.CONTROL_AF_STATE_INACTIVE
    private var mRawX = 0
    private var mRawY = 0
    private var mFlagShowFocusImage = false
    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        super.onCaptureCompleted(session, request, result)
        Log.i(
            "Thread",
            "onCaptureCompleted---->" + Thread.currentThread().name
        )
        Log.i("PreviewSessionCallback", "onCaptureCompleted")

        val nowAfState = result.get(CaptureResult.CONTROL_AF_STATE) ?: return
        //获取失败
        //这次的值与之前的一样，忽略掉
        if (nowAfState == mAfState) {
            return
        }
        mAfState = nowAfState
        mMainHandler.post {
            judgeFocus()
        }
    }

    private fun judgeFocus() {

        when (mAfState) {
            CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN, CameraMetadata.CONTROL_AF_STATE_PASSIVE_SCAN -> focusFocusing()
            CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED, CameraMetadata.CONTROL_AF_STATE_PASSIVE_FOCUSED -> focusSucceed()
            CameraMetadata.CONTROL_AF_STATE_INACTIVE -> focusInactive()
            CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED, CameraMetadata.CONTROL_AF_STATE_PASSIVE_UNFOCUSED -> focusFailed()
        }
    }

    private fun focusFocusing() {
        //得到宽高
        val width: Int = mFocusImage.width
        val height: Int = mFocusImage.height
        //居中
        val margin = MarginLayoutParams(mFocusImage.layoutParams)
        margin.setMargins(
            mRawX - width / 2,
            mRawY - height * 2,
            margin.rightMargin,
            margin.bottomMargin
        )
        val layoutParams = FrameLayout.LayoutParams(margin)
        mFocusImage.layoutParams = layoutParams
        //显示
        if (!mFlagShowFocusImage) {
            mFocusImage.startFocusing()
            mFlagShowFocusImage = true
        }
    }

    private fun focusSucceed() {
        if (mFlagShowFocusImage) {
            mFocusImage.focusSuccess()
            mFlagShowFocusImage = false
        }
    }

    private fun focusInactive() {
        mFocusImage.stopFocus()
        mFlagShowFocusImage = false
    }

    private fun focusFailed() {
        if (mFlagShowFocusImage) {
            mFocusImage.focusFailed()
            mFlagShowFocusImage = false
        }
    }


    init {
        squareCameraPreview.setMyFocusPositionTouchEvent(this)
    }

    override fun getPosition(event: MotionEvent?) {
        mRawX = event?.rawX?.toInt()!!
        mRawY = event.rawY.toInt()
    }
}
