package com.nuvio.app.core.build

enum class TrailerPlaybackMode {
    IN_APP,
    EXTERNAL,
}

expect object AppFeaturePolicy {
    val pluginsEnabled: Boolean
    val supportersContributorsPageEnabled: Boolean
    val donationActionsEnabled: Boolean
    val donationProgressEnabled: Boolean
    val accountDeletionEnabled: Boolean
    val personalMediaAddonCopyEnabled: Boolean
    val p2pEnabled: Boolean
    val trailerPlaybackMode: TrailerPlaybackMode
    val heroTrailerPlaybackSupported: Boolean
    val inAppUpdaterEnabled: Boolean
    /** Passive counterpart to [inAppUpdaterEnabled] for channels that can't download/install their
     * own update — a new release drops a card into the notification feed instead of a banner. */
    val altStoreUpdateAlertsEnabled: Boolean
    val imdbRatingLogoEnabled: Boolean
    val mediaPlaybackForegroundServiceEnabled: Boolean
    val downloadForegroundServiceEnabled: Boolean
    val customServerConnectionsEnabled: Boolean
}
