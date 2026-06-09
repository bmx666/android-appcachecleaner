package com.github.bmx666.appcachecleaner.model

import java.util.Locale

// Pure domain model: no Android types, so all list filter/sort/selection logic over it is
// plain-JVM unit-testable without Robolectric. The PackageInfo handle (needed only to
// re-query cache size after a clean) lives separately in PackageRepository's name-keyed
// handle map. `cacheBytes` is the internal cache size already extracted at the Android edge
// (0 on pre-O or on error). Selection ("checked") is NOT a field here: it lives as a
// Set<String> of package names in PackageRepository, decoupled from these objects so it
// survives re-sort/re-filter without touching the list elements.
data class AppPackage(
    val name: String,
    var label: String,
    var locale: Locale,
    var cacheBytes: Long,
    var visible: Boolean,
    var ignore: Boolean,
) {
    override fun toString(): String = name
}
