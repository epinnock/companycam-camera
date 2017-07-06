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
    return @[@"onClose", @"onError", @"photoTaken"];
}

#pragma mark React methods

RCT_EXPORT_METHOD(setActive) {
    isActive = YES;
}

RCT_EXPORT_METHOD(setInactive) {
    isActive = NO;
}

@end
