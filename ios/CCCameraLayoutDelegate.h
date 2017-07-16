//
//  CCCameraLayoutDelegate.h
//  newcam
//
//  Created by Matt Boyd on 5/21/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

@class CCCamera;

@protocol CCCameraLayoutDelegate <NSObject>

// The CCCameraLayoutDelegate protocol provides methods that allow an object to access certain UI features of a generic camera layout.

// This method sets the reference to the CCCamera object
@optional
-(void)setCameraObject:(CCCamera *)_camera;

// This method sets the place name label
@optional
-(void)setPlaceNameLabel:(NSString *)name;

// This method sets the visibility of the flash button
@optional
-(void)setFlashButtonVisibility;

// This method sets the flash button image
@optional
-(void)setFlashModeImage:(NSString *)flashMode;

// This method sets the resolution button image
@optional
-(void)setResolutionImage:(NSString *)resolutionMode;

// This method sets the camera button visibility
@optional
-(void)setCameraButtonVisibility;

// This method sets the camera mode layout features
@optional
-(void)setCameraMode:(NSString *)cameraMode;

// This method shows the resolution layout
@optional
-(void)showResolutionLayout;

// This method hides the resolution layout
@optional
-(void)hideResolutionLayout;

// This method shows an auto focus indicator view at the given position while the camera is focusing and/or exposing
@optional
-(void)showAutoFocusIndicator:(CGPoint)touchPoint :(BOOL)setRepeating;

// This method hides the auto focus indicator view
@optional
-(void)hideAutoFocusIndicator;

// This method returns the current orientation of the layout
@optional
-(UIDeviceOrientation)getCurrentOrientation;

@end
