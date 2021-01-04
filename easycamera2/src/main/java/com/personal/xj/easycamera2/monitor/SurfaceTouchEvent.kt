package com.personal.xj.easycamera2.monitor

import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import com.personal.xj.easycamera2.view.SquareCameraPreview

/**
 *
 * @ClassName:      SurfaceTouchEvent
 * @Description:     Surface触摸事件监听，完成具体点击聚焦任务
 * @Author:         xj
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SurfaceTouchEvent(
    private val mCameraCharacteristics: CameraCharacteristics,
    private val mSquareView: SquareCameraPreview,
    private val mPreviewBuilder: CaptureRequest.Builder,
    private val mCameraCaptureSession: CameraCaptureSession,
    private val mHandler: Handler,
    private val mPreviewSessionCallback: PreviewSessionCallback
) :
    SquareCameraPreview.MySurfaceTouchEvent {
    override fun onAreaTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val rect =
                        mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                    Log.i(
                        "onAreaTouchEvent",
                        "SENSOR_INFO_ACTIVE_ARRAY_SIZE,,,,,,,,rect.left--->" + rect!!.left + ",,,rect.top--->" + rect.top + ",,,,rect.right--->" + rect.right + ",,,,rect.bottom---->" + rect.bottom
                    )
                    val size =
                        mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                    Log.i(
                        "onAreaTouchEvent",
                        "mCameraCharacteristics,,,,size.getWidth()--->" + size!!.width + ",,,size.getHeight()--->" + size.height
                    )
                    val areaSize = 200
                    val right = rect.right
                    val bottom = rect.bottom
                    val viewWidth = mSquareView.width
                    val viewHeight = mSquareView.height
                    val ll: Int
                    val rr: Int
                    val newRect: Rect
                    val centerX = event.x.toInt()
                    val centerY = event.y.toInt()
                    ll = (centerX * right - areaSize) / viewWidth
                    rr = (centerY * bottom - areaSize) / viewHeight
                    val focusLeft = clamp(ll, 0, right)
                    val focusBottom = clamp(rr, 0, bottom)
                    Log.i(
                        "focus_position",
                        "focusLeft--->" + focusLeft + ",,,focusTop--->" + focusBottom + ",,,focusRight--->" + (focusLeft + areaSize) + ",,,focusBottom--->" + (focusBottom + areaSize)
                    )
                    newRect = Rect(
                        focusLeft,
                        focusBottom,
                        focusLeft + areaSize,
                        focusBottom + areaSize
                    )
                    val meteringRectangle = MeteringRectangle(newRect, 500)
                    val meteringRectangleArr =
                        arrayOf(meteringRectangle)
                    /**通过关闭自动对焦 并再次开启自动对焦完成点击对焦的操作*/
                    mPreviewBuilder.set(

                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_OFF
                    )
                    mPreviewBuilder.set(
                        CaptureRequest.CONTROL_AF_REGIONS,
                        meteringRectangleArr
                    )
                    mPreviewBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                    )
                    //发送请求
                    updatePreview()
                }
            }
        }

        return true
    }

    /**
     * 触摸对焦的计算
     *
     * @param x
     * @param min
     * @param max
     * @return
     */
    private fun clamp(x: Int, min: Int, max: Int): Int {
        return when {
            x < min -> {
                min
            }
            x > max -> {
                max
            }
            else -> {
                x
            }
        }
    }

    /**
     * 更新预览
     */
    private fun updatePreview() {

        try {
            mCameraCaptureSession.setRepeatingRequest(
                mPreviewBuilder.build(),
                mPreviewSessionCallback,
                mHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("updatePreview", "ExceptionExceptionException")
        }
    }


}