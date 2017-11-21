//
//  CCCameraModuleIOS.m
//  newcam
//
//  Created by Matt Boyd on 5/18/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "CCCameraModuleIOS.h"
#import "CCCameraEnums.h"

@implementation CCCameraModuleIOS

@synthesize isActive;

RCT_EXPORT_MODULE(CCCameraModuleIOS);

#pragma mark RCTBridgeModule protocol methods

-(NSArray<NSString *> *)supportedEvents {
    return @[@"onClose", @"onError", @"onPhotoTaken", @"onPhotoAccepted"];
}

- (NSDictionary *)constantsToExport
{
    return @{
             @"FlashMode": @{
                     @"off": @(CCCameraFlashModeOff),
                     @"on": @(CCCameraFlashModeOn),
                     @"auto": @(CCCameraFlashModeAuto),
                     @"torch": @(CCCameraFlashModeTorch),
                   },
             @"CameraMode": @{
                     @"fastcam": @(CCCameraModeFastCam),
                     @"photo": @(CCCameraModeCamera),
                     @"scanner": @(CCCameraModeScanner),
                   },
             };
}


#pragma mark React methods

RCT_EXPORT_METHOD(setActive) {
    isActive = YES;

    // Post a notification to let any interested objects know that the CCCameraModule is active
    [[NSNotificationCenter defaultCenter] postNotificationName:@"CCCameraModuleActiveNotification"
                                                        object:self
                                                      userInfo:nil];
}

RCT_EXPORT_METHOD(setInactive) {
    isActive = NO;

    // Post a notification to let any interested objects know that the CCCameraModule is inactive
    [[NSNotificationCenter defaultCenter] postNotificationName:@"CCCameraModuleInactiveNotification"
                                                        object:self
                                                      userInfo:nil];
}

RCT_EXPORT_METHOD(releaseCamera) {
    isActive = NO;

    // Post a notification to let any interested objects know that the CCCameraModule is inactive
    [[NSNotificationCenter defaultCenter] postNotificationName:@"CCCameraModuleReleaseNotification"
                                                        object:self
                                                      userInfo:nil];
}

RCT_EXPORT_METHOD(capture) {
    // Post a notification to let any interested objects know that capture is called
    [[NSNotificationCenter defaultCenter] postNotificationName:@"CCCameraModuleCaptureNotification"
                                                        object:self
                                                      userInfo:nil];
}

@end
