//
//  CCCameraEnums.h
//  CCCamera
//
//  Created by Jason Gaare on 11/9/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

typedef NS_ENUM( NSInteger, CCCameraSetupResult ) {
  CCCameraSetupResultSuccess,
  CCCameraSetupResultCameraNotAuthorized,
  CCCameraSetupResultSessionConfigurationFailed
};

typedef NS_ENUM( NSInteger, CCCameraFlashMode ) {
  CCCameraFlashModeAuto,
  CCCameraFlashModeOn,
  CCCameraFlashModeOff,
  CCCameraFlashModeTorch
};

typedef NS_ENUM( NSInteger, CCCameraResolutionMode ) {
  CCCameraResolutionModeNormal,
  CCCameraResolutionModeHigh,
  CCCameraResolutionModeSuper
};

typedef NS_ENUM( NSInteger, CCCameraMode ) {
  CCCameraModeFastCam,
  CCCameraModeCamera,
  CCCameraModeScanner
};

typedef NS_ENUM( NSInteger, AVCamSetupResult ) {
  AVCamSetupResultSuccess,
  AVCamSetupResultCameraNotAuthorized,
  AVCamSetupResultSessionConfigurationFailed
};

typedef NS_ENUM( NSInteger, CCCameraOrientation ) {
  CCCameraOrientationPortrait,
  CCCameraOrientationLandscapeLeft,
  CCCameraOrientationLandscapeRight,
  CCCameraOrientationPortraitUpsideDown,
};
