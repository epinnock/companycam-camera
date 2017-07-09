//
//  CCCamera.h
//  newcam
//
//  Created by Matt Boyd on 5/10/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import "CCCameraDelegate.h"
#import "CCCameraManager.h"
#import "UIImage+CCHelper.h"
#import "StorageUtility.h"

@interface CCCamera : NSObject <CCCameraDelegate, AVCapturePhotoCaptureDelegate> {
    
    // The session queue is used to handle the camera-related functionality that occurs on background threads without blocking the UI
    dispatch_queue_t captureSessionQueue;
    
    // The mCaptureSession is used to display the camera preview and to capture the photo.
    AVCaptureSession *captureSession;
    
    // The camera is a reference to the current camera being used
    AVCaptureDevice *camera;
    
    // The deviceInput is used to capture input from the current camera
    AVCaptureDeviceInput *deviceInput;
    
    // The photoOutput is used to record the output from the current camera
    AVCapturePhotoOutput *photoOutput;
    
    // The photoData is used to store the NSData from the photo output
    NSData *photoData;
    
}

@property (nonatomic) dispatch_queue_t captureSessionQueue;
@property (nonatomic) AVCaptureSession *captureSession;
@property (nonatomic) AVCaptureDevice *camera;
@property (nonatomic) AVCaptureDeviceInput *deviceInput;
@property (nonatomic) AVCapturePhotoOutput *photoOutput;
@property (nonatomic) NSData *photoData;

-(void)setupSession;
-(void)configureSession;
-(NSString *)getFlashModeString;
-(NSString *)getResolutionModeString;
-(NSString *)getCameraModeString;
-(void)persistFlashMode:(NSString *)thisFlashMode;
-(void)persistResolutionMode:(NSString *)thisResolutionMode;
-(void)persistCameraMode:(NSString *)thisCameraMode;

@end
