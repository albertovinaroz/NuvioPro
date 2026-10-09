package com.nuvio.app.features.settings

import platform.Foundation.NSUserDefaults

actual object ProfileSectionsStorage {
    private const val expandedKey = "nuvio_profile_sections.expanded"

    actual fun loadExpanded(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(expandedKey)

    actual fun saveExpanded(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = expandedKey)
    }
}
