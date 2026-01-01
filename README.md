# Lavender Photos!
This is Lavender Photos, a no non-sense, smooth, and performant gallery app for Android!

<img src="/assets/images/Banner.png" alt="banner" style="width=100%">

# Features
- Browse all your photos and videos smoothly, separated by date
- Add and remove albums as you wish, no arbitrary or forced selections
- Search for an image by its name or date (in many formats!)
- Immich integration for safe and easy cloud media backup
- Trash Bin that's sorted by recently trashed
- Full fledged favouriting system
- A selection system that doesn't suck
- Edit and personalize any photo or video, any time, without an internet connection
- Secure sensitive photos in an encrypted medium, for safe keeping
- Find all the relevant information for a photo from one button click
- Copy and Move photos to albums easily
- Clean UI and smooth UX
- Privacy focused design, no chance of anything happening without your permission
- Customizable to your heart's content
- WAY more to come

# Help us translate
Lavender Photos is meant to be accessible for all, and contributing a translation would greatly help with that!
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

# Compile
- Clone this repo & cd into it
- Run `mkdir keys` and copy your signing keys as `releasekey.pk8` and `releasekey.x509.pem` into the newly created keys folder
- Run `chmod +x ./build.sh`
- Run `./build.sh TYPE` where `TYPE` is one of `release` or `debug`
- Install the resulting `photos_signed_TYPE.apk`
- Note: `build.sh` accepts normal gradle arguments after the first `TYPE` argument
- Note: for unsigned builds just use normal grade commands. ie: `./gradlew TYPE` where `TYPE` is one of `assembleRelease` or `assembleDebug`
  - Resulting unsigned APK will be in `./app/build/outputs/apk/{debug/release}`
