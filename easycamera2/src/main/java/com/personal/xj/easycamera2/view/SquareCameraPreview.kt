package com.personal.xj.easycamera2.view

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import com.personal.xj.easycamera2.monitor.ISeekBarUpData
import com.personal.xj.easycamera2.utils.Zoom
import kotlin.math.roundToInt
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SquareCameraPreview : SurfaceView {
    private var iUpdate: ISeekBarUpData? = null
    private var mScaleFactor = 1
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mContext: Context? = null
    private  var mCameraBuilder: CaptureRequest.Builder?=null
    private  var  mCharacteristics: CameraCharacteristics?=null
    private  var  mZoom: Zoom?=null
    private  var  mSession:CameraCaptureSession?=null
    private  var  mHandler:Handler?=null
    private var mFocusPositionTouchEvent: FocusPositionTouchEvent? = null
    private  var mMySurfaceTouchEvent:MySurfaceTouchEvent?=null


    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        init(context)
    }

    @SuppressLint("NewApi")
    private fun init(context: Context) {
        mContext = context
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
    }
    private var aspectRatio = 0f
    /**
     * Measure the view and its content to determine the measured width and the
     * measured height
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //获得宽高
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        //根据宽高比 计算宽高并设置
        if (aspectRatio == 0f) {
            //如果为0直接使用宽高
            setMeasuredDimension(width, height)
        } else {
            //如果不为0通过计算获得宽高
            /*
           对相机镜框进行中央裁剪
             */
            val newWidth: Int
            val newHeight: Int
            //计算比例
            val actualRatio = if (width > height) aspectRatio else 1f / aspectRatio
            //计算新的宽高
            if (width < height * actualRatio) {
                newHeight = height
                newWidth = (height * actualRatio).roundToInt()
            } else {
                newWidth = width
                newHeight = (width / actualRatio).roundToInt()
            }
            setMeasuredDimension(newWidth, newHeight)
        }
    }
    fun setAspectRatio(width: Int, height: Int) {
        //打印
        require(width > 0 && height > 0) {
            "大小不能为负数"
        }
        //计算出宽高比
        aspectRatio = width.toFloat() / height.toFloat()
        //给 SurfaceView 中 holder设置宽高
        holder.setFixedSize(width, height)
        //刷新页面
        requestLayout()
    }

    /**
     * 设置Camera的信息，后续变焦使用
     */
    fun  setCamera(session: CameraCaptureSession,characteristics: CameraCharacteristics?,cameraBuilder: CaptureRequest.Builder?,handler: Handler){
        mSession=session
        mCameraBuilder=cameraBuilder
        mCharacteristics=characteristics
        mHandler=handler
        mZoom = Zoom(mCharacteristics)
    }

    /**
     * 对点击事件进行管理
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //二指缩小放大
        mScaleDetector!!.onTouchEvent(event)
        //单指点击位置获取，手动对焦实现
        mFocusPositionTouchEvent?.getPosition(event)
        //手动对焦触发
        return mMySurfaceTouchEvent?.onAreaTouchEvent(event)!!
    }

    fun setSeekBarUpData(iUpdate: ISeekBarUpData?) {
        this.iUpdate = iUpdate
    }

    private fun handleZoom() {

        mCameraBuilder?.let {builder->
            var zoom = mZoom?.getZoom(builder)
            zoom?.let {
                if (mScaleFactor == ZOOM_IN) {
                    zoom += ZOOM_DELTA.toFloat()
                } else if (mScaleFactor == ZOOM_OUT) {
                    zoom -= ZOOM_DELTA.toFloat()
                }
                iUpdate?.progressUpdate(zoom)
                mZoom?.setZoom(builder,zoom)
                mSession?.setRepeatingRequest(builder.build(), null, mHandler)
            }
        }
    }

    /**
     * 通过 SimpleOnScaleGestureListener 监听手势指令，当 [ScaleFactor]为0进行缩小，为1进行放大
     */
    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor = detector.scaleFactor.toInt()
            mCameraBuilder?.let {
                /** 开始设置*/
                    handleZoom()
            }
            return true
        }
    }

    companion object {
        val TAG = SquareCameraPreview::class.java.simpleName
        private const val ZOOM_OUT = 0
        private const val ZOOM_IN = 1
        private const val ZOOM_DELTA = 0.1

    }
    interface FocusPositionTouchEvent {
        fun getPosition(event: MotionEvent?)
    }

    fun setmFocusPositionTouchEvent(focusPositionTouchEvent: FocusPositionTouchEvent) {
            mFocusPositionTouchEvent = focusPositionTouchEvent
    }

    interface MySurfaceTouchEvent {
        fun onAreaTouchEvent(event: MotionEvent?): Boolean
    }
    fun setMySurfaceTouchEvent(mySurfaceTouchEvent: MySurfaceTouchEvent){
        mMySurfaceTouchEvent=mySurfaceTouchEvent
    }
}
