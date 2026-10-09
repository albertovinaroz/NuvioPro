package com.nuvio.app.features.settings

import platform.Foundation.NSUserDefaults

actual object CommunityCacheStorage {
    private const val prefix = "nuvio_community_cache."

    actual fun load(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(prefix + key)

    actual fun save(key: String, payload: String?) {
        if (payload == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(prefix + key)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(payload, forKey = prefix + key)
        }
    }
}
