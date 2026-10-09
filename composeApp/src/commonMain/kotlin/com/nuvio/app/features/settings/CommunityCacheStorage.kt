package com.nuvio.app.features.settings

/** Device-wide cache for the Supporters & Contributors page (not profile scoped). */
internal expect object CommunityCacheStorage {
    fun load(key: String): String?
    fun save(key: String, payload: String?)
}
