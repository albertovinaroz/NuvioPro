package com.nuvio.app.features.notifications

internal expect object NotificationFeedStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
