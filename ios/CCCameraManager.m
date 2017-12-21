//
//  CCCameraManager.m
//  newcam
//
//  Created by Matt Boyd on 5/10/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "CCCameraManager.h"

@implementation CCCameraManager

static CCCameraView *latestView;

RCT_EXPORT_MODULE(CompanyCamCamera)

-(UIView *)view {

    if (latestView == nil) {
        latestView = [[CCCameraView alloc] initWithManager:self bridge:self.bridge];
    }
    else {
        [latestView setupView];
    }

    // Create the CCCamera object
    latestView.camera = [[CCCamera alloc] init];

    // Set the layout object's reference to the camera
    [latestView.cameraLayout setCameraObject:latestView.camera];

    return latestView;
}

+(CCCameraView *)getLatestView {
    return latestView;
}

#pragma mark React props

RCT_CUSTOM_VIEW_PROPERTY(storagePath, NSString, CCCameraView) {
    NSString *storagePath = [RCTConvert NSString:json];
    [latestView setStoragePath:storagePath];
}

RCT_CUSTOM_VIEW_PROPERTY(projectName, NSString, CCCameraView) {
    NSString *projectName = [RCTConvert NSString:json];
    [latestView setProjectName:projectName];
}

RCT_CUSTOM_VIEW_PROPERTY(projectAddress, NSString, CCCameraView) {
    NSString *projectAddress = [RCTConvert NSString:json];
    [latestView setProjectAddress:projectAddress];
}

RCT_CUSTOM_VIEW_PROPERTY(exifLat, double, CCCameraView) {
    double exifLat = [RCTConvert double:json];
    [latestView setExifLat:exifLat];
}

RCT_CUSTOM_VIEW_PROPERTY(exifLon, double, CCCameraView) {
    double exifLon = [RCTConvert double:json];
    [latestView setExifLon:exifLon];
}

RCT_CUSTOM_VIEW_PROPERTY(exifLocTimestamp, double, CCCameraView) {
    double exifLocTimestamp = [RCTConvert double:json];
    [latestView setExifLocTimestamp:exifLocTimestamp];
}

RCT_CUSTOM_VIEW_PROPERTY(hideNativeUI, BOOL, CCCameraView) {
    BOOL hideNativeUI = [RCTConvert BOOL:json];
    [latestView setHideNativeUI:hideNativeUI];
}

RCT_EXPORT_VIEW_PROPERTY(onClose, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPhotoTaken, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPhotoAccepted, RCTDirectEventBlock)

RCT_CUSTOM_VIEW_PROPERTY(flashMode, NSInteger, CCCameraFlashMode) {
    CCCameraFlashMode mode = [RCTConvert NSInteger:json];
    [self setFlashMode:mode];
}

- (void)setFlashMode:(CCCameraFlashMode)flashMode {
    AVCaptureDevice *device = [[latestView.camera deviceInput] device];
    NSError *error = nil;

    AVCaptureTorchMode torchMode;

    // Limit to torch on/off
    switch (flashMode) {
        case CCCameraFlashModeAuto:
            torchMode = AVCaptureTorchModeOff;
            break;
        case CCCameraFlashModeOn:
            torchMode = AVCaptureTorchModeOff;
            break;
        case CCCameraFlashModeOff:
            torchMode = AVCaptureTorchModeOff;
            break;
        case CCCameraFlashModeTorch:
            torchMode = AVCaptureTorchModeOn;
            break;
        default:
            torchMode = AVCaptureTorchModeOff;
            break;
    }

    if (![device lockForConfiguration:&error]) {
        NSLog(@"%@", error);
        return;
    }
    if (device.hasTorch && [device isTorchModeSupported:torchMode])
    {
        NSError *error = nil;
        if ([device lockForConfiguration:&error])
        {
            [device setTorchMode:torchMode];
            [device unlockForConfiguration];
        }
        else
        {
            NSLog(@"%@", error);
        }
    }
    [device unlockForConfiguration];
}

RCT_CUSTOM_VIEW_PROPERTY(cameraMode, NSInteger, CCCameraMode) {
    CCCameraMode mode = [RCTConvert NSInteger:json];
    [latestView.camera changeCameraMode:mode];
}

RCT_CUSTOM_VIEW_PROPERTY(resolutionMode, NSInteger, CCCameraResolutionMode) {
    CCCameraResolutionMode mode = [RCTConvert NSInteger:json];
    [latestView.camera changeResolution:mode];
}


@end
