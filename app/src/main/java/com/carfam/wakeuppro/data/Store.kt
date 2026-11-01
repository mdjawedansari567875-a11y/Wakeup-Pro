package com.carfam.wakeuppro.data

import android.content.Context
import com.carfam.wakeuppro.games.History
import org.json.JSONArray
import org.json.JSONObject

/** All persistence lives here (SharedPreferences + JSON). */
class Store private constructor(ctx: Context) : History {

    private val sp = ctx.applicationContext.getSharedPreferences("wakeup_pro", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var inst: Store? = null

        fun get(c: Context): Store =
            inst ?: synchronized(this) { inst ?: Store(c).also { inst = it } }
    }

    // ---------------- Alarms ----------------

    fun alarms(): List<Alarm> {
        val arr = JSONArray(sp.getString("alarms", "[]") ?: "[]")
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Alarm(
                id = o.getInt("id"),
                hour = o.getInt("h"),
                minute = o.getInt("m"),
                days = o.getInt("d"),
                enabled = o.getBoolean("e"),
                label = o.optString("l", "")
            )
        }
    }

    fun saveAlarms(list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("h", it.hour).put("m", it.minute)
                    .put("d", it.days).put("e", it.enabled).put("l", it.label)
            )
        }
        sp.edit().putString("alarms", arr.toString()).apply()
    }

    fun alarm(id: Int): Alarm? = alarms().firstOrNull { it.id == id }

    fun upsert(a: Alarm) {
        val l = alarms().toMutableList()
        val i = l.indexOfFirst { it.id == a.id }
        if (i >= 0) l[i] = a else l.add(a)
        saveAlarms(l)
    }

    fun delete(id: Int) = saveAlarms(alarms().filter { it.id != id })

    fun nextId(): Int {
        val n = sp.getInt("next_id", 1)
        sp.edit().putInt("next_id", n + 1).apply()
        return n
    }

    // ---------------- Settings ----------------

    fun settings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            volume = sp.getInt("s_volume", d.volume),
            soundUri = sp.getString("s_sound", d.soundUri) ?: "",
            vibration = sp.getBoolean("s_vibration", d.vibration),
            darkTheme = sp.getBoolean("s_dark", d.darkTheme),
            difficulty = sp.getInt("s_diff", d.difficulty),
            is24h = sp.getBoolean("s_24h", d.is24h),
            challengeCount = sp.getInt("s_count", d.challengeCount)
        )
    }

    fun saveSettings(s: AppSettings) {
        sp.edit()
            .putInt("s_volume", s.volume)
            .putString("s_sound", s.soundUri)
            .putBoolean("s_vibration", s.vibration)
            .putBoolean("s_dark", s.darkTheme)
            .putInt("s_diff", s.difficulty)
            .putBoolean("s_24h", s.is24h)
            .putInt("s_count", s.challengeCount)
            .apply()
    }

    // ---------------- Challenge history (anti-repetition) ----------------

    override fun recentIds(): List<String> =
        (sp.getString("hist", "") ?: "").split("\n").filter { it.isNotEmpty() }

    override fun addId(id: String) {
        val l = recentIds().toMutableList()
        l.add(id)
        sp.edit().putString("hist", l.takeLast(400).joinToString("\n")).apply()
    }

    override fun lastCombo(): String = sp.getString("last_combo", "") ?: ""
    override fun setLastCombo(v: String) = sp.edit().putString("last_combo", v).apply()

    /** Adaptive difficulty offset, -2..+2, added to the chosen default difficulty. */
    fun adaptive(): Int = sp.getInt("adaptive", 0)
    fun setAdaptive(v: Int) = sp.edit().putInt("adaptive", v.coerceIn(-2, 2)).apply()

    // ---------------- Generic accessors (stopwatch / timer state) ----------------

    fun getLong(k: String, d: Long = 0L): Long = sp.getLong(k, d)
    fun putLong(k: String, v: Long) = sp.edit().putLong(k, v).apply()
    fun getStr(k: String, d: String = ""): String = sp.getString(k, d) ?: d
    fun putStr(k: String, v: String) = sp.edit().putString(k, v).apply()
    fun getBool(k: String, d: Boolean = false): Boolean = sp.getBoolean(k, d)
    fun putBool(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
}
