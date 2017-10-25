## CompanyCam Camera

React Native camera component for CompanyCam

***
## Published versions: [companycam-camera](https://www.npmjs.com/package/companycam-camera)

See https://github.com/CompanyCam/companycam-camera/releases

***
## Component props

#### storagePath
Type: `string`. Full path (e.g. "/var/mobile/Containers/Data/Application/[UUID]/Documents") that the component should use when writing photos.

#### projectName
Type: `string`. For displaying only.

#### projectAddress
Type: `string`. For displaying only.

#### exifLat
Type: `number`. Latitude to be written to EXIF data.

#### exifLon
Type: `number`. Longitude to be written to EXIF data.

#### exifLocTimestamp
Type: `number`. Unix timestamp (in seconds) for location, to be written to EXIF data.

#### onClose
Type: `(errmsg: string, button: string) => void`. Close the camera (i.e., stop rendering the component).  The `button` argument indicates why the close request was made:
- `button === 'error'`: An error occurred; description is given by `errmsg`
- `button === 'label'`: The address label was pressed
- `button === 'close'`: The close button was pressed
- `button === 'capture'`: A photo was taken, and needs to be confirmed/edited

#### onPhotoTaken
Type: `(file: string, dims: [w: int, h: int]) => void`. Invoked when a photo is taken in ordinary (i.e., non-fastcam) mode.

#### onPhotoAccepted
Type: `(file: string, dims: [w: int, h: int]) => void`. Invoked when a photo is taken in fastcam mode.

#### auxModeCaption
Type: `string`.  Caption for the 'auxiliary' button at the bottom (e.g. 'AFTER CAM').

#### onAuxModeClicked
Type: `() => void`. Invoked when the 'auxiliary' button at the bottom is pressed (e.g. launch after cam).

#### hideNativeUI
Type: `bool`. Determine whether the native UI should be hidden (default is `false`).

***
## Development

### OpenCV Setup

#### iOS:

Download the v3.2.0 `iOS pack` from https://opencv.org/releases.html and put `opencv2.framework` into `node_modules/companycam-camera/ios`.

#### Android:

Add NDK via Android SDK manager.  Install OpenCV Android SDK.  In `android/app/CMakeLists.txt`, make sure the line `SET(OpenCV_DIR [...]/OpenCV-android-sdk/sdk/native/jni)` is pointing to the correct path (we should figure out a way to avoid this step).

### Scanner functionality

The scanner's functionality is in the repo `companycam-docscan`.  At the moment, the correct workflow to modify the scanner is to clone that repo, modify the files there, and copy the files in `lib/src` to both the Android and iOS native folders of `companycam-camera`.  In other words, don't modify the docscan files in `companycam-camera`, because they are duplicated for Android and iOS and will get out of sync that way.

