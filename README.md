## CompanyCam Camera

React Native camera component for CompanyCam

***
## Published versions: [companycam-camera](https://www.npmjs.com/package/companycam-camera)

### v40 - May 5, 2017:
- https://github.com/CompanyCam/companycam-camera/commit/17c33c72ab17f254bfa6e4d3976a6c175e0982e6
- Used in store version (4.1.0/6.1.0) of the app.  No iOS camera functionality.
- Still has apptimize dependency; to build on recent companycam branches, may need
`maven { url "http://maven.apptimize.com/artifactory/repo" }`
in the repositories section of android/build.gradle.

### v44 - Aug 22, 2017:
- https://github.com/CompanyCam/companycam-camera/commit/c2633c650707e924c762c1b5787fc8ed423b8afd
- Basic iOS camera functionality. Apptimize dependency removed.

### v46 - Sep 12, 2017 (?):
- Commit ??
- Add OpenCV scanner functionality to iOS camera.

### v47 - Oct 2, 2017:
- https://github.com/CompanyCam/companycam-camera/commit/d0dd6b411a41404cf62c44661c3e8cb88bb37541
- Cloned version 40, and removed Apptimize
- Currently on branch `v40minusApptimize`

### v48 - Oct 16, 2017:
- https://github.com/CompanyCam/companycam-camera/commit/771049881b1a8e8cb57745dd5afb8fcfd9e0bbd4
- Ensure consistent prop names onPhotoTaken and onPhotoAccepted

### v49 - Oct 23, 2017:
- https://github.com/CompanyCam/companycam-camera/commit/78c346af4ad4c7ea3795583be43ddc1a4345988b
- First release candidate for iOS camera and OpenCV scanner on Android+iOS
- Fix some rotation and cropping issues
- Various improvements to scanner, and fix Android scanner rotation
- Fixes concerning observers and button timeouts

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

TODO: Finish this. Add NDK via Android SDK manager.  Install OpenCV Android SDK.

### Scanner functionality

The scanner's functionality is in the repo `companycam-docscan`.  At the moment, the correct workflow to modify the scanner is to clone that repo, modify the files there, and copy the files in `lib/src` to both the Android and iOS native folders of `companycam-camera`.  In other words, don't modify the docscan files in `companycam-camera`, because they are duplicated for Android and iOS and will get out of sync that way.

