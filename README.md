<img src="/assets/images/Banner.png" alt="banner" style="width=100%">

<b><p align="center">This is Lavender Photos, a no non-sense, stylish, and performant gallery app for Android!</p></b>

<div align="center">
 
  [![GitHub Release](https://img.shields.io/github/v/release/kaii-lb/LavenderPhotos?style=for-the-badge&color=green)](https://github.com/kaii-lb/LavenderPhotos/releases/latest)
  [![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/kaii-lb/LavenderPhotos/total?style=for-the-badge)](https://github.com/kaii-lb/LavenderPhotos/releases)
  [![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/kaii-lb/LavenderPhotos/workflow.yml?branch=main&style=for-the-badge&label=Nightly)](https://github.com/kaii-lb/LavenderPhotos/actions/workflows/workflow.yml)
  [![Static Badge](https://img.shields.io/badge/Koltin-100%25-green?style=for-the-badge)](https://kotlinlang.org/)

  [![Dynamic JSON Badge](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fapt.izzysoft.de%2Ffdroid%2Fapi%2Fv1%2Fpackages%2Fcom.kaii.photos&query=%24.packages%5B0%5D.versionName&style=for-the-badge&label=IzzyOnDroid&color=blue)](https://apt.izzysoft.de/fdroid/index/apk/com.kaii.photos)
</div>

# Features
- **Smooth Browsing:** Browse all your photos and videos smoothly, separated by date
- **Flexible Albums:** Add and remove albums as you wish, no arbitrary or forced selections
- **Smart Search:** Search for an image by its name, date or tag!
- **Cloud Backup:** Immich integration for safe and easy cloud media backup
- **Media Tagging:** Tagging system to properly organize media
- **Trash Bin:** Trash Bin that's sorted by recently trashed, so you don't lose track of what went where
- **Favourites System:** Full fledged favouriting system, compatible with Immich and other apps
- **Album Groups:** Create groups of albums for easy organization of your memories
- **Offline Editing:** Edit and personalize any photo or video, at any time, without an internet connection
- **Secure Folder:** Secure sensitive photos with bulletproof AES-256 encryption
- **Quick Info:** Find all the relevant information for a photo from one button click
- **Easy Management:** Copy and Move photos to albums easily, across storage mediums or even the cloud!
- **Modern Interface:** Clean UI, smooth UX and a playful interface
- **Privacy First:** No data is collected, no AI is trained, and no actions without permission
- **Heavily Customizable:** Change colors, behaviours, styles, and more
- **Future:** WAY more to come

# Help us translate
With over 24 languages supported, Lavender Photos is accessible for all, and contributing a translation would greatly help with that!
Our [Weblate](https://hosted.weblate.org/projects/lavender-photos/) instance is a great place to start.

# Screenshots
|             Main View             |                Albums                | Cloud                                 |
|:---------------------------------:|:------------------------------------:|:--------------------------------------|
|   ![](/assets/images/Main.png)    |    ![](/assets/images/Albums.png)    | ![](/assets/images/Cloud.png)         |
|           Secure Folder           |              Favourites              | Search                                |
|  ![](/assets/images/Secure.png)   |  ![](/assets/images/Favourites.png)  | ![](/assets/images/Search.png)        |
|              Privacy              |                Trash                 | Look & Feel                           |
|  ![](/assets/images/Privacy.png)  |    ![](/assets/images/Trash.png)     | ![](/assets/images/LookAndFeel.png)   |

# Verification
- Package name: com.kaii.photos
- Signing hash: B2:6E:8A:CD:20:D2:BD:B5:1D:EE:0D:F9:65:AA:40:BD:86:43:D3:F8:95:E8:25:A0:CD:DF:51:FE:27:5B:3E:C1

# Install
- Grab a release version from the [releases page](https://github.com/kaii-lb/LavenderPhotos/releases)
- Or a nightly version from the [actions page](https://github.com/kaii-lb/LavenderPhotos/actions)
- Or use the [IzzyOnDroid build](https://apt.izzysoft.de/fdroid/index/apk/com.kaii.photos) with an F-Droid client

# Building
- Clone the repo: `git clone https://github.com/kaii-lb/LavenderPhotos.git && cd LavenderPhotos`
- Run `mkdir keys` and copy your signing keys as `releasekey.pk8` and `releasekey.x509.pem` into the newly created keys folder
- Run `chmod +x ./build.sh`
- Run `./build.sh tag` where `tag` is one of `release` or `debug`
- Install the resulting `outputs/photos_signed_${tag}_${abi}.apk` (abi is one of `arm64-v8a` or `armeabi-v7a`)
- Note: `build.sh` accepts a `--install` arguement which immediately installs the APK after it finished compiling
- Note: for unsigned builds just use normal grade commands. ie: `./gradlew tag` where `tag` is one of `assembleRelease` or `assembleDebug`
  - Resulting unsigned APK will be in `./app/build/outputs/apk/{debug/release}`
