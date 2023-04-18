package com.github.bmx666.appcachecleaner.const

import androidx.annotation.IntRange

class Constant {
    class Intent {
        class ExtraSearchText {
            companion object {
                const val ACTION = "ExtraSearchText"
                const val NAME_CLEAR_CACHE_TEXT_LIST = "clear_cache_text_list"
                const val NAME_STORAGE_TEXT_LIST = "storage_text_list"
            }
        }

        class Scenario {
            companion object {
                const val ACTION = "Scenario"
                const val NAME_TYPE = "type"
            }
        }

        class StopAccessibilityService {
            companion object {
                const val ACTION = "StopAccessibilityService"
            }
        }

        class StopAccessibilityServiceFeedback {
            companion object {
                const val ACTION = "StopAccessibilityServiceFeedback"
            }
        }

        class ClearCache {
            companion object {
                const val ACTION = "ClearCache"
                const val NAME_PACKAGE_LIST = "package_list"
                const val NAME_MAX_WAIT_APP_TIMEOUT = "max_wait_app_timeout"
            }
        }

        class CleanCacheAppInfo {
            companion object {
                const val ACTION = "CleanCacheAppInfo"
                const val NAME_PACKAGE_NAME = "package_name"
            }
        }

        class CleanCacheFinish {
            companion object {
                const val ACTION = "CleanCacheFinish"
                const val NAME_INTERRUPTED = "interrupted"
            }
        }
    }

    class Bundle {
        class PackageFragment {
            companion object {
                const val KEY_CUSTOM_LIST_NAME = "custom_list_name"
            }
        }
    }

    class Settings {
        class CacheClean {
            companion object {
                @IntRange(from = 250)
                const val MIN_DELAY_PERFORM_CLICK_MS = 250
                @IntRange(from = 8 * 250)
                const val MIN_WAIT_APP_PERFORM_CLICK_MS = 8 * MIN_DELAY_PERFORM_CLICK_MS
                @IntRange(from = 8 * 250)
                const val DEFAULT_WAIT_APP_PERFORM_CLICK_MS = 30000
            }
        }
    }

    enum class Scenario {
        DEFAULT,
    }
}