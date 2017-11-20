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
#import <Photos/Photos.h>
#import "CCCameraDelegate.h"
#import "CCCameraEnums.h"
#import "CCCameraManager.h"
#import "UIImage+CCMHelper.h"
#import "StorageUtility.h"
#import "NSData+CCImageExif.h"
#import "NSMutableDictionary+CCImageMetadata.h"

@interface CCCamera : NSObject <CCCameraDelegate, AVCaptureVideoDataOutputSampleBufferDelegate> {

    // The session queue is used to handle the camera-related functionality that occurs on background threads without blocking the UI
    dispatch_queue_t captureSessionQueue;

    // The mCaptureSession is used to display the camera preview and to capture the photo.
    AVCaptureSession *captureSession;

    // The camera is a reference to the current camera being used
    AVCaptureDevice *camera;

    // The cameraType is a reference to the camera type (rear- or forward-facing) currently being used
    AVCaptureDevicePosition cameraType;

    // The deviceInput is used to capture input from the current camera
    AVCaptureDeviceInput *deviceInput;

    // The photoOutput is used to record the output from the current camera
    AVCaptureStillImageOutput *photoOutput;

    // The photoData is used to store the NSData from the photo output
    NSData *photoData;

    // The videoOutput is used to provide the preview frames for the scanner mode
    AVCaptureVideoDataOutput *videoOutput;

    // The ipDidAllocate flag describes whether or not the image processor has already been initialized for the scanner mode
    BOOL ipDidAllocate;

    // The currentScaleNumber and startingScaleNumber are used to handle pinch/zoom gestures
    double currentScaleNumber;
    double startingScaleNumber;
}

@property (nonatomic) dispatch_queue_t captureSessionQueue;
@property (nonatomic) AVCaptureSession *captureSession;
@property (nonatomic) AVCaptureDevice *camera;
@property (nonatomic) AVCaptureDevicePosition cameraType;
@property (nonatomic) AVCaptureDeviceInput *deviceInput;
@property (nonatomic) AVCaptureStillImageOutput *photoOutput;
@property (nonatomic) NSData *photoData;
@property (nonatomic) AVCaptureVideoDataOutput *videoOutput;
@property (nonatomic) BOOL ipDidAllocate;
@property (nonatomic) double currentScaleNumber;
@property (nonatomic) double startingScaleNumber;

-(void)setupSession;
-(void)configureSession;
-(void)processPhotoData;
-(void)processPhotoData:(UIImage *)latestScanImage;
-(NSString *)getFlashModeString;
-(NSString *)getResolutionModeString;
-(NSString *)getCameraModeString;
//-(void)persistFlashMode:(NSString *)thisFlashMode;
-(void)persistResolutionMode:(NSString *)thisResolutionMode;
-(void)persistCameraMode:(NSString *)thisCameraMode;

-(void)changeCameraMode:(CCCameraMode)mode;

@end
