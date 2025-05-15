# Tulsi Photos

Tulsi is a fork of [LavenderPhotos](https://github.com/KaustubhPatange/LavenderPhotos), a smooth and performant gallery app for Android. This fork was created to implement custom UI enhancements and additional features that better suit my personal preferences.

## About This Fork

I created Tulsi because I wanted to customize certain aspects of the UI and add features that weren't available in the original LavenderPhotos app. While maintaining the core functionality and performance of the original, Tulsi includes:

- **Customized UI**: Enhanced floating bottom app bar with rounded corners, proper elevation, and animations
- **Google Lens Integration**: Seamless integration with Google Lens for visual search
- **Improved Layout**: Adjusted spacing and margins for a more polished look

All credit for the original codebase goes to the LavenderPhotos project. Tulsi builds upon that foundation with these customizations.

## Features

### Original Features from LavenderPhotos
- Browse all your photos and videos smoothly, separated by date
- Add and remove albums as you wish, no arbitrary or forced selections
- Search for an image by its name or date (in many formats!)
- Trash Bin that's sorted by recently trashed
- Full-fledged favoriting system
- A selection system that doesn't suck
- Edit and personalize any photo, any time, without an internet connection (even works in landscape mode!)
- Secure sensitive photos in an encrypted medium, for safe keeping
- Find all the relevant information for a photo from one button click
- Copy and Move photos to albums easily
- Clean UI and smooth UX
- Privacy-focused design, no chance of anything happening without your permission

### Tulsi-Specific Enhancements
- **Google Lens Integration**: Search for information about your photos with Google Lens directly from the app
- **Floating Bottom App Bar**: Customized bottom app bar with rounded corners (35% corner radius), proper elevation, and smooth animations
- **Adjusted UI Spacing**: Customized horizontal padding/margins in the photo grid view
- **Enhanced Single Photo View**: Transparent background in single photo view with adjusted width (0.95f) and height (76.dp)

## Google Lens Integration

Tulsi features a robust Google Lens integration that allows you to search for information about your photos with a single tap. The implementation uses a multi-layered approach with fallbacks to ensure compatibility across different device configurations:

1. Direct integration with Google app using ACTION_SEND intent
2. Integration with Google Photos app
3. Generic chooser dialog for maximum compatibility
4. Direct ACTION_VIEW with Google app
5. Web fallback to lens.google.com

This ensures that the Google Lens feature works reliably across different Android versions and device configurations.

## Screenshots
  Main View                 |  Albums                   |
:--------------------------:|:-------------------------:|
![](/assets/images/main.png)|![](/assets/images/albums.png)

  Secure Folder            |  Favourites & Trash       |  Search                  |
:-------------------------:|:-------------------------:|:-------------------------:
  ![](/assets/images/locked.png)|![](/assets/images/favtrash.png)|![](/assets/images/search.png)
