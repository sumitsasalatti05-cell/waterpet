package com.waterpet.app

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Draws the pet over other apps: it peeks in from the right edge, wiggles,
 * then slides out fully and shows a speech bubble with "Drank!" / "Later".
 */
object PetOverlay {
    private val main = Handler(Looper.getMainLooper())
    private var view: View? = null
    private var wm: WindowManager? = null
    private var autoHide: Runnable? = null

    private const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    private val messages = listOf(
        "Psst! %s here 🐾 Time for a glass of water!",
        "Hydration check! 💧 %s says drink up!",
        "%s is thirsty... and so are you! 🥤",
        "Sip sip hooray! %s believes in you 💙",
        "Your body called. It wants water! — %s"
    )

    fun show(context: Context, onLater: () -> Unit = {}) {
        val app = context.applicationContext
        if (view != null || !Settings.canDrawOverlays(app)) return

        val prefs = Prefs(app)
        val d = app.resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()
        val windowManager = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = windowManager

        val message = TextView(app).apply {
            text = messages.random().format(prefs.petName)
            textSize = 15f
            setTextColor(Color.parseColor("#0D3B66"))
            maxWidth = px(200)
        }
        val drank = pill(app, "Drank! 💧", "#2196F3", Color.WHITE, d)
        val later = pill(app, "Later", "#E3F2FD", Color.parseColor("#0D3B66"), d)
        val buttons = LinearLayout(app).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, px(10), 0, 0)
            addView(drank)
            addView(later, LinearLayout.LayoutParams(WRAP, WRAP).apply { leftMargin = px(8) })
        }
        val bubble = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(14), px(12), px(14), px(12))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = px(18).toFloat()
                setStroke(px(2), Color.parseColor("#90CAF9"))
            }
            addView(message)
            addView(buttons)
        }
        val pet = TextView(app).apply {
            text = prefs.petEmoji
            textSize = 64f
        }
        val root = LinearLayout(app).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            visibility = View.INVISIBLE
            addView(bubble, LinearLayout.LayoutParams(WRAP, WRAP).apply {
                rightMargin = px(4)
                bottomMargin = px(12)
            })
            addView(pet)
        }

        later.setOnClickListener {
            hide()
            onLater()
        }
        drank.setOnClickListener {
            val count = prefs.addGlass()
            val goal = prefs.goal
            message.text = if (count >= goal) {
                "🎉 Goal reached! $count/$goal glasses. ${prefs.petName} is so proud!"
            } else {
                "Yay! $count/$goal glasses 💧 Keep it up!"
            }
            buttons.visibility = View.GONE
            ObjectAnimator.ofFloat(pet, "translationY", 0f, -px(30).toFloat(), 0f).apply {
                duration = 500
                repeatCount = 2
            }.start()
            main.postDelayed({ if (view === root) hide() }, 2200)
        }

        val lp = WindowManager.LayoutParams(
            WRAP, WRAP,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Not focusable: the app underneath keeps working normally.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            y = px(140)
        }

        view = root
        windowManager.addView(root, lp)
        root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (root.width == 0) return
                root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                animateIn(root, bubble, pet)
            }
        })

        // If the user ignores it, tuck the pet away after 30 seconds.
        autoHide = Runnable { hide() }
        main.postDelayed(autoHide!!, 30_000)
    }

    private fun animateIn(root: View, bubble: View, pet: View) {
        val w = root.width.toFloat()
        val peek = pet.width * 0.55f // how far the pet is pushed off-screen while peeking
        bubble.alpha = 0f
        root.translationX = w
        root.visibility = View.VISIBLE
        // Stage 1: pet peeks out from the screen edge
        root.animate().translationX(peek).setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                if (view !== root) return@withEndAction
                ObjectAnimator.ofFloat(pet, "rotation", 0f, -12f, 12f, -8f, 8f, 0f)
                    .setDuration(700).start()
                // Stage 2: slide fully in and show the speech bubble
                main.postDelayed({
                    if (view !== root) return@postDelayed
                    bubble.animate().alpha(1f).setDuration(300).start()
                    root.animate().translationX(0f).setDuration(450)
                        .setInterpolator(DecelerateInterpolator()).start()
                }, 900)
            }.start()
    }

    fun hide(animated: Boolean = true) {
        val v = view ?: return
        view = null
        autoHide?.let { main.removeCallbacks(it) }
        autoHide = null
        val manager = wm
        if (animated && v.isAttachedToWindow) {
            v.animate().translationX(v.width.toFloat()).setDuration(350)
                .withEndAction { removeSafely(manager, v) }.start()
        } else {
            removeSafely(manager, v)
        }
    }

    private fun removeSafely(manager: WindowManager?, v: View) {
        try {
            manager?.removeView(v)
        } catch (_: Exception) {
        }
    }

    private fun pill(ctx: Context, label: String, bg: String, fg: Int, d: Float): TextView =
        TextView(ctx).apply {
            text = label
            textSize = 14f
            setTextColor(fg)
            gravity = Gravity.CENTER
            val h = (14 * d).toInt()
            val v = (8 * d).toInt()
            setPadding(h, v, h, v)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bg))
                cornerRadius = 100f * d
            }
            isClickable = true
        }
}
