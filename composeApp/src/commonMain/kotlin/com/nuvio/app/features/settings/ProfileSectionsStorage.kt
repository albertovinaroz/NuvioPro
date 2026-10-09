package com.nuvio.app.features.settings

/** Which collapsible Profile sections are open, device wide, as comma-separated section keys. */
internal expect object ProfileSectionsStorage {
    fun loadExpanded(): String?
    fun saveExpanded(value: String)
}
