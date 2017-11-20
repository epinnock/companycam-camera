//
//  CCCameraDelegate.h
//  newcam
//
//  Created by Matt Boyd on 5/30/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import "CCCameraEnums.h"

@protocol CCCameraDelegate <NSObject>

// The CCCameraDelegate protocol provides methods to operate a camera and query it about its properties.

// This method starts the camera
@optional
-(void)startCamera;

// This method releases the camera
@optional
-(void)releaseCamera;

// This method sets the camera resolution based on the given string
@optional
-(void)setResolution:(NSString *)resolutionMode;

// This method returns a boolean that describes whether or not the device has an available front-facing camera
@optional
-(BOOL)hasFrontCamera;

// This method returns a boolean that describes whether or not the device has an available rear-facing camera
@optional
-(BOOL)hasRearCamera;

// This method toggles the camera between forward-facing and rear-facing
@optional
-(void)toggleCamera;

// This method returns a boolean that describes whether or not the current camera has flash capability
@optional
-(BOOL)hasFlash;

// This method toggles the flash state
@optional
-(void)toggleFlash;

// This method captures a photo from the camera
@optional
-(void)takePicture:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject;

// This method handles a screen touch event
@optional
-(void)handleTouchEvent:(UIEvent *)event;

// This method handles a pinch/zoom event
@optional
-(void)handleZoom:(double)zoomScale :(BOOL)zoomEnded;

@end
