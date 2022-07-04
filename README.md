# Android Cache Cleaner

## Description

Since Android 6 (Marshmallow) is not possible anymore to clear cache of all apps at same time and Google has moved this permission to system apps only. There is only one possible way left - open manually info about application and find specific "Storage" menu and then press "Clean cache" button.

**AndroidCacheCleaner** can request all installed user and system apps and it replaces all manual actions related to clean cache using Accessibility service.

**AndroidCacheCleaner** saves the last checked apps to show them next time only.

## screenshots

|Android 11 and early|Android 12 and later|
|-----------------|-------------------|
|accessibility button in navbar|floating accessibility button|
|![Android 11 and early](fastlane/metadata/android/en-US/images/phoneScreenshots/android_11.png?raw=true "Android 11 and early")|![Android 12 and later](fastlane/metadata/android/en-US/images/phoneScreenshots/android_12.png?raw=true "Android 12 and later")|

## How to use

1. Run app
2. Open **Accessibility Settings**
3. Enable Accessibility support for **AppCacheCleaner**
4. Return back to AppCacheCleaner app
5. Open **Usage Stats Settings**
6. Enable Usage Stats support for **AppCacheCleaner**
7. Return back to AppCacheCleaner app
8. Click **User Apps Cache**, **System Apps Cache** OR **All Apps Cache**
9. Check required apps (use floating button to check/uncheck all apps)
10. Press **Clean Cache** floating button
11. Press **Accessibility button** to interrupt process OR wait until process will be finished
12. Press **Close App** OR clean app from task manager to disable Accessibility

## Customized Settings UI

Many companies are supplied Customized Android UI, including Settings. It can cause issue becuase app is looking specific text and if doesn't match then app doens't do anything.

1. Select any app and go to "App Info"
2. Write down text for "Storage" menu
3. Go to "Storage" menu
4. Write down text for "Clear Cache" button
5. Open app with specific locale (change on required locale in Settings and go back into app)
6. Click **Add Extra Search Text**
7. Type user custom search text for "Storage" menu" and press "OK", or "REMOVE" otherwise
8. Type user custom search text for "Clear cache" button" and press "OK", or "REMOVE" otherwise

## Icon copyright

Icon made by [Smashicons](https://www.flaticon.com/authors/smashicons) from [Flaticon](https://www.flaticon.com/)
