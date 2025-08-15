package com.example.usedpalace.profileMenus

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import network.ApiService
import org.json.JSONObject

class InformationActivity : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences
    private var saleId: Int = -1
    private var sellerId: Int = -1
    private var buyerId: Int = -1

    // Views
    private lateinit var imageSlider: ViewPager2
    private lateinit var productTitle: TextView
    private lateinit var versionTextView: TextView
    private lateinit var changeLogTextView: TextView
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var messageButton: Button
    private lateinit var buttonBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_information)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()

        showChangeLog(this, changeLogTextView)
        showLastVersion(this, versionTextView)
    }

    private fun initializeViews() {
        buttonBack = findViewById(R.id.buttonBack)
        changeLogTextView = findViewById(R.id.changeLogText)
        versionTextView = findViewById(R.id.lastVersion)
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            navigateBackToProfile()

        }
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