package com.example.banudaapi

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.banuba.example.videoeditor.utils.GetMultipleContents
import com.banuba.sdk.token.storage.license.EditorLicenseManager
import com.banuba.sdk.token.storage.provider.TokenProvider
import com.example.banudaapi.databinding.ActivityMainBinding
import com.example.banudaapi.video_editor.VideoEditorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val tokenProvider: TokenProvider by inject(named("banubaTokenProvider"))

    private val selectVideos = registerForActivityResult(GetMultipleContents()) {
        if (it.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main.immediate).launch {
                EditorLicenseManager.initialize(tokenProvider.getToken())
            }
            openVideoEditor(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.buttonGetVideo.setOnClickListener {
            selectVideos.launch("video/*")
        }
    }

    private fun openVideoEditor(videos: List<Uri>) {
        val intent = VideoEditorActivity.createIntent(this, videos)
        startActivity(intent)
    }
}