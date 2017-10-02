## CompanyCam camera component

TODO: Add instructions/props here.

## Versions published to npm: [companycam-camera](https://www.npmjs.com/package/companycam-camera)

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
