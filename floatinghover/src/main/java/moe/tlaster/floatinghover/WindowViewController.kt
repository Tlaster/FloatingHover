package moe.tlaster.floatinghover

import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class WindowViewController(private val mWindowManager: WindowManager) {

    fun addView(width: Int, height: Int, isTouchable: Boolean, view: View) {
        // If this view is untouchable then add the corresponding flag, otherwise set to zero which
        // won't have any effect on the OR'ing of flags.
        val touchableFlag = if (isTouchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        val params = WindowManager.LayoutParams(
            width,
            height,
            windowType,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or touchableFlag,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        mWindowManager.addView(view, params)
    }

    fun removeView(view: View) {
        if (null != view.parent) {
            mWindowManager.removeView(view)
        }
    }

    fun getViewPosition(view: View): Point {
        val params = view.layoutParams as WindowManager.LayoutParams
        return Point(params.x, params.y)
    }

    fun moveViewTo(view: View, x: Int, y: Int) {
        val params = view.layoutParams as WindowManager.LayoutParams
        params.x = x
        params.y = y
        mWindowManager.updateViewLayout(view, params)
    }

    fun showView(view: View) {
        try {
            val params = view.layoutParams as WindowManager.LayoutParams
            mWindowManager.addView(view, params)
        } catch (e: IllegalStateException) {
            // The view is already visible.
        }

    }

    fun hideView(view: View) {
        try {
            mWindowManager.removeView(view)
        } catch (e: IllegalArgumentException) {
            // The View wasn't visible to begin with.
        }

    }

    fun makeTouchable(view: View) {
        val params = view.layoutParams as WindowManager.LayoutParams
        params.flags =
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv() and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        mWindowManager.updateViewLayout(view, params)
    }

    fun makeUntouchable(view: View) {
        val params = view.layoutParams as WindowManager.LayoutParams
        params.flags =
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mWindowManager.updateViewLayout(view, params)
    }

}