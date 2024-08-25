package com.github.bmx666.appcachecleaner.const

import android.os.Build
import androidx.annotation.IntRange
import kotlin.coroutines.cancellation.CancellationException

class Constant {
    class Intent {
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

        class ClearData {
            companion object {
                const val ACTION = "ClearData"
                const val NAME_PACKAGE_LIST = "package_list"
            }
        }

        class AppInfo {
            companion object {
                const val ACTION = "AppInfo"
                const val NAME_PACKAGE_NAME = "package_name"
            }
        }

        class ClearCacheFinish {
            companion object {
                const val ACTION = "ClearCacheFinish"
                const val NAME_MESSAGE = "message"
                const val NAME_PACKAGE_NAME = "package_name"
            }
        }

        class ClearDataFinish {
            companion object {
                const val ACTION = "ClearDataFinish"
                const val NAME_MESSAGE = "message"
                const val NAME_PACKAGE_NAME = "package_name"
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
                val MIN_GO_BACK_AFTER_APPS =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 1
                    else 0
                @IntRange(from = 0)
                val DEFAULT_GO_BACK_AFTER_APPS =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 25
                    else MIN_GO_BACK_AFTER_APPS
                @IntRange(from = 0)
                const val MAX_GO_BACK_AFTER_APPS = 50
                @IntRange(from = 1)
                const val DEFAULT_FORCE_STOP_TRIES = 2
            }
        }

        class Extra {
            companion object {
                const val DEFAULT_SHOW_BUTTON_CLEAN_CACHE_DISABLED_APPS = false
                const val DEFAULT_SHOW_BUTTON_START_STOP_SERVICE = false
                const val DEFAULT_SHOW_BUTTON_CLOSE_APP = false
                const val DEFAULT_SHOW_BUTTON_CLEAR_DATA = false
                const val DEFAULT_ACTION_FORCE_STOP_APPS = false
                const val DEFAULT_ACTION_STOP_SERVICE = false
                const val DEFAULT_ACTION_CLOSE_APP = false
            }
        }

        class Filter {
            companion object {
                const val DEFAULT_HIDE_DISABLED_APPS = false
                const val DEFAULT_HIDE_IGNORED_APPS = true
            }
        }

        class FirstBoot {
            companion object {
                // enable first boot by default
                const val DEFAULT_FIRST_BOOT = true
            }
        }

        class UI {
            enum class Contrast {
                STANDARD,
                MEDIUM,
                HIGH,
            }

            companion object {
                // disable force Dark theme by default
                const val DEFAULT_FORCE_NIGHT_MODE = false
                // enable dynamic color by default
                const val DEFAULT_DYNAMIC_COLOR = true
                // dynamic color contrast by default
                val DEFAULT_CONTRAST = Contrast.STANDARD
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
            val PACKAGE_WAIT_DIALOG =
                CancellationException("package_wait_dialog")
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
        USER_APPS,
        SYSTEM_APPS,
        ALL_APPS,
        DISABLED_APPS,
        IGNORED_APPS,
        CUSTOM_LIST_ADD,
        CUSTOM_LIST_EDIT,
    }

    enum class Navigation {
        FIRST_BOOT,
        HOME,
        HELP,
        SETTINGS,
        PACKAGE_LIST,
    }
}