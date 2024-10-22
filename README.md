# Recording Studio
An Android sound recorder app. External effect plugin support is planned for the Full version.

---
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" width="200px">](https://f-droid.org/en/packages/io.github.leonidius20.recorder.lite/)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid2.png" width="200px">](https://apt.izzysoft.de/fdroid/index/apk/io.github.leonidius20.recorder.lite)


---

## Screenshots
![Screenshots collage](/docs/merged.png)

## Features
* Audio recording & playback, renaming and deleting recordings;
* Support for various container formats and codecs, including uncompressed WAV;
* Adjustable sample rate, bit rate/depth;
* Compatible with scoped storage (no storage access permission needed on newer Android versions);
* Automatically stopping recordings on low battery or storage & pausing on incoming call.

## Technologies
This app implements a foreground & bound <ins>service</ins>, context-registered <ins>broadcast receivers</ins>, and uses [RecyclerView](https://developer.android.com/develop/ui/views/layout/recyclerview) with [DiffUtil](https://developer.android.com/reference/androidx/recyclerview/widget/DiffUtil) and <ins>payloads</ins>, [Data Binding](https://developer.android.com/topic/libraries/data-binding), [MediaStore API](https://developer.android.com/training/data-storage/shared/media#media_store), AndroidX [Preference](https://developer.android.com/develop/ui/views/components/settings) and [Media3](https://developer.android.com/media/media3) libraries, and Android Architecture Components ([Lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle), [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel), [Navigation](https://developer.android.com/jetpack/androidx/releases/navigation)).

## Credits
The app's icon is from [Freepik on Flaticon](https://www.flaticon.com/free-icons/studio).

## License 
This project is licensed under the [GNU General Public License v3](LICENSE).
