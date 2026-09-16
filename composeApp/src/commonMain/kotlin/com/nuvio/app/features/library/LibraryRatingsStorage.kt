package com.nuvio.app.features.library

internal expect object LibraryRatingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
