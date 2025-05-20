package com.example.usedpalace.profilemenus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import org.json.JSONArray
import org.json.JSONObject

class InformationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_information)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val buttonBack: Button = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            navigateBackToProfile()

        }

        val changeLogTextView = findViewById<TextView>(R.id.changeLogText)
        showChangeLog(this, changeLogTextView)

        val versionTextView = findViewById<TextView>(R.id.lastVersion)
        showLastVersion(this, versionTextView)
    }
    private fun navigateBackToProfile() {
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            putExtra("SHOW_PROFILE_FRAGMENT", true)

            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        navigateBackToProfile()
    }

    private fun showChangeLog(context: Context, textView: TextView) {
        val changelogText = buildFullChangeLog(context)
        textView.text = changelogText
    }

    private fun buildFullChangeLog(context: Context): String {
        val inputStream = context.assets.open("changeLog.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        val jsonObject = JSONObject(jsonString)
        val versionsArray = jsonObject.getJSONArray("versions")

        val stringBuilder = StringBuilder()

        for (i in 0 until versionsArray.length()) {
            val versionEntry = versionsArray.getJSONObject(i)
            val version = versionEntry.getString("version")
            val changesArray = versionEntry.getJSONArray("changes")

            stringBuilder.append("Verzió: $version\n")

            for (j in 0 until changesArray.length()) {
                val change = changesArray.getString(j)
                stringBuilder.append("- $change\n")
            }

            stringBuilder.append("\n")
        }

        return stringBuilder.toString()
    }

    private fun showLastVersion(context: Context, textView: TextView) {
        val inputStream = context.assets.open("changeLog.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        val jsonObject = JSONObject(jsonString)
        val versionsArray = jsonObject.getJSONArray("versions")
        val latestVersionEntry = versionsArray.getJSONObject(versionsArray.length() - 1)
        val version = latestVersionEntry.getString("version")

        textView.text = "Legutóbbi verzió: $version"
    }



    private fun readChangeLog(context: Context): Pair<String, List<String>> {
        val inputStream = context.assets.open("changeLog.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        val jsonObject = JSONObject(jsonString)
        val versionsArray = jsonObject.getJSONArray("versions")
        val firstEntry = versionsArray.getJSONObject(0)

        val version = firstEntry.getString("version")
        val changesJsonArray = firstEntry.getJSONArray("changes")

        val changesList = mutableListOf<String>()
        for (i in 0 until changesJsonArray.length()) {
            changesList.add(changesJsonArray.getString(i))
        }

        return Pair(version, changesList)
    }

}