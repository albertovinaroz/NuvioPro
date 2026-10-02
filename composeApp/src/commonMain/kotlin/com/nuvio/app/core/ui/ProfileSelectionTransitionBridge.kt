package com.nuvio.app.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * One-shot "a profile was just tapped" signal — fired the instant AppGate commits to the exit
 * transition (same synchronous call as beginProfileTransition(), so there's no extra delay added
 * on the Kotlin side). Exists because Compose Multiplatform's iOS render pipeline has inherent
 * multi-frame latency between a state change and a *newly started* animation's first visible
 * frame (recompose -> relaunch LaunchedEffect -> dispatch coroutine -> first tick) — native
 * SwiftUI/UIKit, reacting to this signal directly, can respond on the very next run loop tick
 * instead. See ProfileEditSaveState.kt for the same one-shot-Channel shape used for the same
 * reason (a request a single, specific collector should react to immediately).
 */
internal object ProfileSelectionTransitionState {
    private val tapChannel = Channel<Unit>(Channel.BUFFERED)
    val tapRequests: Flow<Unit> = tapChannel.receiveAsFlow()

    fun requestTransition() {
        tapChannel.trySend(Unit)
    }
}

/**
 * Bridges [ProfileSelectionTransitionState] to native Swift — mirrors
 * [HeroTrailerMuteController][com.nuvio.app.features.details.HeroTrailerMuteController]'s
 * observe/stop lifecycle so Swift can start collecting on `.onAppear` and cancel on
 * `.onDisappear` without leaking a coroutine. A fresh instance per SwiftUI view is fine — it just
 * observes the same underlying singleton.
 */
class NativeProfileSelectionTransitionController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observationJob: Job? = null

    fun observeState(callback: () -> Unit) {
        observationJob?.cancel()
        observationJob = scope.launch {
            ProfileSelectionTransitionState.tapRequests.collect { callback() }
        }
    }

    fun stopObserving() {
        observationJob?.cancel()
        observationJob = null
    }
}
