package com.nuvio.app.features.downloads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Bridges [DownloadsRepository] to the native Library header button (see
 * LibraryHeaderGlassButtons in ContentView.swift) — mirrors
 * [HeroTrailerMuteController][com.nuvio.app.features.details.HeroTrailerMuteController]'s
 * observe/stop lifecycle so Swift can start collecting on `.onAppear` and cancel on
 * `.onDisappear` without leaking a coroutine per navigation.
 */
class NativeDownloadsButtonController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observationJob: Job? = null

    /** [callback]'s `progress` is the running downloads' overall 0–1 progress, 0 when none is running. */
    fun observeState(callback: (isDownloading: Boolean, hasUnseenCompleted: Boolean, progress: Float) -> Unit) {
        DownloadsRepository.ensureLoaded()
        observationJob?.cancel()
        observationJob = scope.launch {
            combine(
                DownloadsRepository.uiState,
                DownloadsRepository.hasUnseenCompleted,
            ) { uiState, hasUnseenCompleted ->
                val progress = uiState.downloadingProgress
                Triple(progress != null, hasUnseenCompleted, progress ?: 0f)
            }.collect { (isDownloading, hasUnseenCompleted, progress) ->
                callback(isDownloading, hasUnseenCompleted, progress)
            }
        }
    }

    fun stopObserving() {
        observationJob?.cancel()
        observationJob = null
    }
}
