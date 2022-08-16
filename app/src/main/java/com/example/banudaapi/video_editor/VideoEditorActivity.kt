package com.example.banudaapi.video_editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.banuba.sdk.export.data.ExportResult
import com.example.banudaapi.databinding.ActivityVideoEditorBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class VideoEditorActivity : AppCompatActivity() {
    private var count = 0

    companion object {

        private const val EXTRA_PREDEFINED_VIDEOS = "EXTRA_PREDEFINED_VIDEOS"

        fun createIntent(context: Context, predefinedVideos: List<Uri>): Intent {
            return Intent(context, VideoEditorActivity::class.java).apply {
                val listData = ArrayList(predefinedVideos)
                putParcelableArrayListExtra(EXTRA_PREDEFINED_VIDEOS, listData)
            }
        }
    }

    private val predefinedVideos by lazy(LazyThreadSafetyMode.NONE) {
        intent.getParcelableArrayListExtra<Uri>(EXTRA_PREDEFINED_VIDEOS)
    }

    private val binding: ActivityVideoEditorBinding by lazy {
        ActivityVideoEditorBinding.inflate(
            layoutInflater
        )
    }
    private val viewModel by viewModel<VideoEditorViewModel>()

    private var videoPath: Uri = Uri.EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val extras = intent.extras?.getString("URI")
        extras?.let {
            videoPath = Uri.parse(it)
        }
        setupView()
    }

    private fun setupView() = with(binding) {
        predefinedVideos?.toTypedArray()?.let {
            viewModel.addVideosToPlayback(it)
        }

        viewModel.prepare(surfaceView.holder)

        btnFilter.setOnClickListener {
            if (count % 2 == 0) {
                viewModel.applyLutEffect()
            } else {
                viewModel.removeLutEffect()
            }
            count++
        }

        btnExport.setOnClickListener {
            viewModel.startExport()
        }


        viewModel.exportResultData.observe(this@VideoEditorActivity) { exportResult ->
            when (exportResult) {
                is ExportResult.Inactive, is ExportResult.Stopped -> {
                }

                is ExportResult.Progress -> {}
                is ExportResult.Success -> {
                    Toast.makeText(
                        this@VideoEditorActivity,
                        "Export Success: ${exportResult.videoList}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is ExportResult.Error -> {
                    Toast.makeText(
                        this@VideoEditorActivity,
                        getString(exportResult.type.messageResId),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.start()
    }

    override fun onStop() {
        super.onStop()
        viewModel.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer(binding.surfaceView.holder)
    }
}