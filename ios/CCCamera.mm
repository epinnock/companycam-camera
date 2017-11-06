//
//  CCCamera.m
//  newcam
//
//  Created by Matt Boyd on 5/10/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Much of the camera-related content of this class is taken from the Apple example project 'AVCam Objective-C'
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#import "CCCamera.h"
//#import <opencv2/core/core.hpp>
#import <opencv2/videoio/cap_ios.h>

// These strings are used to save and retrieve persistent settings to the NSUserDefaults
#define PREFS_FLASH_MODE @"PREFS_FLASH_MODE"
#define PREFS_RESOLUTION_MODE @"PREFS_RESOLUTION_MODE"
#define PREFS_CAMERA_MODE @"PREFS_CAMERA_MODE"

// Define some constants for pinch & zoom
#define MAX_PINCH_SCALE_NUM   3.f
#define MIN_PINCH_SCALE_NUM   1.f

// Define a constant for the maximum image processing size for the scanner mode
#define MAX_DIM_PROCESSING_OUTPUT 1024

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

@interface CCCamera() <CvVideoCameraDelegate>

// The flashMode string defines the flash mode to use
// "auto" = auto flash
// "on" = flash on
// "off" = flash off
// "torch" = flash continuously on
@property (nonatomic) CCCameraFlashMode flashMode;

// The resolutionMode string describes the image resolution to use
// "normal" = lowest resolution
// "high" = high resolution
// "super" = highest resolution
@property (nonatomic) CCCameraResolutionMode resolutionMode;

// The cameraMode string describes the type of camera setting to use
// "fastcam" = FastCam mode means that the user won't be given the option to edit photos after capturing them
// "camera" = this is the default camera mode that allows the user to edit photos after capturing them
// "scanner" = this is a mode that tries to identify documents in the photo and transforms the image to show a flattened version of the document
@property (nonatomic) CCCameraMode cameraMode;

// The setupResult describes the state of the camera setup and whether or not it was successful
@property (nonatomic) CCCameraSetupResult setupResult;

@property (nonatomic) CvVideoCamera *videoCamera;

@end

@implementation CCCamera

@synthesize flashMode;
@synthesize resolutionMode;
@synthesize cameraMode;
@synthesize setupResult;
@synthesize captureSessionQueue;
@synthesize captureSession;
@synthesize camera;
@synthesize cameraType;
@synthesize deviceInput;
@synthesize photoOutput;
@synthesize photoData;
@synthesize videoOutput;
@synthesize ipDidAllocate;
@synthesize currentScaleNumber;
@synthesize startingScaleNumber;

-(id)init {

    self = [super init];
    if (self) {

        // Register to receive a notification when the CCCameraModule is made inactive
        [[NSNotificationCenter defaultCenter] removeObserver:self];
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onSetInactive:)
                                                     name:@"CCCameraModuleInactiveNotification"
                                                   object:nil];

        // Register to receive notifications when the app is sent to the background or enters the foreground
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onSetActive:) name:UIApplicationWillEnterForegroundNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onSetInactive:) name:UIApplicationDidEnterBackgroundNotification object:nil];

        // Set the default camera type to be back-facing
        self.cameraType = AVCaptureDevicePositionBack;

        // Uncomment this section if you'd like to add listeners for the completion of the auto focus or auto exposure routines
        ///////////////////////////////////////////
        //// Add observers for the focus and exposure
        //[self.camera addObserver:self forKeyPath:@"adjustingFocus" options:0 context:nil];
        //[self.camera addObserver:self forKeyPath:@"adjustingExposure" options:0 context:nil];
        ///////////////////////////////////////////

        // Get the saved settings from the NSUserDefaults.  Restrict the possible flash modes to "torch" and "off".  Restrict the possible camera modes to "fastcam" and "camera".
        self.flashMode = (CCCameraFlashMode)[[[NSUserDefaults standardUserDefaults] objectForKey:PREFS_FLASH_MODE] intValue];
        if (!(self.flashMode == CCCameraFlashModeOff || self.flashMode == CCCameraFlashModeTorch)) {
            self.flashMode = CCCameraFlashModeOff;
        }
        self.resolutionMode = (CCCameraResolutionMode)[[[NSUserDefaults standardUserDefaults] objectForKey:PREFS_RESOLUTION_MODE] intValue];
        self.cameraMode = (CCCameraMode)[[[NSUserDefaults standardUserDefaults] objectForKey:PREFS_CAMERA_MODE] intValue];
        if (!(self.cameraMode == CCCameraModeFastCam || self.cameraMode == CCCameraModeCamera)) {
            self.cameraMode = CCCameraModeCamera;
        }

        // Setup the capture session
        [self setupSession];

        // Start the default camera
        [self startCamera];
    }

    return self;
}

// This method responds to the UIApplicationWillEnterForegroundNotification
-(void)onSetActive:(NSNotification *)notification {

    // Setup the capture session
    [self setupSession];

    // Start the camera
    [self startCamera];
}

// This method responds to the CCCameraModuleInactiveNotification and the UIApplicationDidEnterBackgroundNotification
-(void)onSetInactive:(NSNotification *)notification {

    // Release the camera
    [self releaseCamera];

    // Stop the capture session
    [self.captureSession stopRunning];

    // Remove any observers
    // [[NSNotificationCenter defaultCenter] removeObserver:self];
}

-(void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

#pragma mark Session management methods

// This method sets up the capture session
-(void)setupSession {

    // Create the captureSession
    self.captureSession = [[AVCaptureSession alloc] init];

    // Initialize the queue for the captureSession
    self.captureSessionQueue = dispatch_queue_create( "session queue", DISPATCH_QUEUE_SERIAL );

    // Set up the preview view.
    CCCameraView *thisCameraView = [CCCameraManager getLatestView];
    thisCameraView.previewView.previewLayer.session = self.captureSession;

    // Initialize the zoom scale parameters
    currentScaleNumber = 1.0;
    startingScaleNumber = 1.0;

    // Set the default setup result
    self.setupResult = CCCameraSetupResultSuccess;

    /*
     Check video authorization status. Video access is required and audio
     access is optional. If audio access is denied, audio is not recorded
     during movie recording.
     */
    switch ( [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo] )
    {
        case AVAuthorizationStatusAuthorized:
        {
            // The user has previously granted access to the camera.
            break;
        }
        case AVAuthorizationStatusNotDetermined:
        {
            /*
             The user has not yet been presented with the option to grant
             video access. We suspend the session queue to delay session
             setup until the access request has completed.

             Note that audio access will be implicitly requested when we
             create an AVCaptureDeviceInput for audio during session setup.
             */
            dispatch_suspend( self.captureSessionQueue );
            [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^( BOOL granted ) {
                if ( ! granted ) {
                    self.setupResult = CCCameraSetupResultCameraNotAuthorized;
                }
                dispatch_resume( self.captureSessionQueue );
            }];
            break;
        }
        default:
        {
            // The user has previously denied access.
            self.setupResult = CCCameraSetupResultCameraNotAuthorized;
            break;
        }
    }

    /*
     Setup the capture session.
     In general it is not safe to mutate an AVCaptureSession or any of its
     inputs, outputs, or connections from multiple threads at the same time.

     Why not do all of this on the main queue?
     Because -[AVCaptureSession startRunning] is a blocking call which can
     take a long time. We dispatch session setup to the sessionQueue so
     that the main queue isn't blocked, which keeps the UI responsive.
     */
    dispatch_async( self.captureSessionQueue, ^{

        // Configure the session
        [self configureSession];

        // Start running the session
        [self.captureSession startRunning];
    } );
}

// This method configures the capture session
-(void)configureSession {

    NSError *error = nil;

    [self.captureSession beginConfiguration];

    /*
     We do not create an AVCaptureMovieFileOutput when setting up the session because the
     AVCaptureMovieFileOutput does not support movie recording with AVCaptureSessionPresetPhoto.
     */
    self.captureSession.sessionPreset = AVCaptureSessionPresetPhoto;

    // Add video input.
    AVCaptureDeviceInput *videoDeviceInput = [AVCaptureDeviceInput deviceInputWithDevice:self.camera error:&error];
    if (!videoDeviceInput) {
        NSLog( @"Could not create video device input: %@", error );
        self.setupResult = CCCameraSetupResultSessionConfigurationFailed;
        [self.captureSession commitConfiguration];
        return;
    }
    if ([self.captureSession canAddInput:videoDeviceInput]) {
        [self.captureSession addInput:videoDeviceInput];
        self.deviceInput = videoDeviceInput;

        dispatch_async( dispatch_get_main_queue(), ^{
            /*
             Why are we dispatching this to the main queue?
             Because AVCaptureVideoPreviewLayer is the backing layer for AVCamPreviewView and UIView
             can only be manipulated on the main thread.
             Note: As an exception to the above rule, it is not necessary to serialize video orientation changes
             on the AVCaptureVideoPreviewLayer’s connection with other session manipulation.

             Use the status bar orientation as the initial video orientation.
             */
            UIInterfaceOrientation statusBarOrientation = [UIApplication sharedApplication].statusBarOrientation;
            AVCaptureVideoOrientation initialVideoOrientation = AVCaptureVideoOrientationPortrait;
            if ( statusBarOrientation != UIInterfaceOrientationUnknown ) {
                initialVideoOrientation = (AVCaptureVideoOrientation)statusBarOrientation;
            }

            [CCCameraManager getLatestView].previewView.previewLayer.connection.videoOrientation = initialVideoOrientation;
        } );
    }
    else {
        NSLog( @"Could not add video device input to the session" );
        self.setupResult = CCCameraSetupResultSessionConfigurationFailed;
        [self.captureSession commitConfiguration];
        return;
    }

    // Add photo output
    AVCaptureStillImageOutput *thisPhotoOutput = [[AVCaptureStillImageOutput alloc] init];
    if ([self.captureSession canAddOutput:thisPhotoOutput]) {

        // Remove any existing photoOutputs first
        if (self.photoOutput != nil) {
            [self.captureSession removeOutput:self.photoOutput];
        }
        [self.captureSession addOutput:thisPhotoOutput];
        self.photoOutput = thisPhotoOutput;
    }
    else {
        NSLog( @"Could not add photo output to the session" );
        self.setupResult = CCCameraSetupResultSessionConfigurationFailed;
        [self.captureSession commitConfiguration];
        return;
    }

    //    CCCameraView *latestView = [CCCameraManager getLatestView];
    //    self.videoCamera = [[CvVideoCamera alloc] initWithParentView:latestView.previewView];
    //    self.videoCamera.defaultAVCaptureDevicePosition = AVCaptureDevicePositionBack;
    //    self.videoCamera.defaultAVCaptureSessionPreset = AVCaptureSessionPreset1280x720;
    //    self.videoCamera.defaultFPS = 30;
    //    //self.videoSource.imageWidth = 1280;
    //    //self.videoSource.imageHeight = 720;
    //    self.videoCamera.delegate = self;
    //    self.videoCamera.recordVideo = NO;
    //    self.videoCamera.grayscaleMode = NO;
    //    [self startCvVideoCamera];

    // Add a video output for capturing the preview frames
    AVCaptureVideoDataOutput *thisVideoOutput = [[AVCaptureVideoDataOutput alloc] init];
    if ([self.captureSession canAddOutput:thisVideoOutput]) {

        // Remove any existing videoOutputs first
        if (self.videoOutput != nil) {
            [self.captureSession removeOutput:self.videoOutput];
        }

        // Set some properties of the videoOutput
        NSDictionary *newVideoSettings = [[NSDictionary alloc] initWithObjectsAndKeys:[NSNumber numberWithInt:kCVPixelFormatType_32BGRA], (id)kCVPixelBufferPixelFormatTypeKey, nil];
        [thisVideoOutput setVideoSettings:newVideoSettings];

        [self.captureSession addOutput:thisVideoOutput];
        self.videoOutput = thisVideoOutput;

        // Set the AVCaptureVideoDataOutputSampleBufferDelegate for the videoOutput if the camera is in scanner mode
        if (self.cameraMode == CCCameraModeScanner) {
            [self.videoOutput setSampleBufferDelegate:self queue:self.captureSessionQueue];
        }
    }
    else {
        NSLog( @"Could not add video output to the session" );
        self.setupResult = CCCameraSetupResultSessionConfigurationFailed;
        [self.captureSession commitConfiguration];
        return;
    }

    [self.captureSession commitConfiguration];
}

#pragma mark Enumerated type conversions

// This method returns a string that describes the current flash mode
-(NSString *)getFlashModeString {

    switch (self.flashMode) {
        case CCCameraFlashModeAuto:
            return @"auto";
        case CCCameraFlashModeOn:
            return @"on";
        case CCCameraFlashModeOff:
            return @"off";
        case CCCameraFlashModeTorch:
            return @"torch";
        default:
            return @"off";
    }
}

// This method returns a CCCameraFlashMode constanst for the given string
-(CCCameraFlashMode)getFlashModeFromString:(NSString *)thisFlashMode {

    if ([thisFlashMode isEqualToString:@"auto"]) {
        return CCCameraFlashModeAuto;
    }
    else if ([thisFlashMode isEqualToString:@"on"]) {
        return CCCameraFlashModeOn;
    }
    else if ([thisFlashMode isEqualToString:@"off"]) {
        return CCCameraFlashModeOff;
    }
    else if ([thisFlashMode isEqualToString:@"torch"]) {
        return CCCameraFlashModeTorch;
    }
    else {
        return CCCameraFlashModeOff;
    }
}

// This method returns a string that describes the current resolution mode
-(NSString *)getResolutionModeString {

    switch (self.resolutionMode) {
        case CCCameraResolutionModeNormal:
            return @"normal";
        case CCCameraResolutionModeHigh:
            return @"high";
        case CCCameraResolutionModeSuper:
            return @"super";
        default:
            return @"normal";
    }
}

// This method returns a CCCameraResolutionMode constanst for the given string
-(CCCameraResolutionMode)getResolutionModeFromString:(NSString *)thisResolutionMode {

    if ([thisResolutionMode isEqualToString:@"normal"]) {
        return CCCameraResolutionModeNormal;
    }
    else if ([thisResolutionMode isEqualToString:@"high"]) {
        return CCCameraResolutionModeHigh;
    }
    else if ([thisResolutionMode isEqualToString:@"super"]) {
        return CCCameraResolutionModeSuper;
    }
    else {
        return CCCameraResolutionModeNormal;
    }
}

// This method returns a string that describes the current camera mode
-(NSString *)getCameraModeString {

    switch (self.cameraMode) {
        case CCCameraModeFastCam:
            return @"fastcam";
        case CCCameraModeCamera:
            return @"camera";
        case CCCameraModeScanner:
            return @"scanner";
        default:
            return @"camera";
    }
}

// This method returns a CCCameraMode constanst for the given string
-(CCCameraMode)getCameraModeFromString:(NSString *)thisCameraMode {

    if ([thisCameraMode isEqualToString:@"fastcam"]) {
        return CCCameraModeFastCam;
    }
    else if ([thisCameraMode isEqualToString:@"camera"]) {
        return CCCameraModeCamera;
    }
    else if ([thisCameraMode isEqualToString:@"scanner"]) {
        return CCCameraModeScanner;
    }
    else {
        return CCCameraModeCamera;
    }
}

// This method updates the flash setting for the camera
-(void)updateFlashSetting:(CCCameraFlashMode)thisFlashMode {

    if ([self.camera isTorchAvailable] && [self.camera isFlashAvailable]) {
        BOOL success = [self.camera lockForConfiguration:nil];
        if (success) {

            switch (self.flashMode) {
                case CCCameraFlashModeAuto:
                    //[self.camera setFlashMode:AVCaptureFlashModeAuto];
                    [self.camera setTorchMode:AVCaptureTorchModeOff];
                    break;
                case CCCameraFlashModeOn:
                    //[self.camera setFlashMode:AVCaptureFlashModeOn];
                    [self.camera setTorchMode:AVCaptureTorchModeOff];
                    break;
                case CCCameraFlashModeOff:
                    //[self.camera setFlashMode:AVCaptureFlashModeOff];
                    [self.camera setTorchMode:AVCaptureTorchModeOff];
                    break;
                case CCCameraFlashModeTorch:
                    //[self.camera setFlashMode:AVCaptureFlashModeOff];
                    [self.camera setTorchMode:AVCaptureTorchModeOn];
                    break;
                default:
                    //[self.camera setFlashMode:AVCaptureFlashModeOff];
                    [self.camera setTorchMode:AVCaptureTorchModeOff];
                    break;
            }

            [self.camera  unlockForConfiguration];
        }
    }
}

// This method resizes a UIImage based on the image resolution setting and the size of the CCCameraView
-(UIImage *)resizeImage:(UIImage*)image {

    // Get a reference to the CCCameraView
    CCCameraView *latestView = [CCCameraManager getLatestView];
    UIDeviceOrientation currentOrientation = [latestView.cameraLayout getCurrentOrientation];
    CGSize viewSize = latestView.bounds.size;
    CGSize imageSize = image.size;

    CGFloat screenWidth = viewSize.width;
    CGFloat screenHeight = viewSize.height;

    CGFloat imageRatio;
    CGFloat screenRatio;

    // If the camera is in scanner mode, then the resulting scanned image may have any aspect ratio.  Scale it's longest size to match the max dimension for the selected resolution mode.
    if (self.cameraMode == CCCameraModeScanner) {

        // Get the max dimension for the current resolution setting
        CGFloat maxDimension = [self getDesiredMinimumHeightForResolution:self.resolutionMode];

        if (image.size.width > image.size.height) {
            imageRatio = image.size.height / image.size.width;
            image = [image CCMScaledToSize:CGSizeMake(maxDimension, floorf(imageRatio * maxDimension))];
        }
        else {
            imageRatio = image.size.width / image.size.height;
            image = [image CCMScaledToSize:CGSizeMake(floorf(imageRatio * maxDimension), maxDimension)];
        }

        return image;
    }

    // Crop and scale the image for other modes besides scanner

    // Check if the device is in portrait
    if (UIDeviceOrientationIsPortrait(currentOrientation))
    {
        screenRatio = screenWidth / screenHeight;
        imageRatio = imageSize.width / imageSize.height;

        CGFloat cropWidth = (screenRatio / imageRatio) * imageSize.width;
        CGFloat xOffset = (imageSize.width - cropWidth) / 2.f;
        CGRect cropRect = CGRectMake(xOffset, 0, cropWidth, imageSize.height);

        NSLog([NSString stringWithFormat:@"The cropRect is %0.f by %0.f", cropRect.size.width, cropRect.size.height]);

        image = [image CCMCroppedImage:cropRect];
    }
    else
    {
        // account for phone locked to portrait
        screenRatio = fminf(screenHeight, screenWidth) / fmaxf(screenHeight, screenWidth);
        imageRatio = imageSize.height / imageSize.width;

        CGFloat cropHeight = (screenRatio / imageRatio) * imageSize.height;
        CGFloat yOffset = (imageSize.height - cropHeight) / 2.f;
        CGRect cropRect = CGRectMake(0, yOffset, imageSize.width, cropHeight);

        NSLog([NSString stringWithFormat:@"The cropRect is %0.f by %0.f", cropRect.size.width, cropRect.size.height]);

        image = [image CCMCroppedImage:cropRect];
    }

    // Get the max dimension for the current resolution setting
    CGFloat maxDimension = [self getDesiredMinimumHeightForResolution:self.resolutionMode];

    // Resize to max dimension on one side keeping aspect ratio
    if (UIDeviceOrientationIsPortrait(currentOrientation)) {
        CGFloat newImageRatio = image.size.width / image.size.height;
        image = [image CCMScaledToSize:CGSizeMake(floorf(newImageRatio * maxDimension), maxDimension)];
    }
    else {
        CGFloat newImageRatio = image.size.height / image.size.width;
        image = [image CCMScaledToSize:CGSizeMake(maxDimension, floorf(newImageRatio * maxDimension))];
    }

    return image;
}

// This method returns the minimum desired image height in pixels for the given resolution setting
-(CGFloat)getDesiredMinimumHeightForResolution:(CCCameraResolutionMode)thisResolutionMode {

    if (thisResolutionMode == CCCameraResolutionModeSuper) {
        return 2160.0f;
    }
    else if (thisResolutionMode == CCCameraResolutionModeHigh) {
        return 1920.0f;
    }
    else {
        return 1440.0f;
    }
}

// This method uses the orientation of the layout and the current camera to determine the equivalent image orientation that can be used to display the image oriented upright with the device in portrait.
-(UIImageOrientation)getCurrentImageOrientation {

    UIImageOrientation orientation;

    // Get the current orientation of the layout
    CCCameraView *latestView = [CCCameraManager getLatestView];
    UIDeviceOrientation currentOrientation = [latestView.cameraLayout getCurrentOrientation];

    // Check if the current camera is forward-facing
    BOOL cameraIsFront = self.camera.position == AVCaptureDevicePositionFront;

    // Determine an equivalent image orientation for the output image given the current orientation of the layout and whether or not the camera is front-facing
    switch (currentOrientation) {
        case UIDeviceOrientationLandscapeLeft:
            if (cameraIsFront) {
                orientation = UIImageOrientationDownMirrored;
            }
            else {
                orientation = UIImageOrientationUp;
            }
            break;
        case UIDeviceOrientationPortraitUpsideDown:
            if (cameraIsFront) {
                orientation = UIImageOrientationRightMirrored;
            }
            else {
                orientation = UIImageOrientationLeft;
            }
            break;
        case UIDeviceOrientationLandscapeRight:
            if (cameraIsFront) {
                orientation = UIImageOrientationUpMirrored;
            }
            else {
                orientation = UIImageOrientationDown;
            }
            break;
        default:
            if (cameraIsFront) {
                orientation = UIImageOrientationLeftMirrored;
            }
            else {
                orientation = UIImageOrientationRight;
            }
            break;
    }

    return orientation;
}

// This method processes the photo data and executes the proper callback after an image is received
-(void)processPhotoData {
    [self processPhotoData:nil];
}

// This method processes the photo data and executes the proper callback after an image is received
-(void)processPhotoData:(UIImage *)latestScanImage {

    // If the photoData wasn't nil, then save the image to the file system.  Or if the current camera mode is CCCameraModeScanner, then make sure the latestScanImage isn't nil.
    if ((self.cameraMode == CCCameraModeScanner && latestScanImage != nil) || self.photoData != nil) {

        // Get a reference to the CCCameraView
        CCCameraView *latestView = [CCCameraManager getLatestView];

        // If the current camera mode is CCCameraModeScanner, then use process the latestScanImage
        UIImage *originalImage;
        if (self.cameraMode == CCCameraModeScanner) {
            originalImage = latestScanImage;
        }

        // Otherwise, process the photoData
        else {
            originalImage = [UIImage imageWithData:self.photoData scale:1.0f];
        }

        // Rotate the original image to the proper orientation.  Using the initWithCGImage method seems to produce better results for the images returned from the image processor, whereas the CCMRotateForImageOrientation method works better for images returned directly from the camera.
        UIImage *rotatedImage;
        if (self.cameraMode == CCCameraModeScanner)  {
            rotatedImage = ([[UIImage alloc] initWithCGImage:latestScanImage.CGImage scale:1.f orientation:[self getCurrentImageOrientation]]);
        }
        else {
            rotatedImage = [originalImage CCMRotateForImageOrientation:[self getCurrentImageOrientation]];
        }

        // Crop the image to the size of the CCCameraView
        UIImage *croppedImage = [self resizeImage:rotatedImage];

        // Set the additional pieces of metadata
        NSMutableDictionary *mutableMetadata = [[self.photoData exifDataForImage] mutableCopy];
        NSDate *now = [NSDate date];
        [mutableMetadata setDateDigitized:now];
        [mutableMetadata setDateOriginal:now];
        [mutableMetadata setImageOrientation:[self getCurrentImageOrientation]];
        [mutableMetadata setLocation:[latestView getExifLocation]];

        // Get the data for the rotated image
        NSData *jpeg = UIImageJPEGRepresentation(croppedImage, 0.6f);

        // Write the rotated image data to the file system
        NSString *filePath = [StorageUtility writeDataToFile:jpeg];

        // If saveToPhone is set, then save the image to the device in addition to sending it to the server.
        BOOL saveToPhone = [[NSUserDefaults standardUserDefaults] boolForKey:@"SaveToPhone"];
        if (saveToPhone) {

            // Check for permission to acceess the camera roll
            [PHPhotoLibrary requestAuthorization:^( PHAuthorizationStatus status ) {
                if (status == PHAuthorizationStatusAuthorized) {

                    NSLog(@"Trying to save a photo to the camera roll");

                    // Add the photo to the camera roll
                    [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                        PHAssetCreationRequest *creationRequest = [PHAssetCreationRequest creationRequestForAsset];
                        [creationRequest addResourceWithType:PHAssetResourceTypePhoto data:UIImageJPEGRepresentation(croppedImage, 0.7f) options:nil];
                    } completionHandler:^( BOOL success, NSError * _Nullable error ) {
                        if (!success) {
                            NSLog(@"Error occurred while saving photo to photo library: %@", error);
                        }
                    }];
                }
                else {
                    NSLog( @"Not authorized to save photo" );
                }
            }];
        }

        // Execute the proper callback depending on the current camera mode
        if (self.cameraMode != CCCameraModeFastCam) {
            [latestView doPhotoTaken:filePath :(int)CGImageGetWidth(croppedImage.CGImage) :(int)CGImageGetHeight(croppedImage.CGImage) completion:^{

              // wait two seconds to enable buttons to allow editor to load
              dispatch_time_t delayTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2.0 * NSEC_PER_SEC));
              dispatch_after(delayTime, dispatch_get_main_queue(), ^(void){
                // Hide the loading view and enable the buttons again
                [latestView.cameraLayout hideLoadingView];
                [latestView.cameraLayout enableButtons];
              });
            }];
        }
        else {
            [latestView doPhotoAccepted:filePath :(int)CGImageGetWidth(croppedImage.CGImage) :(int)CGImageGetHeight(croppedImage.CGImage)];
        }
    }
}

#pragma mark CCCameraDelegate methods

// This method starts the camera
-(void)startCamera {

    dispatch_async( self.captureSessionQueue, ^{

        NSArray<AVCaptureDevice *> *devices = [AVCaptureDevice devices];
        AVCaptureDevice *newVideoDevice = nil;

        // Look for a device with the preferred position
        for (AVCaptureDevice *device in devices) {
            if (device.position == self.cameraType) {
                newVideoDevice = device;
                break;
            }
        }

        if (newVideoDevice) {

            // Set the reference to the camera
            self.camera = newVideoDevice;
            NSError *error;
            [self.camera lockForConfiguration:&error];
            if ([self.camera isFocusModeSupported:AVCaptureFocusModeContinuousAutoFocus]) {
                [self.camera setFocusMode:AVCaptureFocusModeContinuousAutoFocus];
            }
            if ([self.camera isExposureModeSupported:AVCaptureExposureModeContinuousAutoExposure]) {
                [self.camera setExposureMode:AVCaptureExposureModeContinuousAutoExposure];
            }
            [self.camera unlockForConfiguration];

            // Remove the existing device input first if there is one, since using the front and back camera simultaneously is not supported.
            [self releaseCamera];

            // Uncomment this section to add listeners for the completion of the auto focus and auto exposure routines
            ///////////////////////////////////////////
            // Add observers for the focus and exposure
            //[self.camera addObserver:self forKeyPath:@"adjustingFocus" options:0 context:nil];
            //[self.camera addObserver:self forKeyPath:@"adjustingExposure" options:0 context:nil];
            ///////////////////////////////////////////

            // Add the new input to the capture session
            [self configureSession];

            // Set the layout object's reference to the camera
            CCCameraView *latestView = [CCCameraManager getLatestView];
            [latestView.cameraLayout setCameraObject:latestView.camera];

            // Update the flash setting for this camera
            [self updateFlashSetting:self.flashMode];
        }
    });
}

// This method releases the camera
-(void)releaseCamera {

    if (self.captureSession != nil) {
        [self.captureSession beginConfiguration];
        if (self.deviceInput != nil) {
            [self.captureSession removeInput:self.deviceInput];
        }
        if (self.photoOutput != nil) {
            [self.captureSession removeOutput:self.photoOutput];
            self.photoOutput = nil;
        }
        if (self.videoOutput != nil) {
            [self.captureSession removeOutput:self.videoOutput];
            self.videoOutput = nil;
        }
        [self.captureSession commitConfiguration];

        // Uncomment this section if you'd like to add listeners for the completion of the auto focus or auto exposure routines
        ///////////////////////////////////////////
        /*
         // Remove the observers for the focus and exposure if necessary
         @try {
         [self.camera removeObserver:self forKeyPath:@"adjustingFocus" context:nil];
         [self.camera removeObserver:self forKeyPath:@"adjustingExposure" context:nil];
         }
         @catch(id exception) {
         }*/
        ////////////////////////////////////////////
    }
}

// This method sets the camera resolution based on the given string
-(void)setResolution:(NSString *)resolutionMode {

    // TODO: Update setResolution method
}

// This method returns a boolean that describes whether or not the device has an available front-facing camera
-(BOOL)hasFrontCamera {

    NSArray<AVCaptureDevice *> *devices = [AVCaptureDevice devices];
    for (AVCaptureDevice *device in devices) {
        if (device.position == AVCaptureDevicePositionFront) {
            return YES;
        }
    }

    // If control reaches this point, then no forward-facing camera was found
    return NO;
}

// This method returns a boolean that describes whether or not the device has an available rear-facing camera
-(BOOL)hasRearCamera {

    NSArray<AVCaptureDevice *> *devices = [AVCaptureDevice devices];
    for (AVCaptureDevice *device in devices) {
        if (device.position == AVCaptureDevicePositionBack) {
            return YES;
        }
    }

    // If control reaches this point, then no rear-facing camera was found
    return NO;
}

// This method toggles the camera between forward-facing and rear-facing
-(void)toggleCamera {

    AVCaptureDevicePosition preferredPosition;

    switch (self.cameraType) {
        case AVCaptureDevicePositionUnspecified:
        case AVCaptureDevicePositionFront:
            preferredPosition = AVCaptureDevicePositionBack;
            break;
        case AVCaptureDevicePositionBack:
            preferredPosition = AVCaptureDevicePositionFront;
            break;
    }

    // Update the camera type
    self.cameraType = preferredPosition;

    // Close the current camera
    [self releaseCamera];

    // Reset the currentScaleNumber and the startingScaleNumber
    currentScaleNumber = 1.0;
    startingScaleNumber = 1.0;

    // Initialize the camera again
    [self startCamera];
}

// This method returns a boolean that describes whether or not the current camera has flash capability
-(BOOL)hasFlash {
    return [self.camera hasTorch];
}

// This method toggles the flash state
-(void)toggleFlash {

    if (self.flashMode == CCCameraFlashModeTorch) {
        self.flashMode = CCCameraFlashModeOff;
    }
    else {
        self.flashMode = CCCameraFlashModeTorch;
    }

    // Update the flash mode for the camera
    [self updateFlashSetting:self.flashMode];
}

// This method captures a photo from the camera
-(void)takePicture {

    // The volumeButtonHandler may try to trigger this method, so make sure the current camera mode is CCCameraModeCamera or CCCameraModeFastCam before trying to capture a photo.
    if (!(self.cameraMode == CCCameraModeCamera || self.cameraMode == CCCameraModeFastCam)) {
        return;
    }

    // Animate the screen flash
    CCCameraView *latestView = [CCCameraManager getLatestView];
    [latestView.cameraLayout animateScreenFlash];

    // Show the loading view and disable all the buttons while the photo is processing
    if (self.cameraMode == CCCameraModeCamera) {
        [latestView.cameraLayout showLoadingView];
        [latestView.cameraLayout disableButtons];

        // Pause the camera preview while the photo is processing
        [latestView.previewView.previewLayer.connection setEnabled:NO];
    }

    /*
     Retrieve the video preview layer's video orientation on the main queue before
     entering the session queue. We do this to ensure UI elements are accessed on
     the main thread and session configuration is done on the session queue.
     */

    AVCaptureVideoOrientation videoPreviewLayerVideoOrientation = latestView.previewView.previewLayer.connection.videoOrientation;

    dispatch_async(self.captureSessionQueue, ^{

        // Update the photo output's connection to match the video orientation of the video preview layer.
        AVCaptureConnection *photoOutputConnection = [self.photoOutput connectionWithMediaType:AVMediaTypeVideo];
        photoOutputConnection.videoOrientation = videoPreviewLayerVideoOrientation;

        [self.photoOutput captureStillImageAsynchronouslyFromConnection:[self.photoOutput connectionWithMediaType:AVMediaTypeVideo] completionHandler:^(CMSampleBufferRef imageDataSampleBuffer, NSError *error) {

            if (imageDataSampleBuffer) {

                self.photoData = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageDataSampleBuffer];

                // If the photoData wasn't nil, then save the image to the file system
                [self processPhotoData];

                //                if (self.photoData != nil) {
                //
                //                    // Get a reference to the CCCameraView
                //                    CCCameraView *thisCameraView = [CCCameraManager getLatestView];
                //
                //                    // Create a UIImage from the photoData and rotate it to the proper orientation
                //                    UIImage *originalImage = [UIImage imageWithData:self.photoData scale:1.0f];
                //                    UIImage *rotatedImage = [originalImage CCMRotateForImageOrientation:[self getCurrentImageOrientation]];
                //
                //                    // Crop the image to the size of the CCCameraView
                //                    UIImage *croppedImage = [self resizeImage:rotatedImage];
                //
                //                    // Set the additional pieces of metadata
                //                    NSMutableDictionary *mutableMetadata = [[self.photoData exifDataForImage] mutableCopy];
                //                    NSDate *now = [NSDate date];
                //                    [mutableMetadata setDateDigitized:now];
                //                    [mutableMetadata setDateOriginal:now];
                //                    [mutableMetadata setImageOrientation:[self getCurrentImageOrientation]];
                //                    [mutableMetadata setLocation:[thisCameraView getExifLocation]];
                //
                //                    // Get the data for the rotated image
                //                    NSData *jpeg = UIImageJPEGRepresentation(croppedImage, 0.6f);
                //
                //                    // Write the rotated image data to the file system
                //                    NSString *filePath = [StorageUtility writeDataToFile:jpeg];
                //
                //                    // If saveToPhone is set, then save the image to the device in addition to sending it to the server.
                //                    BOOL saveToPhone = [[NSUserDefaults standardUserDefaults] boolForKey:@"SaveToPhone"];
                //                    if (saveToPhone) {
                //
                //                        // Check for permission to acceess the camera roll
                //                        [PHPhotoLibrary requestAuthorization:^( PHAuthorizationStatus status ) {
                //                            if (status == PHAuthorizationStatusAuthorized) {
                //
                //                                NSLog(@"Trying to save a photo to the camera roll");
                //
                //                                // Add the photo to the camera roll
                //                                [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                //                                    PHAssetCreationRequest *creationRequest = [PHAssetCreationRequest creationRequestForAsset];
                //                                    [creationRequest addResourceWithType:PHAssetResourceTypePhoto data:UIImageJPEGRepresentation(croppedImage, 0.7f) options:nil];
                //                                } completionHandler:^( BOOL success, NSError * _Nullable error ) {
                //                                    if (!success) {
                //                                        NSLog(@"Error occurred while saving photo to photo library: %@", error);
                //                                    }
                //                                }];
                //                            }
                //                            else {
                //                                NSLog( @"Not authorized to save photo" );
                //                            }
                //                        }];
                //                    }
                //
                //                    // Execute the proper callback depending on the current camera mode
                //                    if (self.cameraMode != CCCameraModeFastCam) {
                //                        [thisCameraView doPhotoTaken:filePath :(int)CGImageGetWidth(croppedImage.CGImage) :(int)CGImageGetHeight(croppedImage.CGImage)];
                //
                //                        // Hide the loading view and enable the buttons again
                //                        [latestView.cameraLayout hideLoadingView];
                //                        [latestView.cameraLayout enableButtons];
                //                    }
                //                    else {
                //                        [thisCameraView doPhotoAccepted:filePath :(int)CGImageGetWidth(croppedImage.CGImage) :(int)CGImageGetHeight(croppedImage.CGImage)];
                //                    }
                //                }
            }
            else {

                // Hide the loading view
                [latestView.cameraLayout hideLoadingView];

                // TODO: Present an error message?
            }
        }];

    } );
}

// This method handles a screen touch event
-(void)handleTouchEvent:(UIEvent *)event {

    // Handle multi-touch events
    if ([[event allTouches] count] > 1) {

    }

    // Handle single-touch events
    else {

        // Trigger the tap-to-autofocus and tap-to-expose
        [self handleFocus:event];
    }
}

// This method handles a pinch/zoom event
-(void)handleZoom:(double)zoomScale :(BOOL)zoomEnded {

    // Set the currentScaleNumber
    currentScaleNumber = startingScaleNumber * zoomScale;

    // Clamp the currentScaleNumber to a range of [MIN_PINCH_SCALE_NUM - MAX_PINCH_SCALE_NUM]
    if (currentScaleNumber < MIN_PINCH_SCALE_NUM) {
        currentScaleNumber = MIN_PINCH_SCALE_NUM;
    }
    else if (currentScaleNumber > MAX_PINCH_SCALE_NUM) {
        currentScaleNumber = MAX_PINCH_SCALE_NUM;
    }

    NSError *error = nil;
    if ([self.camera lockForConfiguration:&error]) {
        self.camera.videoZoomFactor = currentScaleNumber;
        [self.camera unlockForConfiguration];
    } else {
        NSLog(@"error: %@", error);
    }

    // If this zoom event has ended, then set the startingScaleNumber for the next zoome event
    if (zoomEnded) {
        startingScaleNumber = currentScaleNumber;
    }
}

#pragma mark AVCaptureVideoDataOutputSampleBufferDelegate methods

- (void)captureOutput:(AVCaptureOutput *)captureOutput didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    NSLog(@"Got a preview frame!");

    CCCameraView *latestView = [CCCameraManager getLatestView];

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // See the following link for information about converting the CMSampleBufferRef into a UIImage
    // https://gist.github.com/jebai/8108270
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Get a CMSampleBuffer's Core Video image buffer for the media data
    CVImageBufferRef imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);

    // Lock the base address of the pixel buffer
    CVPixelBufferLockBaseAddress(imageBuffer, 0);

    // Get the number of bytes per row for the pixel buffer
    void *baseAddress = CVPixelBufferGetBaseAddress(imageBuffer);

    // Get the number of bytes per row for the pixel buffer
    size_t bytesPerRow = CVPixelBufferGetBytesPerRow(imageBuffer);

    // Get the pixel buffer width and height
    size_t width = CVPixelBufferGetWidth(imageBuffer);
    size_t height = CVPixelBufferGetHeight(imageBuffer);

    NSLog(@"w: %zu h: %zu bytesPerRow:%zu", width, height, bytesPerRow);

    // Check if the image processor has been initialized yet
    if (!self.ipDidAllocate) {

        // Initialize the image processor with the width and height of the preview image
        [latestView.cameraLayout initImageProcessor:(int)width :(int)height :MAX_DIM_PROCESSING_OUTPUT];

        // Set the ipDidAllocate flag so that subsequent frames get sent to the image processor
        self.ipDidAllocate = YES;
    }

    // Otherwise, send the preview image to the image processor
    else {

        // Create a device-dependent RGB color space
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();

        // Create a bitmap graphics context with the sample buffer data
        CGContextRef context = CGBitmapContextCreate(baseAddress,
                                                     width,
                                                     height,
                                                     8,
                                                     bytesPerRow,
                                                     colorSpace,
                                                     kCGBitmapByteOrder32Little
                                                     | kCGImageAlphaPremultipliedFirst);
        // Create a Quartz image from the pixel data in the bitmap graphics context
        CGImageRef quartzImage = CGBitmapContextCreateImage(context);

        // Unlock the pixel buffer
        CVPixelBufferUnlockBaseAddress(imageBuffer,0);

        // Free up the context and color space
        CGContextRelease(context);
        CGColorSpaceRelease(colorSpace);

        // TODO: Set the proper image orientation
        // Create an image object from the Quartz image
        UIImage *image = [UIImage imageWithCGImage:quartzImage
                                             scale:1.0f
                                       orientation:UIImageOrientationUp];

        // Release the Quartz image
        CGImageRelease(quartzImage);

        // Send the image to the image processor
        BOOL requestNextFrame = [latestView.cameraLayout setPreviewBytes:image];

        // If the scan is done, then stop sending the frames to the image processor
        if (!requestNextFrame) {

            // Remove the sample buffer delegate from the video output
            [self.videoOutput setSampleBufferDelegate:nil queue:nil];

            // Remove the ipDidAllocate flag so that the image processor will get initialized again the next time the scanner mode is selected
            self.ipDidAllocate = NO;

            // Get the image output from the image processor
            UIImage *latestScanImage = [latestView.cameraLayout getOutputImage];

            // Process the output image
            [self processPhotoData:latestScanImage];
        }
    }
}

#pragma mark CvVideoCameraDelegate methods

// This method starts the CvVideoCamera on the main thread
-(void)startCvVideoCamera {

    // If this isn't being called from the main thread, switch it to the main thread
    if ([NSThread currentThread] != [NSThread mainThread]) {
        [self performSelector:@selector(startCvVideoCamera) onThread:[NSThread mainThread] withObject:nil waitUntilDone:NO];
        return;
    }

    // Start the CvVideoCamera
    if (self.videoCamera != nil) {
        [self.videoCamera start];
    }
}

#ifdef __cplusplus
- (void)processImage:(cv::Mat&)image; {
    // Do some OpenCV stuff with the image
    NSLog(@"processImage was called!");
}
#endif

#pragma mark Touch event handling

// This method handles auto focus events
-(void)handleFocus:(UIEvent *)event {

    // Check to make sure that the camera supports focus
    if (self.camera != nil && [self.camera isFocusModeSupported:AVCaptureFocusModeAutoFocus] && [self.camera isFocusPointOfInterestSupported]) {

        // Get a reference to the CCCameraView
        CCCameraView *latestView = [CCCameraManager getLatestView];

        // Get the coordinates of the touch point
        UITouch *touch = [[event allTouches] anyObject];
        CGPoint thisTouchPoint = [touch locationInView:latestView];

        // Normalize the touch point coordinates with respect to the height and width of the camera view
        int previewWidth = latestView.frame.size.width;
        int previewHeight = latestView.frame.size.height;

        // The x and y coordinates from the touch event need to be converted to normalized landscape coordinates in order to set the focus point of interest

        // First, normalize the coordinates in the camera view reference frame
        double n_y = thisTouchPoint.y/previewHeight;
        double n_x = thisTouchPoint.x/previewWidth;

        // Then convert the normalized coordinates to a landcape reference frame with (0, 0) representing the upper left and (1, 1) representing the lower right when the home button is on the right.
        CGPoint nsc = CGPointMake(n_y, 1.0 - n_x);

        // Set the focus point of interest
        NSError *error;
        [self.camera lockForConfiguration:&error];
        [self.camera setFocusPointOfInterest:nsc];
        [self.camera setFocusMode:AVCaptureFocusModeContinuousAutoFocus];

        // Check if the camera support auto exposure
        if ([self.camera isExposureModeSupported:AVCaptureExposureModeAutoExpose] && [self.camera isExposurePointOfInterestSupported]) {

            // Set the exposure point of interest
            [self.camera setExposurePointOfInterest:nsc];
            [self.camera setExposureMode:AVCaptureExposureModeContinuousAutoExposure];
        }
        [self.camera unlockForConfiguration];

        // Show the focusIndicatorView
        [latestView.cameraLayout showAutoFocusIndicator:thisTouchPoint :YES];
    }
}

#pragma mark Persistent settings

// This method persists the flash mode to the NSUserDefaults
-(void)persistFlashMode:(NSString *)thisFlashMode {

    // Persist the flash mode
    self.flashMode = [self getFlashModeFromString:thisFlashMode];
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:PREFS_FLASH_MODE];
    [[NSUserDefaults standardUserDefaults] setObject:[NSNumber numberWithInt:self.flashMode] forKey:PREFS_FLASH_MODE];
}

// This method persists the resolution mode to the NSUserDefaults
-(void)persistResolutionMode:(NSString *)thisResolutionMode {

    // Persist the resolution mode
    self.resolutionMode = [self getResolutionModeFromString:thisResolutionMode];
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:PREFS_RESOLUTION_MODE];
    [[NSUserDefaults standardUserDefaults] setObject:[NSNumber numberWithInt:self.resolutionMode] forKey:PREFS_RESOLUTION_MODE];
}

// This method persists the camera mode to the NSUserDefaults
-(void)persistCameraMode:(NSString *)thisCameraMode {

    // Persist the camera mode
    self.cameraMode = [self getCameraModeFromString:thisCameraMode];
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:PREFS_CAMERA_MODE];
    [[NSUserDefaults standardUserDefaults] setObject:[NSNumber numberWithInt:self.cameraMode] forKey:PREFS_CAMERA_MODE];

    // Set the AVCaptureVideoDataOutputSampleBufferDelegate for the videoOutput if the camera is in scanner mode.
    if (self.cameraMode == CCCameraModeScanner) {
        [self.videoOutput setSampleBufferDelegate:self queue:self.captureSessionQueue];
    }

    // Otherwise, remove the AVCaptureVideoDataOutputSampleBufferDelegate
    else {
        [self.videoOutput setSampleBufferDelegate:nil queue:nil];

        // Remove the ipDidAllocate flag so that the image processor will get initialized again the next time the scanner mode is selected
        self.ipDidAllocate = NO;

        // Clear the visible preview from the image processor
        CCCameraView *latestView = [CCCameraManager getLatestView];
        [latestView.cameraLayout clearVisiblePreview];
    }
}

#pragma mark -
#pragma mark Key-Value observation methods

-(void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {

    if ([keyPath isEqualToString:@"adjustingFocus"] || [keyPath isEqualToString:@"adjustingExposure"]) {

        // If the camera is no longer focusing or exposing, then hide the focus indicator view
        if (self.camera != nil && !self.camera.isAdjustingFocus && !self.camera.isAdjustingExposure) {

            // Get a reference to the CCCameraView
            CCCameraView *latestView = [CCCameraManager getLatestView];
            [latestView.cameraLayout hideAutoFocusIndicator];
        }
    }
}


@end
