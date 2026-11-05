package com.waterpet.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Pets {
    val list = listOf(
        "🐱" to "Cat",
        "🐶" to "Dog",
        "🐹" to "Hamster",
        "🐰" to "Bunny",
        "🦊" to "Fox"
    )
}

/** Simple settings + daily water counter storage. */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("waterpet", Context.MODE_PRIVATE)

    var petIndex: Int
        get() = sp.getInt("pet", 0).coerceIn(0, Pets.list.size - 1)
        set(v) = sp.edit().putInt("pet", v).apply()

    var petName: String
        get() = sp.getString("name", "Mochi") ?: "Mochi"
        set(v) = sp.edit().putString("name", v.ifBlank { "Mochi" }).apply()

    /** Daily goal in glasses. */
    var goal: Int
        get() = sp.getInt("goal", 8)
        set(v) = sp.edit().putInt("goal", v).apply()

    /** Minutes between reminders. */
    var intervalMin: Int
        get() = sp.getInt("interval", 45)
        set(v) = sp.edit().putInt("interval", v).apply()

    var startHour: Int
        get() = sp.getInt("startHour", 8)
        set(v) = sp.edit().putInt("startHour", v).apply()

    var endHour: Int
        get() = sp.getInt("endHour", 22)
        set(v) = sp.edit().putInt("endHour", v).apply()

    /** True when the user has turned reminders on. */
    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    val petEmoji: String get() = Pets.list[petIndex].first

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val todayCount: Int
        get() = if (sp.getString("day", "") == today()) sp.getInt("count", 0) else 0

    fun addGlass(): Int {
        val c = todayCount + 1
        sp.edit().putString("day", today()).putInt("count", c).apply()
        return c
    }

    /** Whether the current hour is inside the user's active hours. */
    fun isActiveNow(): Boolean {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            startHour == endHour -> true // treated as "all day"
            startHour < endHour -> h in startHour until endHour
            else -> h >= startHour || h < endHour // overnight window
        }
    }
}
