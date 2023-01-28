package com.github.bmx666.appcachecleaner.const

class Constant {
    class Intent {
        class ExtraSearchText {
            companion object {
                const val ACTION = "ExtraSearchText"
                const val NAME_CLEAR_CACHE_TEXT_LIST = "clear_cache_text_list"
                const val NAME_STORAGE_TEXT_LIST = "storage_text_list"
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
}