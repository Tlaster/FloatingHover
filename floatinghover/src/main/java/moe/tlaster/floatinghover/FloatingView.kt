package moe.tlaster.floatinghover

import android.animation.ArgbEvaluator
import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.RelativeLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.marginTop


class FloatingView : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private val homeFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)

    private val homeListenerReceiver = object : BroadcastReceiver() {
        internal val SYSTEM_DIALOG_REASON_KEY = "reason"
        internal val SYSTEM_DIALOG_REASON_HOME_KEY = "homekey"

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY)

            if (action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS
                && reason != null && reason == SYSTEM_DIALOG_REASON_HOME_KEY
            ) {
                requireClose()
            }
        }
    }

    private var isHomeReceiverRegistered = false
    private var isShowing = false
    private var isInit = false
    private lateinit var iconView: View
    private var iconXOffset = 0
    private lateinit var contentView: View
    private var contentViewCenterX = 0F
    private var contentViewCenterY = 0F
    private var contentViewPositionX: Float = 0F
    private var contentViewPositionY: Float = 0F
    private var isClosing = false
    private var iconViewInitY: Float = 0F

    internal var statusBarOffset: Int = 0

    override fun onFinishInflate() {
        super.onFinishInflate()
        iconView = getChildAt(0)
        contentView = getChildAt(1)
        this.post {
            if (!isInit) {
                isInit = true
                contentViewPositionX = contentView.x + contentView.width / 2
                contentViewPositionY = contentView.y + contentView.height / 2
                contentViewCenterY = contentView.pivotY
                contentViewCenterX = contentView.pivotX
                iconXOffset = iconView.width / 4
                iconView.x = -iconXOffset.toFloat()
                iconViewInitY = iconView.y
                contentView.visibility = View.GONE
                val position = intArrayOf(0, 0)
                iconView.getLocationOnScreen(position)
                statusBarOffset = position[1] - iconView.marginTop
                showIconViewStartAnimation()
            }
        }
    }

    fun getDragger(): View {
        return iconView
    }

    fun moveDragger(x: Float, y: Float) {
        iconView.x = x
        iconView.y = y
    }

    private fun showIconViewStartAnimation() {
        isShowing = true
        iconView.scaleX = 0F
        iconView.scaleY = 0F
        iconView.animate()
            .scaleY(1F)
            .scaleX(1F)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                startIdleAnimation()
            }
            .start()
    }

    private var animationFocalBreathing: ValueAnimator? = null

    private fun startIdleAnimation() {
        animationFocalBreathing = ValueAnimator.ofFloat(1F, 1.1F, 1F)
        animationFocalBreathing?.interpolator = AccelerateDecelerateInterpolator()
        animationFocalBreathing?.duration = 1000
        animationFocalBreathing?.repeatCount = ValueAnimator.INFINITE
        animationFocalBreathing?.addUpdateListener { animation ->
            val newFocalFraction = animation.animatedValue as Float
            iconView.scaleX = newFocalFraction
            iconView.scaleY = newFocalFraction
        }
        animationFocalBreathing?.start()
    }

    internal fun closeIconView() {
        if (isClosing) {
            return
        }
        isClosing = true
        iconView.animate()
            .x(-iconView.width.toFloat())
            .alpha(0F)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                closeCleanup()
            }
            .start()
    }

    internal fun onDraggerClick() {
        showContent()
    }

    private fun showContent() {
        context.registerReceiver(homeListenerReceiver, homeFilter)
        isHomeReceiverRegistered = true
        iconView.animate()
            .x(contentViewPositionX - iconView.width / 2)
            .y(contentViewPositionY - iconView.height / 2)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                ValueAnimator.ofObject(ArgbEvaluator(), Color.TRANSPARENT, Color.argb(125, 0, 0, 0))
                    .apply {
                        addUpdateListener {
                            this@FloatingView.setBackgroundColor(it.animatedValue as Int)
                        }
                    }.start()

                iconView.animate()
                    .scaleX(0F)
                    .scaleY(0F)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        iconView.visibility = View.GONE
                    }
                    .start()
                contentView.visibility = View.VISIBLE
                ViewAnimationUtils.createCircularReveal(
                    contentView,
                    contentViewCenterX.toInt(),
                    contentViewCenterY.toInt(),
                    0F,
                    Math.hypot(contentViewCenterX.toDouble(), contentViewCenterY.toDouble()).toFloat()
                ).apply {
                    requestFocusable?.run()
                    start()
                }
            }.start()
    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && isShowing) {
            val positionX = ev.x
            val positionY = ev.y
            if (positionX < contentViewPositionX + contentView.width / 2 && positionX > contentViewPositionX - contentView.width / 2
                && positionY > contentViewPositionY - contentView.height / 2 && positionY < contentViewPositionY + contentView.height / 2) {
                return super.dispatchTouchEvent(ev)
            }
            requireClose()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInit) {
            showIconViewStartAnimation()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (!::iconView.isInitialized || !::contentView.isInitialized) {
            return super.dispatchKeyEvent(event)
        }
        if (event != null && isShowing) {
            requireClose()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    var closeCallback: Runnable? = null
    var requestFocusable: Runnable? = null

    private fun requireClose() {
        if (isClosing) {
            return
        }
        isClosing = true
        ValueAnimator.ofObject(FloatEvaluator(), 1, 0)
            .apply {
                addUpdateListener {
                    this@FloatingView.alpha = (it.animatedValue as Float)
                }
                doOnEnd {
                    closeCleanup()
                }
            }.start()
    }

    private fun closeCleanup() {
        isShowing = false
        closeCallback?.run()
        setBackgroundColor(Color.TRANSPARENT)
        animationFocalBreathing?.cancel()
        iconView.x = -iconXOffset.toFloat()
        iconView.y = iconViewInitY
        iconView.alpha = 1F
        iconView.scaleX = 1F
        iconView.scaleY = 1F
        contentView.visibility = View.GONE
        iconView.visibility = View.VISIBLE
        this@FloatingView.alpha = 1F
        if (isHomeReceiverRegistered) {
            context.unregisterReceiver(homeListenerReceiver)
        }
        isHomeReceiverRegistered = false
        isClosing = false
    }
}
