package com.nisha.ezyscreenrecorder

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nisha.ezyscreenrecorder.databinding.ActivityVideosListsScreenBinding

class VideosListsScreen : AppCompatActivity() {
    private lateinit var binding: ActivityVideosListsScreenBinding
    private val itemList = listOf("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4","http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4","http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4","http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4","http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4","http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4","http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4") // Sample list of string items

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosListsScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        getSupportActionBar()?.hide();

        val adapter = MyAdapter(itemList) { selectedItem ->
            // Handle item click
            val intent = Intent(this@VideosListsScreen, MainActivity::class.java)
            intent.putExtra("selectedItem", selectedItem)
            startActivity(intent)
        }

        binding. recyclerView.layoutManager = LinearLayoutManager(this)
        binding. recyclerView.adapter = adapter
    }
}