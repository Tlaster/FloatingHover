package moe.tlaster.floatinghover

import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.LayoutRes

class FloatingController(private val context: Context, val floatingView: FloatingView) {
    constructor(context: Context, @LayoutRes layout: Int):
            this(context, LayoutInflater.from(context).inflate(layout, null, false) as FloatingView)

    private var isInit = false
    private val dragger by lazy {
        View(context).apply {
        }
    }
    private val windowViewController by lazy {
        WindowViewController(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
    }
    private var draggerWidth: Int = 0
    private var draggerHeight: Int = 0
    private var draggerPositionX: Float = 0F
    private var draggerPositionY: Float = 0F
    private var lastY: Float = 0F
    private var viewStartY: Float = 0F
    private var isMoving = false
        set(value) {
            field = value
            if (value) {
                stopTimer()
            }
        }

    init {
        floatingView.post {
            if (!isInit) {
                isInit = true
                val draggerView = floatingView.getDragger()
                draggerWidth = draggerView.width
                draggerHeight = draggerView.height
                draggerPositionX = draggerView.x
                draggerPositionY = draggerView.y
//                showDragger()
            }
        }
        floatingView.requestFocusable = Runnable {
            windowViewController.makeTouchable(floatingView)
        }
        floatingView.closeCallback = Runnable {
            windowViewController.removeView(floatingView)
        }
        dragger.setOnClickListener {
            stopTimer()
            windowViewController.removeView(dragger)
            windowViewController.makeTouchable(floatingView)
            floatingView.onDraggerClick()
        }
        dragger.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastY = event.rawY
                    viewStartY = event.y
                    isMoving = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val y = event.rawY
                    if (Math.abs(y - lastY) > 2) {
                        isMoving = true
                        val targetX = -draggerWidth / 4
                        val targetY = (y - viewStartY - floatingView.statusBarOffset).toInt()
                        windowViewController.moveViewTo(dragger, targetX, targetY)
                        floatingView.moveDragger(targetX.toFloat(), targetY.toFloat())
                    }
                    lastY = y
                    true
                }
                else -> {
                    if (event.action == MotionEvent.ACTION_UP) {
                        setupTimer()
                    }
                    isMoving
                }
            }
        }
    }

    private val iconViewClosingHandler = Handler()
    private val closeIconViewRunnable = Runnable {
        floatingView.closeIconView()
        windowViewController.removeView(dragger)
        windowViewController.removeView(floatingView)
    }

    private fun stopTimer() {
        iconViewClosingHandler.removeCallbacks(closeIconViewRunnable)
    }

    private fun setupTimer() {
        iconViewClosingHandler.postDelayed(closeIconViewRunnable, 5000)
    }

    fun show() {
        setupTimer()
        windowViewController.addView(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            false,
            floatingView
        )
        if (!isInit) {
            return
        }
        showDragger()
    }

    fun showContentView() {
        windowViewController.addView(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            true,
            floatingView
        )
        floatingView.post {
            floatingView.showContentView()
        }
    }

    fun hide() {
        floatingView.requireClose()
    }

    private fun showDragger() {
        windowViewController.addView(
            draggerWidth,
            draggerHeight,
            true,
            dragger
        )
        windowViewController.moveViewTo(dragger, draggerPositionX.toInt(), draggerPositionY.toInt())
    }
}