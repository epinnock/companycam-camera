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
Type: `(file: string, dims: [w: int, h: int], photoOrigin: string) => void`. Invoked when a photo is taken and should be displayed to the user for reviewing or editing.  Valid photoOrigin values are:
- `undefined` or `null`: No origin information; treat as `'STANDARD_CAMERA'`
- `'STANDARD_CAMERA'`: Taken as normal photo without fastcam
- `'STANDARD_CAMERA_FASTCAM'`: Taken as normal photo with fastcam
- `'STANDARD_CAMERA_DOCSCAN'`: Taken with document scanner

#### onPhotoAccepted
Type: `(file: string, dims: [w: int, h: int], photoOrigin: string) => void`. Invoked when a photo is taken and should be immediately uploaded.  Valid photoOrigin values are the same as for `onPhotoTaken`.

#### hideNativeUI
Type: `bool`. Determine whether the native UI should be hidden (default is `false`).

#### cameraTrayData
Array of objects `{ url:string, uploaded:bool, isDocument:bool }`.
- `url`: Image to show; if local, must have a `file://` prefix
- `uploaded`: Has this image uploaded?
- `edited`: Has this image been edited?
- `isDocument`: Should this go in the tray for the document scanner, rather than the photos tray?

#### cameraTrayVisible
Type: `bool`.  Show camera tray?

#### onSelectTrayItem
Type: `(item) => void`.  Invoked when a camera tray item is clicked.  The value of `item` is one of the elements of the `cameraTrayData` array.

#### setCameraTrayVisible
Type: `(bool) => void`.  Set visibility of camera tray.

***
## Development

### OpenCV Setup

#### iOS:

Download the v3.2.0 `iOS pack` from https://opencv.org/releases.html and put `opencv2.framework` into `node_modules/companycam-camera/ios`.

#### Android:

Add NDK via Android SDK manager.  Install OpenCV Android SDK.  In `android/app/CMakeLists.txt`, make sure the line `SET(OpenCV_DIR [...]/OpenCV-android-sdk/sdk/native/jni)` is pointing to the correct path (we should figure out a way to avoid this step).

### Scanner functionality

The scanner's functionality is in the repo `companycam-docscan`.  At the moment, the correct workflow to modify the scanner is to clone that repo, modify the files there, and copy the files in `lib/src` to both the Android and iOS native folders of `companycam-camera`.  In other words, don't modify the docscan files in `companycam-camera`, because they are duplicated for Android and iOS and will get out of sync that way.
