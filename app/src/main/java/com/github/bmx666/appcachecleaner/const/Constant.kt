package com.github.bmx666.appcachecleaner.const

import androidx.annotation.IntRange
import kotlin.coroutines.cancellation.CancellationException

class Constant {
    class Intent {
        class Settings {
            companion object {
                const val ACTION = "Settings"

                const val NAME_CLEAR_CACHE_TEXT_LIST = "clear_cache_text_list"
                const val NAME_CLEAR_DATA_TEXT_LIST = "clear_data_text_list"
                const val NAME_STORAGE_TEXT_LIST = "storage_text_list"
                const val NAME_OK_TEXT_LIST = "ok_text_list"

                const val NAME_DELAY_FOR_NEXT_APP_TIMEOUT = "delay_for_next_app_timeout"
                const val NAME_MAX_WAIT_APP_TIMEOUT = "max_wait_app_timeout"
                const val NAME_MAX_WAIT_CLEAR_CACHE_BUTTON_TIMEOUT = "max_wait_clear_cache_button_timeout"
                const val NAME_MAX_WAIT_ACCESSIBILITY_EVENT_TIMEOUT = "max_wait_accessibility_event_timeout"
                const val NAME_GO_BACK_AFTER_APPS = "go_back_after_apps"

                const val NAME_SCENARIO = "scenario"
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
                const val NAME_MESSAGE = "message"
                const val NAME_PACKAGE_NAME = "package_name"
            }
        }
    }

    class Bundle {
        class AppCacheCleanerActivity {
            companion object {
                const val KEY_SKIP_FIRST_RUN = "skip_first_run"
            }
        }

        class PackageFragment {
            companion object {
                const val KEY_PACKAGE_LIST_ACTION = "package_list_action"
                const val KEY_CUSTOM_LIST_NAME = "custom_list_name"
                const val KEY_HIDE_STATS = "hide_stats"
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
                @IntRange(from = 8 * 250)
                const val MAX_WAIT_APP_PERFORM_CLICK_MS = DEFAULT_WAIT_APP_PERFORM_CLICK_MS * 2
                @IntRange(from = 1)
                const val DEFAULT_PERFORM_CLICK_COUNT_TRIES = (DEFAULT_WAIT_APP_PERFORM_CLICK_MS - MIN_DELAY_PERFORM_CLICK_MS) / MIN_DELAY_PERFORM_CLICK_MS
                @IntRange(from = 0)
                const val MIN_WAIT_CLEAR_CACHE_BUTTON_MS = 0
                @IntRange(from = 0)
                const val DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS = 1000
                @IntRange(from = 0)
                const val MIN_DELAY_FOR_NEXT_APP_MS = 0
                @IntRange(from = 0)
                const val MAX_DELAY_FOR_NEXT_APP_MS = 10000
                @IntRange(from = 0)
                const val DEFAULT_DELAY_FOR_NEXT_APP_MS = 1000
                @IntRange(from = 0)
                const val DEFAULT_WAIT_ACCESSIBILITY_EVENT_MS = 2000
                @IntRange(from = 0)
                const val MIN_WAIT_ACCESSIBILITY_EVENT_MS = 250
                @IntRange(from = 0)
                const val MAX_WAIT_ACCESSIBILITY_EVENT_MS = 10000
                @IntRange(from = 0)
                const val DEFAULT_GO_BACK_AFTER_APPS = 25
                @IntRange(from = 0)
                const val MIN_GO_BACK_AFTER_APPS = 0
                @IntRange(from = 1)
                const val MIN_GO_BACK_AFTER_APPS_FOR_API_34 = 1
                @IntRange(from = 0)
                const val MAX_GO_BACK_AFTER_APPS = 50
            }
        }

        class Extra {
            companion object {
                const val DEFAULT_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS = false
                const val DEFAULT_SHOW_BUTTON_START_STOP_SERVICE = false
                const val DEFAULT_SHOW_BUTTON_CLOSE_APP = false
                const val DEFAULT_ACTION_STOP_SERVICE = false
                const val DEFAULT_ACTION_CLOSE_APP = false
            }
        }

        class Filter {
            companion object {
                const val DEFAULT_HIDE_DISABLED_APPS = false
                const val DEFAULT_HIDE_IGNORED_APPS = false
            }
        }

        class FirstBoot {
            companion object {
                // enable first boot by default
                const val DEFAULT_FIRST_BOOT = true
            }
        }

        class UI {
            companion object {
                // disable force Dark theme by default
                const val DEFAULT_FORCE_NIGHT_MODE = false
                // enable dynamic color by default
                const val DEFAULT_DYNAMIC_COLOR = true
            }
        }
    }

    class CancellationJobMessage {
        companion object {
            val CANCEL_IGNORE =
                CancellationException("cancel_ignore")
            val CANCEL_INIT =
                CancellationException("cancel_init")
            val CANCEL_INTERRUPTED_BY_SYSTEM =
                CancellationException("cancel_interrupted_by_system")
            val CANCEL_INTERRUPTED_BY_USER =
                CancellationException("cancel_interrupted_by_user")

            // valid only for package jobs
            val PACKAGE_WAIT_NEXT_STEP =
                CancellationException("package_wait_next_step")
            val PACKAGE_FINISH =
                CancellationException("package_finish")
            val PACKAGE_FINISH_FAILED =
                CancellationException("package_finish_failed")
        }
    }

    enum class Scenario {
        DEFAULT,
        XIAOMI_MIUI
    }

    enum class PackageListAction {
        DEFAULT,
        CUSTOM_ADD_EDIT,
        DISABLED_CLEAN,
        CUSTOM_CLEAN,
        IGNORED_APPS_EDIT
    }

    enum class Navigation {
        FIRST_BOOT,
        HOME,
        HELP,
        SETTINGS,
        PACKAGE_LIST,
    }
}