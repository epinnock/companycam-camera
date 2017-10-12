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

#### photoTaken
Type: `(file: string, dims: [w: int, h: int]) => void`. Invoked when a photo is taken in ordinary (i.e., non-fastcam) mode.

#### photoAccepted
Type: `(file: string, dims: [w: int, h: int]) => void`. Invoked when a photo is taken in fastcam mode.

#### auxModeCaption
Type: `string`.  Caption for the 'auxiliary' button at the bottom (e.g. 'AFTER CAM').

#### onAuxModeClicked
Type: `() => void`. Invoked when the 'auxiliary' button at the bottom is pressed (e.g. launch after cam).
