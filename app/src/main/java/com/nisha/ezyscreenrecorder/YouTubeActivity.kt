package com.nisha.ezyscreenrecorder


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat



class YouTubeActivity : AppCompatActivity() {

    private lateinit var button: Button
    private val REQUEST_CODE_PICK_VIDEO = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_you_tube)
        getSupportActionBar()?.setTitle("Reaction Video Maker");

        button = findViewById(R.id.button)

        button.setOnClickListener {
            if (VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pickVideo()
            }
            else
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted, request it from the user
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1011
                )
            } else {
                // Permission is already granted, launch video picker
                pickVideo()
            }



        }
    }
    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_CODE_PICK_VIDEO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->

                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("selectedItem", uri.toString())
                    startActivity(intent)


            }
        }
    }
}
