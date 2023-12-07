# Cache Cleaner

## Description

Since the release of Android 6 (Marshmallow), the option to clear the cache of all apps at once has been restricted by Google to system apps only. This means that users no longer have the convenience of clearing the cache of all their apps simultaneously. To clear the cache of individual apps, one must manually navigate to each app's "App Info" screen, locate the "Storage" menu, and then select the "Clean cache" button.

To simplify this process and save time, **Cache Cleaner** requests a list of installed apps on your device and utilizes the Accessibility service to replace the manual steps involved in clearing the cache. This allows for a more convenient and automated process of cache cleaning.

**Cache Cleaner** provides the added benefit of customizable app lists. This functionality allows you to create personalized lists of specific apps, giving you the flexibility to selectively clear the cache of only the apps you choose.

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.github.bmx666.appcachecleaner.googleplay)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.github.bmx666.appcachecleaner/)

or get the APK from the [Releases Section](https://github.com/bmx666/android-appcachecleaner/releases/latest).

## FAQ

* Clear cache process unexpectedly stops -> please read the issue [#8](https://github.com/bmx666/android-appcachecleaner/issues/8)
* **Clean Cache** does not anything -> please read the section [Customized Settings UI](#customized-settings-ui)

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

Many companies or Android ROMs add or change default Android UI, including Settings. This can cause an issue because the **Cache Cleaner** app is looking for specific text and if it doesn't match, the app does nothing.

Please follow the steps below to resolve clear cache issue:

1. Select any app and go to "App Info"
2. Write down text for "Storage" menu
3. Go to "Storage" menu
4. Write down text for "Clear Cache" button
5. Open **Cache Cleaner** app with specific locale (change on required locale in Settings and go back into **Cache Cleaner** app)
6. Open **Settings** menu in the top right corner and find section **Add Extra Search Text**
7. Tap on **Clear Cache**, enter the custom search text for "Clear Cache" button" in input field and press "OK" to save it. To remove custom search text for "Clear Cache" button" just erase a whole text in input field and press "OK"
8. Tap on **Storage**, enter the custom search text for "Storage" menu" in input field and press "OK" to save it. To remove custom search text for "Storage" menu" just erase a whole text in input field and press "OK"

#### Xiaomi smartphones with specific regional MIUI firmware must use the different clear cache scenario:

1. Open **Settings** menu in the top right corner and find section **Other**
2. Click **Clear cache scenario** and select **Xiaomi MIUI**

#### ATTENTION ⚠️: Huawei and Xiaomi smartphones often use aggressive battery optimization that can disrupt communication between **Cache Cleaner** and the Accessibility service, preventing them from working as expected. Please follow the steps below:

1. Open **Settings** menu in the top right corner and find section **Show extra buttons**
2. Enable option **Start/Stop Accessibility service**
3. The next time you start **Cache Cleaner**, if the Accessibility service is running, stop it by clicking **Stop Accessibility service** button. Then, enable it again by clicking **Start Accessibility service** button

## Icon copyright

**Broom** [icon](https://www.flaticon.com/free-icon/broom_2954888) created by [Smashicons](https://www.flaticon.com/authors/smashicons) - [Flaticon](https://www.flaticon.com/)

* [Freepik license for Broom icon](LICENSE_Freepik_icon.pdf)
* [Freepik license for monochrome Broom icon](LICENSE_Freepik_icon_monochrome.pdf)

## Support my project

[<img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;">](https://www.buymeacoffee.com/bmx666)

[<img src="https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_111x69.jpg" alt="PayPal" style="height: 60px !important;">](https://paypal.me/bmx666dev)

[<img src="https://codebard.com/ext_images/patreon-medium-button.png" alt="Patreon" style="height: 60px !important;">](https://www.patreon.com/bmx666)

