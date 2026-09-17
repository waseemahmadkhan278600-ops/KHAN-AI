package com.khan.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread
import org.json.JSONObject

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var input: EditText
    private lateinit var chat: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private val prefs by lazy { getSharedPreferences("khan", MODE_PRIVATE) }
    private val REQ_VOICE = 55

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)
        input = findViewById(R.id.input); chat = findViewById(R.id.chat)
        scroll = findViewById(R.id.scroll); status = findViewById(R.id.status)
        tts = TextToSpeech(this, this)

        findViewById<Button>(R.id.sendBtn).setOnClickListener { send() }
        findViewById<Button>(R.id.voiceBtn).setOnClickListener { listen() }
        findViewById<Button>(R.id.settingsBtn).setOnClickListener { apiDialog() }

        addMessage("Khan", "Hello! Main Khan hoon. Aap mujhse Hindi ya English mein baat kar sakte ho.")
    }

    private fun addMessage(who: String, text: String) {
        val tv = TextView(this)
        tv.text = "$who\n$text"
        tv.textSize = 16f
        tv.setTextColor(getColor(R.color.text))
        tv.setPadding(18, 14, 18, 14)
        tv.background = getDrawable(R.drawable.card_bg)
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 8, 0, 8)
        chat.addView(tv, lp)
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun send() {
        val q = input.text.toString().trim()
        if (q.isEmpty()) return
        input.setText("")
        addMessage("You", q)
        val key = prefs.getString("key", "") ?: ""
        if (key.isEmpty()) {
            addMessage("Khan", "API key set nahi hai. ⚙ API Key par tap karke apni AI API key add karein.")
            return
        }
        status.text = "● Thinking..."
        thread {
            try {
                val answer = callOpenAICompatible(key, q)
                runOnUiThread {
                    status.text = "● Ready"
                    addMessage("Khan", answer)
                    tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "khan")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "● Ready"
                    addMessage("Khan", "Connection error: ${e.message}")
                }
            }
        }
    }

    private fun callOpenAICompatible(key: String, q: String): String {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val c = url.openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("Authorization", "Bearer $key")
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply { put("role","system"); put("content","You are Khan AI, a helpful personal assistant. Reply in the user's language.") })
                put(JSONObject().apply { put("role","user"); put("content",q) })
            })
        }.toString()
        c.outputStream.use { it.write(body.toByteArray()) }
        val text = c.inputStream.bufferedReader().readText()
        val obj = JSONObject(text)
        return obj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    }

    private fun apiDialog() {
        val e = EditText(this)
        e.hint = "Paste API key"
        e.setText(prefs.getString("key",""))
        AlertDialog.Builder(this).setTitle("Khan AI — API Key")
            .setMessage("Key phone par locally save hogi. Kisi ke saath share na karein.")
            .setView(e)
            .setPositiveButton("Save") { _, _ -> prefs.edit().putString("key", e.text.toString().trim()).apply() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun listen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 99); return
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        startActivityForResult(i, REQ_VOICE)
    }

    override fun onActivityResult(r: Int, code: Int, data: Intent?) {
        super.onActivityResult(r, code, data)
        if (r == REQ_VOICE && code == Activity.RESULT_OK) {
            val s = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!s.isNullOrBlank()) { input.setText(s); send() }
        }
    }

    override fun onInit(code: Int) { if (code == TextToSpeech.SUCCESS) tts.language = Locale("hi","IN") }
    override fun onDestroy() { tts.shutdown(); super.onDestroy() }
}
