package com.example.usedpalace.fragments.homeFragmentHelpers

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.usedpalace.R

class FullscreenGalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fullscreen_gallery)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val viewPager = findViewById<ViewPager2>(R.id.fullScreenViewPager)

        val imageUrls = intent.getStringArrayListExtra("IMAGE_URLS") ?: arrayListOf()
        val startPosition = intent.getIntExtra("POSITION", 0)

        val adapter = FullscreenGalleryAdapter(this, imageUrls)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(startPosition, false)

        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        closeButton.setOnClickListener {
            finish() // bezárja a fullscreen Activity-t
        }
    }
}