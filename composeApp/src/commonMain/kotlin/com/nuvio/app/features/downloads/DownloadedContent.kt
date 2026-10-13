package com.nuvio.app.features.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
private fun rememberDownloadsUiState(): DownloadsUiState {
    val downloads by remember {
        DownloadsRepository.ensureLoaded()
        DownloadsRepository.uiState
    }.collectAsStateWithLifecycle()
    return downloads
}

/** Ids of movies with a finished download — see [DownloadsUiState.downloadedMetaIds]. */
@Composable
fun rememberDownloadedMetaIds(): Set<String> = rememberDownloadsUiState().downloadedMetaIds

/** Whether this episode of [parentMetaId] has a finished download. */
@Composable
fun rememberIsEpisodeDownloaded(parentMetaId: String, seasonNumber: Int?, episodeNumber: Int?): Boolean =
    "${parentMetaId.trim()}|${seasonNumber ?: -1}|${episodeNumber ?: -1}" in
        rememberDownloadsUiState().downloadedEpisodeKeys
