package com.example.banudaapi.di

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.banuba.example.videoeditor.export.CustomExportParamsProvider
import com.banuba.example.videoeditor.export.CustomPublishManager
import com.banuba.example.videoeditor.export.EnableExportAudioProvider
import com.banuba.sdk.core.domain.ImageLoader
import com.banuba.sdk.export.data.ExportFlowManager
import com.banuba.sdk.export.data.ExportParamsProvider
import com.banuba.sdk.export.data.ForegroundExportFlowManager
import com.banuba.sdk.export.data.PublishManager
import com.banuba.sdk.token.storage.provider.TokenProvider
import com.banuba.sdk.ve.effects.watermark.WatermarkProvider
import com.banuba.sdk.ve.media.VideoGalleryResourceValidator
import com.example.banudaapi.R
import com.example.banudaapi.video_editor.VideoEditorViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

class ViewModelModule {

    val model = module {

        viewModel {
            VideoEditorViewModel(
                appContext = androidApplication(),
                videoValidator = VideoGalleryResourceValidator(context = androidContext()),
                videoPlayer = get(),
                exportFlowManager = get(),
                aspectRatioProvider = get()
            )
        }

        single<TokenProvider>(named("banubaTokenProvider")) {
            object : TokenProvider {
                override fun getToken(): String =
                    androidContext().getString(R.string.banuba_token)
            }
        }

        single<ExportFlowManager> {
            ForegroundExportFlowManager(
                exportDataProvider = get(),
                sessionParamsProvider = get(),
                exportSessionHelper = get(),
                exportDir = get(named("exportDir")),
                publishManager = get(),
                errorParser = get(),
                mediaFileNameHelper = get(),
                exportBundleProvider = get()
            )
        }

        factory<ExportParamsProvider> {
            CustomExportParamsProvider(
                exportDir = get(named("exportDir")),
                mediaFileNameHelper = get(),
                watermarkBuilder = get(),
                exportAudioProvider = get()
            )
        }

        single<PublishManager> {
            CustomPublishManager(
                context = androidContext(),
                albumName = "Banuba Api",
                mediaFileNameHelper = get(),
                dispatcher = Dispatchers.IO
            )
        }

        single<EnableExportAudioProvider> {
            object : EnableExportAudioProvider {
                override var isEnable: Boolean = false
            }
        }

    }
}