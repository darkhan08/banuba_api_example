package com.example.banudaapi.video_editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.banuba.sdk.core.data.MediaDataGalleryValidator
import com.banuba.sdk.core.data.MediaValidationResultType
import com.banuba.sdk.core.domain.AspectRatioProvider
import com.banuba.sdk.core.effects.DrawType
import com.banuba.sdk.core.ext.copyFromAssetsToExternal
import com.banuba.sdk.core.gl.GlViewport
import com.banuba.sdk.effects.ve.VideoEffectsHelper
import com.banuba.sdk.export.data.ExportFlowManager
import com.banuba.sdk.export.data.ExportResult
import com.banuba.sdk.export.data.ExportTaskParams
import com.banuba.sdk.playback.PlaybackError
import com.banuba.sdk.playback.PlayerScaleType
import com.banuba.sdk.playback.VideoPlayer
import com.banuba.sdk.ve.domain.VideoRangeList
import com.banuba.sdk.ve.domain.VideoRecordRange
import com.banuba.sdk.ve.effects.Effects
import com.banuba.sdk.ve.effects.TypedTimedEffect
import com.banuba.sdk.ve.effects.VisualTimedEffect
import com.banuba.sdk.ve.effects.music.MusicEffect
import com.banuba.sdk.ve.ext.VideoEditorUtils
import com.example.banudaapi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class VideoEditorViewModel(
    private val appContext: Application,
    private val videoValidator: MediaDataGalleryValidator,
    private val videoPlayer: VideoPlayer,
    private val exportFlowManager: ExportFlowManager,
    private val aspectRatioProvider: AspectRatioProvider
) : AndroidViewModel(appContext) {

    private val appliedEffects = mutableListOf<TypedTimedEffect<*>>()
    private var exportVideosList = mutableListOf<VideoRecordRange>()
    private var exportMusicList = mutableListOf<MusicEffect>()

    val exportResultData: LiveData<ExportResult> = MediatorLiveData<ExportResult>().apply {
        addSource(exportFlowManager.resultData) { exportResult ->
            value = exportResult
        }
    }

    private val videoPlayerCallback = object : VideoPlayer.Callback {
        override fun onScreenshotTaken(bmp: Bitmap) {}

        override fun onVideoPlaybackError(error: PlaybackError) {
        }

        override fun onVideoPositionChanged(positionMs: Int) {
        }

        override fun onViewportChanged(viewport: GlViewport) {
        }
    }

    fun addVideosToPlayback(videos: Array<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = videos
                .filter { videoValidator.getValidationResult(it) == MediaValidationResultType.VALID_FILE }
                .mapNotNull { VideoEditorUtils.createVideoRecordRange(it, appContext) }
            videoPlayer.setVideoRanges(playlist)
            exportVideosList.addAll(playlist)
        }
    }

    fun prepare(holder: SurfaceHolder) {
        if (videoPlayer.prepare(Size(720, 1280))) {
            with(videoPlayer) {
                setSurfaceHolder(holder)
                setCallback(videoPlayerCallback)
                setScaleType(PlayerScaleType.FIT_SCREEN_HEIGHT)
                setVideoSize(Size(720, 1280))
                setVolume(1f)
            }
        } else {
            Log.e("@@@###", "Error while prepare video editor player")
        }
    }

    fun start() {
        videoPlayer.play(true)
    }

    fun pause() {
        videoPlayer.pause()
    }

    fun releasePlayer(holder: SurfaceHolder) {
        videoPlayer.clearSurfaceHolder(holder)
        videoPlayer.release()
    }

    fun applyLutEffect() {
        val lutEffect = createColorFilterEffect()
        appliedEffects.add(lutEffect)
        videoPlayer.setEffects(appliedEffects)
    }

    fun removeLutEffect() {
        appliedEffects.removeAll { it.drawable.type == DrawType.COLOR }
        videoPlayer.setEffects(appliedEffects)
    }

    private fun createColorFilterEffect(): VisualTimedEffect {
        val colorEffectFile = appContext.copyFromAssetsToExternal("banuba/vivid.png")
        return VisualTimedEffect(
            VideoEffectsHelper.createLutEffect(
                colorEffectFile.path,
                Size(1024, 768)
            )
        )
    }

    fun startExport() {
        val visualStack = Stack<VisualTimedEffect>().apply {
            addAll(appliedEffects.filterIsInstance<VisualTimedEffect>())
        }

        val exportTaskParams = ExportTaskParams(
            videoRanges = VideoRangeList(exportVideosList),
            effects = Effects(visualStack),
            videoVolume = 1f,
            coverFrameSize = Size(720, 1280),
            aspect = aspectRatioProvider.provide(),
            musicEffects = exportMusicList
        )

        exportFlowManager.startExport(exportTaskParams)
    }
}