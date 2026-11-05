package com.waterpet.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var permStatus: TextView
    private lateinit var permBtn: Button
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var toggleBtn: Button
    private val petViews = mutableListOf<TextView>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        buildUi()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, size: Float = 16f, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(Color.parseColor("#0D3B66"))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(2))
        }

    private fun buildUi() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(32), dp(20), dp(32))
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F4FAFF"))
            addView(col)
        })

        col.addView(text("💧 WaterPet", 28f, true))
        col.addView(text("Your favourite pet reminds you to drink water.", 15f))

        // ---- Step 1: overlay permission ----
        col.addView(text("Permission", 18f, true).apply { setPadding(0, dp(20), 0, dp(2)) })
        permStatus = text("", 15f)
        col.addView(permStatus)
        permBtn = Button(this).apply {
            text = "Grant overlay permission"
            setOnClickListener { requestOverlayPermission() }
        }
        col.addView(permBtn)

        // ---- Progress ----
        col.addView(text("Today", 18f, true).apply { setPadding(0, dp(20), 0, dp(2)) })
        progressText = text("", 16f)
        col.addView(progressText)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        col.addView(progressBar)
        col.addView(Button(this).apply {
            text = "+1 glass (log manually)"
            setOnClickListener { prefs.addGlass(); refresh() }
        })

        // ---- Step 5: pet selection ----
        col.addView(text("Choose your pet", 18f, true).apply { setPadding(0, dp(20), 0, dp(2)) })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Pets.list.forEachIndexed { i, (emoji, _) ->
            val tv = TextView(this).apply {
                text = emoji
                textSize = 32f
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener { prefs.petIndex = i; refreshPets() }
            }
            petViews.add(tv)
            row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        col.addView(row)
        col.addView(text("Pet name", 15f))
        col.addView(EditText(this).apply {
            setText(prefs.petName)
            setSingleLine()
            doAfterTextChanged { prefs.petName = it?.toString().orEmpty() }
        })

        // ---- Step 5: goal, interval, active hours ----
        col.addView(text("Settings", 18f, true).apply { setPadding(0, dp(20), 0, dp(2)) })
        addSlider(col, 4, 16, prefs.goal, { "Daily goal: $it glasses" }) {
            prefs.goal = it; refresh()
        }
        addSlider(col, 15, 180, prefs.intervalMin, { "Remind me every $it min" }) {
            prefs.intervalMin = it
        }
        addSlider(col, 0, 23, prefs.startHour, { "Active from %02d:00".format(it) }) {
            prefs.startHour = it
        }
        addSlider(col, 0, 23, prefs.endHour, { "Active until %02d:00".format(it) }) {
            prefs.endHour = it
        }

        // ---- Step 3: start / stop the foreground service ----
        toggleBtn = Button(this).apply { setOnClickListener { toggleService() } }
        col.addView(toggleBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(20) })

        // ---- Step 1/2/4: preview the pet ----
        col.addView(Button(this).apply {
            text = "Preview pet (appears in 3 seconds)"
            setOnClickListener { previewPet() }
        })

        col.addView(text(
            "Tip: on Xiaomi / Oppo / Vivo / Samsung phones, turn off battery " +
                "optimization for this app so reminders keep running.", 13f
        ).apply { setPadding(0, dp(16), 0, dp(2)) })
        col.addView(Button(this).apply {
            text = "Battery settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        })
    }

    private fun addSlider(
        parent: LinearLayout, min: Int, max: Int, initial: Int,
        fmt: (Int) -> String, onChange: (Int) -> Unit
    ) {
        val label = text(fmt(initial), 15f)
        val bar = SeekBar(this).apply {
            this.max = max - min
            progress = initial - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = p + min
                    label.text = fmt(v)
                    if (fromUser) onChange(v)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        parent.addView(label)
        parent.addView(bar)
    }

    private fun refreshPets() {
        petViews.forEachIndexed { i, tv ->
            tv.background = if (i == prefs.petIndex) GradientDrawable().apply {
                setColor(Color.parseColor("#BBDEFB"))
                cornerRadius = dp(14).toFloat()
            } else null
        }
    }

    private fun refresh() {
        val ok = Settings.canDrawOverlays(this)
        permStatus.text = if (ok) "✅ Overlay permission granted"
        else "⚠️ Needed so the pet can appear over other apps"
        permBtn.visibility = if (ok) View.GONE else View.VISIBLE
        val c = prefs.todayCount
        val g = prefs.goal
        progressText.text = "$c / $g glasses 💧"
        progressBar.max = g
        progressBar.progress = c.coerceAtMost(g)
        toggleBtn.text = if (prefs.enabled) "Stop reminders" else "Start reminders"
        refreshPets()
    }

    private fun requestOverlayPermission() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }

    private fun toggleService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }
        val intent = Intent(this, ReminderService::class.java)
        if (prefs.enabled) {
            stopService(intent)
            prefs.enabled = false
        } else {
            ContextCompat.startForegroundService(this, intent)
            prefs.enabled = true
            Toast.makeText(
                this, "${prefs.petName} will remind you every ${prefs.intervalMin} min", Toast.LENGTH_LONG
            ).show()
        }
        refresh()
    }

    private fun previewPet() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        Toast.makeText(this, "Switch to another app to see it!", Toast.LENGTH_SHORT).show()
        handler.postDelayed({ PetOverlay.show(applicationContext) }, 3000)
    }
}
