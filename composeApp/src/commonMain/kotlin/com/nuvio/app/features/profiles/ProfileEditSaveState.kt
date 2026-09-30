package com.nuvio.app.features.profiles

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Bridges [ProfileEditScreen]'s Save button (enabled/saving state, and the save action itself) to
 * a native SwiftUI overlay button (see ProfileEditSaveButton in ContentView.swift) — mirrors
 * [HeroTrailerAudioState][com.nuvio.app.features.details.HeroTrailerAudioState]'s split between a
 * plain state singleton and a per-view [NativeProfileEditSaveController]. Only one Edit Profile
 * screen is ever on-screen at a time, so a single shared singleton (rather than something keyed
 * per-instance) is fine.
 */
object ProfileEditSaveState {
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val saveRequestChannel = Channel<Unit>(Channel.BUFFERED)
    internal val saveRequests: Flow<Unit> = saveRequestChannel.receiveAsFlow()

    internal fun report(enabled: Boolean, saving: Boolean) {
        _enabled.value = enabled
        _saving.value = saving
    }

    /** Called when an Edit Profile screen leaves composition, so a native button that briefly
     * lingers mid-transition never reads as enabled/tappable for a screen that's already gone. */
    internal fun reset() {
        _enabled.value = false
        _saving.value = false
    }

    internal fun requestSave() {
        saveRequestChannel.trySend(Unit)
    }
}

/**
 * Bridges [ProfileEditSaveState] to a native SwiftUI overlay button — mirrors
 * [HeroTrailerMuteController][com.nuvio.app.features.details.HeroTrailerMuteController]'s
 * observe/stop lifecycle so Swift can start collecting on `.onAppear` and cancel on
 * `.onDisappear` without leaking a coroutine per navigation.
 */
class NativeProfileEditSaveController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observationJob: Job? = null

    fun observeState(callback: (enabled: Boolean, saving: Boolean) -> Unit) {
        observationJob?.cancel()
        observationJob = scope.launch {
            combine(ProfileEditSaveState.enabled, ProfileEditSaveState.saving) { enabled, saving ->
                enabled to saving
            }.collect { (enabled, saving) -> callback(enabled, saving) }
        }
    }

    fun stopObserving() {
        observationJob?.cancel()
        observationJob = null
    }

    fun requestSave() {
        ProfileEditSaveState.requestSave()
    }
}
