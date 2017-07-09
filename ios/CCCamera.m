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

// These strings are used to save and retrieve persistent settings to the NSUserDefaults
#define PREFS_FLASH_MODE @"PREFS_FLASH_MODE"
#define PREFS_RESOLUTION_MODE @"PREFS_RESOLUTION_MODE"
#define PREFS_CAMERA_MODE @"PREFS_CAMERA_MODE"

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

// The AVCaptureDeviceDiscoverySession class is used to find the available cameras on this device
@interface AVCaptureDeviceDiscoverySession (Utilities)
-(NSInteger)uniqueDevicePositionsCount;
@end

@implementation AVCaptureDeviceDiscoverySession (Utilities)

// This method returns the number of unique camera postions that are available on this device
- (NSInteger)uniqueDevicePositionsCount {
    
    // Create an array of camera device positions
    NSMutableArray<NSNumber *> *uniqueDevicePositions = [NSMutableArray array];
    
    for (AVCaptureDevice *device in self.devices) {
        if (![uniqueDevicePositions containsObject:@(device.position)]) {
            [uniqueDevicePositions addObject:@(device.position)];
        }
    }
    
    return uniqueDevicePositions.count;
}

@end

@interface CCCamera()
    
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

// The cameraDeviceDiscoverySession contains an array of the available cameras
@property (nonatomic) AVCaptureDeviceDiscoverySession *cameraDeviceDiscoverySession;

@end

@implementation CCCamera

@synthesize flashMode;
@synthesize resolutionMode;
@synthesize cameraMode;
@synthesize setupResult;
@synthesize cameraDeviceDiscoverySession;
@synthesize captureSessionQueue;
@synthesize captureSession;
@synthesize camera;
@synthesize deviceInput;
@synthesize photoOutput;
@synthesize photoData;

-(id)init {
    
    self = [super init];
    if (self) {
        
        // Choose the back dual camera if available, otherwise default to a wide angle camera.
        AVCaptureDevice *videoDevice = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInDuoCamera mediaType:AVMediaTypeVideo position:AVCaptureDevicePositionBack];
        if (!videoDevice) {
            
            // If the back dual camera is not available, default to the back wide angle camera.
            videoDevice = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInWideAngleCamera mediaType:AVMediaTypeVideo position:AVCaptureDevicePositionBack];
            
            // In some cases where users break their phones, the back wide angle camera is not available. In this case, we should default to the front wide angle camera.
            if (! videoDevice) {
                videoDevice = [AVCaptureDevice defaultDeviceWithDeviceType:AVCaptureDeviceTypeBuiltInWideAngleCamera mediaType:AVMediaTypeVideo position:AVCaptureDevicePositionFront];
            }
        }
        
        // Set the reference to the camera
        self.camera = videoDevice;
        NSError *error;
        [self.camera lockForConfiguration:&error];
        [self.camera setFocusMode:AVCaptureFocusModeContinuousAutoFocus];
        [self.camera setExposureMode:AVCaptureExposureModeContinuousAutoExposure];
        [self.camera unlockForConfiguration];
        
        // Add observers for the focus and exposure
        [self.camera addObserver:self forKeyPath:@"adjustingFocus" options:0 context:nil];
        [self.camera addObserver:self forKeyPath:@"adjustingExposure" options:0 context:nil];
        
        // Setup the capture session
        [self setupSession];
        
        // Get the saved settings from the NSUserDefaults.  Restrict the possible flash modes to "torch" and "off".
        self.flashMode = [[[NSUserDefaults standardUserDefaults] objectForKey:PREFS_FLASH_MODE] intValue];
        if (!(self.flashMode == CCCameraFlashModeOff || self.flashMode == CCCameraFlashModeTorch)) {
            self.flashMode = CCCameraFlashModeOff;
        }
        self.resolutionMode = [[[NSUserDefaults standardUserDefaults] objectForKey:PREFS_RESOLUTION_MODE] intValue];
        self.cameraMode = [[[NSUserDefaults standardUserDefaults] objectForKey:PREFS_CAMERA_MODE] intValue];
    }
    
    return self;
}

#pragma mark Session management methods

// This method sets up the capture session
-(void)setupSession {
    
    // Create the captureSession
    self.captureSession = [[AVCaptureSession alloc] init];
    
    // Create the device discovery session.
    NSArray<AVCaptureDeviceType> *deviceTypes = @[AVCaptureDeviceTypeBuiltInWideAngleCamera, AVCaptureDeviceTypeBuiltInDuoCamera];
    self.cameraDeviceDiscoverySession = [AVCaptureDeviceDiscoverySession discoverySessionWithDeviceTypes:deviceTypes mediaType:AVMediaTypeVideo position:AVCaptureDevicePositionUnspecified];
    
    // Initialize the queue for the captureSession
    self.captureSessionQueue = dispatch_queue_create( "session queue", DISPATCH_QUEUE_SERIAL );
    
    // Set up the preview view.
    CCCameraView *thisCameraView = [CCCameraManager getLatestView];
    thisCameraView.previewView.previewLayer.session = self.captureSession;
    
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
    
    if (self.setupResult != CCCameraSetupResultSuccess ) {
        return;
    }
    
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
    AVCapturePhotoOutput *thisPhotoOutput = [[AVCapturePhotoOutput alloc] init];
    if ([self.captureSession canAddOutput:thisPhotoOutput]) {
        [self.captureSession addOutput:thisPhotoOutput];
        self.photoOutput = thisPhotoOutput;
        
        // Enable high resolution photo capture
        self.photoOutput.highResolutionCaptureEnabled = YES;
    }
    else {
        NSLog( @"Could not add photo output to the session" );
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

#pragma mark CCCameraDelegate methods

// This method starts the camera
-(void)startCamera {
    
    // TODO: Update startCamera method
}

// This method releases the camera
-(void)releaseCamera {
    
    // TODO: Update releaseCamera method
    if (self.captureSession != nil && self.deviceInput != nil) {
        [self.captureSession beginConfiguration];
        [self.captureSession removeInput:self.deviceInput];
        [self.captureSession commitConfiguration];
        
        // Remove the observers for the focus and exposure
        [self.camera removeObserver:self forKeyPath:@"isAdjustingFocus" context:nil];
        [self.camera removeObserver:self forKeyPath:@"isAdjustingExposure" context:nil];
    }
}

// This method sets the camera resolution based on the given string
-(void)setResolution:(NSString *)resolutionMode {
    
    // TODO: Update setResolution method
}

// This method returns a boolean that describes whether or not the device has an available front-facing camera
-(BOOL)hasFrontCamera {
    
    if (self.cameraDeviceDiscoverySession == nil || self.cameraDeviceDiscoverySession.devices == nil) {
        return NO;
    }
    else {
        NSArray<AVCaptureDevice *> *devices = self.cameraDeviceDiscoverySession.devices;
        for (AVCaptureDevice *device in devices) {
            if (device.position == AVCaptureDevicePositionFront) {
                return YES;
            }
        }
    }
    
    // If control reaches this point, then no forward-facing camera was found
    return NO;
}

// This method returns a boolean that describes whether or not the device has an available rear-facing camera
-(BOOL)hasRearCamera {
    
    if (self.cameraDeviceDiscoverySession == nil || self.cameraDeviceDiscoverySession.devices == nil) {
        return NO;
    }
    else {
        NSArray<AVCaptureDevice *> *devices = self.cameraDeviceDiscoverySession.devices;
        for (AVCaptureDevice *device in devices) {
            if (device.position == AVCaptureDevicePositionBack) {
                return YES;
            }
        }
    }
    
    // If control reaches this point, then no rear-facing camera was found
    return NO;
}

// This method toggles the camera between forward-facing and rear-facing
-(void)toggleCamera {
    
    dispatch_async( self.captureSessionQueue, ^{
        AVCaptureDevicePosition currentPosition = self.camera.position;
        
        AVCaptureDevicePosition preferredPosition;
        AVCaptureDeviceType preferredDeviceType;
        
        switch ( currentPosition )
        {
            case AVCaptureDevicePositionUnspecified:
            case AVCaptureDevicePositionFront:
                preferredPosition = AVCaptureDevicePositionBack;
                preferredDeviceType = AVCaptureDeviceTypeBuiltInDuoCamera;
                break;
            case AVCaptureDevicePositionBack:
                preferredPosition = AVCaptureDevicePositionFront;
                preferredDeviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera;
                break;
        }
        
        NSArray<AVCaptureDevice *> *devices = self.cameraDeviceDiscoverySession.devices;
        AVCaptureDevice *newVideoDevice = nil;
        
        // First, look for a device with both the preferred position and device type.
        for ( AVCaptureDevice *device in devices ) {
            if ( device.position == preferredPosition && [device.deviceType isEqualToString:preferredDeviceType] ) {
                newVideoDevice = device;
                break;
            }
        }
        
        // Otherwise, look for a device with only the preferred position.
        if ( ! newVideoDevice ) {
            for ( AVCaptureDevice *device in devices ) {
                if ( device.position == preferredPosition ) {
                    newVideoDevice = device;
                    break;
                }
            }
        }
        
        if ( newVideoDevice ) {
            AVCaptureDeviceInput *videoDeviceInput = [AVCaptureDeviceInput deviceInputWithDevice:newVideoDevice error:NULL];
            
            // Set the reference to the camera
            self.camera = newVideoDevice;
            
            // Remove the existing device input first, since using the front and back camera simultaneously is not supported.
            [self releaseCamera];
            
            [self.captureSession beginConfiguration];
            
            if ( [self.captureSession canAddInput:videoDeviceInput] ) {
                [[NSNotificationCenter defaultCenter] removeObserver:self name:AVCaptureDeviceSubjectAreaDidChangeNotification object:camera];
                
                [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(subjectAreaDidChange:) name:AVCaptureDeviceSubjectAreaDidChangeNotification object:newVideoDevice];
                
                [self.captureSession addInput:videoDeviceInput];
                self.deviceInput = videoDeviceInput;
            }
            else {
                [self.captureSession addInput:self.deviceInput];
            }
            
            [self.captureSession commitConfiguration];
        }
    } );
}

// This method returns a boolean that describes whether or not the current camera has flash capability
-(BOOL)hasFlash {
    return [self.camera hasFlash];
}

// This method toggles the flash state
-(void)toggleFlash {
    
    if (self.flashMode == CCCameraFlashModeTorch) {
        self.flashMode = CCCameraFlashModeOff;
    }
    else {
        self.flashMode = CCCameraFlashModeTorch;
    }
    
    // TODO: Update the flash mode for the camera
}

// This method captures a photo from the camera
-(void)takePicture {
    
    /*
     Retrieve the video preview layer's video orientation on the main queue before
     entering the session queue. We do this to ensure UI elements are accessed on
     the main thread and session configuration is done on the session queue.
     */
    CCCameraView *latestView = [CCCameraManager getLatestView];
    AVCaptureVideoOrientation videoPreviewLayerVideoOrientation = latestView.previewView.previewLayer.connection.videoOrientation;
    
    dispatch_async(self.captureSessionQueue, ^{
        
        // Update the photo output's connection to match the video orientation of the video preview layer.
        AVCaptureConnection *photoOutputConnection = [self.photoOutput connectionWithMediaType:AVMediaTypeVideo];
        photoOutputConnection.videoOrientation = videoPreviewLayerVideoOrientation;
        
        // Capture a JPEG photo with flash set to auto and high resolution photo enabled.
        AVCapturePhotoSettings *photoSettings = [AVCapturePhotoSettings photoSettings];
        
        photoSettings.flashMode = AVCaptureFlashModeAuto;
        photoSettings.highResolutionPhotoEnabled = YES;
        if ( photoSettings.availablePreviewPhotoPixelFormatTypes.count > 0 ) {
            photoSettings.previewPhotoFormat = @{ (NSString *)kCVPixelBufferPixelFormatTypeKey : photoSettings.availablePreviewPhotoPixelFormatTypes.firstObject };
        }
        
        /*// Use a separate object for the photo capture delegate to isolate each capture life cycle.
        AVCamPhotoCaptureDelegate *photoCaptureDelegate = [[AVCamPhotoCaptureDelegate alloc] initWithRequestedPhotoSettings:photoSettings willCapturePhotoAnimation:^{
            dispatch_async( dispatch_get_main_queue(), ^{
                self.previewView.videoPreviewLayer.opacity = 0.0;
                [UIView animateWithDuration:0.25 animations:^{
                    self.previewView.videoPreviewLayer.opacity = 1.0;
                }];
            } );
        } capturingLivePhoto:^( BOOL capturing ) {
            
            dispatch_async( self.sessionQueue, ^{
                if ( capturing ) {
                    self.inProgressLivePhotoCapturesCount++;
                }
                else {
                    self.inProgressLivePhotoCapturesCount--;
                }
                
                NSInteger inProgressLivePhotoCapturesCount = self.inProgressLivePhotoCapturesCount;
                dispatch_async( dispatch_get_main_queue(), ^{
                    if ( inProgressLivePhotoCapturesCount > 0 ) {
                        self.capturingLivePhotoLabel.hidden = NO;
                    }
                    else if ( inProgressLivePhotoCapturesCount == 0 ) {
                        self.capturingLivePhotoLabel.hidden = YES;
                    }
                    else {
                        NSLog( @"Error: In progress live photo capture count is less than 0" );
                    }
                } );
            } );
        } completed:^( AVCamPhotoCaptureDelegate *photoCaptureDelegate ) {
            // When the capture is complete, remove a reference to the photo capture delegate so it can be deallocated.
            dispatch_async( self.sessionQueue, ^{
                self.inProgressPhotoCaptureDelegates[@(photoCaptureDelegate.requestedPhotoSettings.uniqueID)] = nil;
            } );
        }];
        
        
        self.inProgressPhotoCaptureDelegates[@(photoCaptureDelegate.requestedPhotoSettings.uniqueID)] = photoCaptureDelegate;*/
        [self.photoOutput capturePhotoWithSettings:photoSettings delegate:self];
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

#pragma mark AVCapturePhotoCaptureDelegate methods

-(void)captureOutput:(AVCapturePhotoOutput *)captureOutput didFinishProcessingPhotoSampleBuffer:(CMSampleBufferRef)photoSampleBuffer previewPhotoSampleBuffer:(CMSampleBufferRef)previewPhotoSampleBuffer resolvedSettings:(AVCaptureResolvedPhotoSettings *)resolvedSettings bracketSettings:(AVCaptureBracketedStillImageSettings *)bracketSettings error:(NSError *)error {
    
    NSLog(@"didFinishProcessingPhotoSampleBuffer was called");
    
    // If there was an error, then simply return
    if (error != nil) {
        NSLog( @"Error capturing photo: %@", error );
        return;
    }
    
    // Otherwise, save the photo data
    self.photoData = [AVCapturePhotoOutput JPEGPhotoDataRepresentationForJPEGSampleBuffer:photoSampleBuffer previewPhotoSampleBuffer:previewPhotoSampleBuffer];
}

-(void)captureOutput:(AVCapturePhotoOutput *)captureOutput didFinishCaptureForResolvedSettings:(AVCaptureResolvedPhotoSettings *)resolvedSettings error:(NSError *)error {
    
    if (error != nil) {
        NSLog(@"A photo wasn't captured!");
        return;
    }
    else {
        NSLog(@"A photo was captured!");
    }
    
    // If the photoData wasn't nil, then save the image to the file system
    if (self.photoData != nil) {
        
        // Create a UIImage from the photoData and rotate it to the proper orientation
        UIImage *originalImage = [UIImage imageWithData:self.photoData scale:1.0f];
        UIImage *rotatedImage = [originalImage rotateUIImage];
        
        // TODO: Need to crop the image?
        
        // Get the data for the rotated image
        NSData *jpeg = UIImageJPEGRepresentation(rotatedImage, 0.6f);
        
        // Write the rotated image data to the file system
        NSString *filePath = [StorageUtility writeDataToFile:jpeg];
        
        //
        CCCameraView *thisCameraView = [CCCameraManager getLatestView];
        [thisCameraView doPhotoTaken:filePath :1080 :1920];
    }
    
    
    /*if ( error != nil ) {
        NSLog( @"Error capturing photo: %@", error );
        [self didFinish];
        return;
    }
    
    if ( self.photoData == nil ) {
        NSLog( @"No photo data resource" );
        [self didFinish];
        return;
    }
    
    [PHPhotoLibrary requestAuthorization:^( PHAuthorizationStatus status ) {
        if ( status == PHAuthorizationStatusAuthorized ) {
            [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                PHAssetCreationRequest *creationRequest = [PHAssetCreationRequest creationRequestForAsset];
                [creationRequest addResourceWithType:PHAssetResourceTypePhoto data:self.photoData options:nil];
                
                if ( self.livePhotoCompanionMovieURL ) {
                    PHAssetResourceCreationOptions *livePhotoCompanionMovieResourceOptions = [[PHAssetResourceCreationOptions alloc] init];
                    livePhotoCompanionMovieResourceOptions.shouldMoveFile = YES;
                    [creationRequest addResourceWithType:PHAssetResourceTypePairedVideo fileURL:self.livePhotoCompanionMovieURL options:livePhotoCompanionMovieResourceOptions];
                }
            } completionHandler:^( BOOL success, NSError * _Nullable error ) {
                if ( ! success ) {
                    NSLog( @"Error occurred while saving photo to photo library: %@", error );
                }
                
                [self didFinish];
            }];
        }
        else {
            NSLog( @"Not authorized to save photo" );
            [self didFinish];
        }
    }];*/
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
