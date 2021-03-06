//
//  CCCameraModule.m
//  newcam
//
//  Created by Matt Boyd on 5/18/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "CCCameraModule.h"

@implementation CCCameraModule

@synthesize isActive;

RCT_EXPORT_MODULE(CCCameraModule);

#pragma mark RCTBridgeModule protocol methods

-(NSArray<NSString *> *)supportedEvents {
    return @[@"onClose", @"onError", @"onPhotoTaken", @"onPhotoAccepted"];
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

@end
