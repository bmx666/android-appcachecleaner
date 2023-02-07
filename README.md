# Cache Cleaner

## Description

Since Android 6 (Marshmallow) it is no longer possible to clear cache of all apps at same time and Google has moved this permission to system apps only. There is only one possible way left - open manually info about application and find specific "Storage" menu and then press "Clean cache" button.

**Cache Cleaner** app can request all installed user and system apps and it replaces all manual actions related to clean cache using Accessibility service.

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.github.bmx666.appcachecleaner)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.github.bmx666.appcachecleaner/)

or get the APK from the [Releases Section](https://github.com/bmx666/android-appcachecleaner/releases/latest).

## List of banned smartphones on Google Play forever due to a large number of negative reviews 🚫

* lge mdh5lm (LG K31) - WindowManager modified by LGE, the Accessibility overlay button cannot be displayed over the screen causing the service to crash
* realme RMX3191 (realme C25) - even after response users are to lazy to read "Help", chapter "Customized Settings UI"

## Warning ⚠️
Since Android 13 Accessibility APIs is restricted for 3rd party apps, the solution can be found [here](https://support.google.com/android/answer/12623953) at the paragraph **Allow restricted settings**.

## screenshots

|Main Screen|Package list|Cache cleaning process|
|-----------------|-------------------|-------------------|
|![Main Screen](fastlane/metadata/android/en-US/images/phoneScreenshots/main_screen.png?raw=true "Main Screen")|![Package list](fastlane/metadata/android/en-US/images/phoneScreenshots/package_list.png?raw=true "Package list")|![Cache cleaning process](fastlane/metadata/android/en-US/images/phoneScreenshots/cache_cleaning_process.png?raw=true "Cache cleaning process")|

## How to use

1. Click clean cache of **User Apps**, **System Apps** OR **All Apps**
2. Enable Usage Stats support for **Cache Cleaner** app if it is required
3. Enable Accessibility support for **Cache Cleaner** app if it is required
4. Check required apps in list (use floating button to check/uncheck all apps)
5. Press **Clean Cache** floating button to run cache clean process
6. Press **Accessibility overlay button** to interrupt process OR wait until process will be finished
7. _(optional)_ Press **Stop Accessibility service** to disable Accessibility service

## Customized Settings UI

Many companies or Android ROMs add or change default Android UI, including Settings. This can cause an issue because the **Cache Cleaner** app is looking for specific text and if it doesn't match, the app does nothing. Please follow the steps below to resolve this issue:

1. Select any app and go to "App Info"
2. Write down text for "Storage" menu
3. Go to "Storage" menu
4. Write down text for "Clear Cache" button
5. Open **Cache Cleaner** app with specific locale (change on required locale in Settings and go back into **Cache Cleaner** app)
6. Open **Settings** menu in the top right corner and find section **Add Extra Search Text**
7. Tap on **Clear Cache**, enter the custom search text for "Clear Cache" button" in input field and press "OK" to save it. To remove custom search text for "Clear Cache" button" just erase a whole text in input field and press "OK"
8. Tap on **Storage**, enter the custom search text for "Storage" menu" in input field and press "OK" to save it. To remove custom search text for "Storage" menu" just erase a whole text in input field and press "OK"

## Icon copyright

**Broom** [icon](https://www.flaticon.com/free-icon/broom_2954888) created by [Smashicons](https://www.flaticon.com/authors/smashicons) - [Flaticon](https://www.flaticon.com/)
