//
//  CCCameraLayout.m
//  newcam
//
//  Created by Matt Boyd on 5/20/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "CCCameraLayout.h"

@implementation CCCameraLayout

@synthesize camera;
@synthesize motionManager;
@synthesize lastOrientation;
@synthesize topSubview;
@synthesize closeButton;
@synthesize placeLabelTopConstraint;
@synthesize placeLabelWidthConstraint;
@synthesize placeLabelRightConstraint;
@synthesize placeLabelView;
@synthesize placeName;
@synthesize scannerMessageLabel;
@synthesize scannerTopConstraint;
@synthesize scannerCenterConstraint;
@synthesize toggleResolution;
@synthesize bottomSubview;
@synthesize toggleCamera;
@synthesize captureButton;
@synthesize toggleFlash;
@synthesize fastCamSubview;
@synthesize fastCamIndicator;
@synthesize fastCamLabel;
@synthesize cameraSubview;
@synthesize cameraIndicator;
@synthesize cameraLabel;
@synthesize scannerSubview;
@synthesize scannerIndicator;
@synthesize scannerLabel;
@synthesize auxModeSubview;
@synthesize auxModeIndicator;
@synthesize auxModeLabel;
@synthesize resolutionTopConstraint;
@synthesize resolutionWidthConstraint;
@synthesize resolutionRightConstraint;
@synthesize resolutionSubview;
@synthesize normalButton;
@synthesize highButton;
@synthesize superButton;
@synthesize resolutionLabel;
@synthesize resolutionDismissButton;
@synthesize resolutionLayoutVisible;
@synthesize buttonViewTopConstraint;
@synthesize buttonViewHeightConstraint;
@synthesize buttonViewRightConstraint;
@synthesize focusIndicatorView;
@synthesize focusIndicatorTimer;
@synthesize focusIndicatorTopConstraint;
@synthesize focusIndicatorLeftConstraint;
@synthesize loadingView;
@synthesize screenFlashView;
@synthesize imageProcessorView;
@synthesize pinchRecognizer;
@synthesize CCCameraBundle;

-(id)initWithCoder:(NSCoder *)aDecoder {

    self = [super initWithCoder:aDecoder];
    if (self) {

        // Load the nib for this view
        self.CCCameraBundle = [NSBundle bundleWithURL:[[NSBundle mainBundle] URLForResource:@"CCCameraResources" withExtension:@"bundle"]];

        if (iPad) {
            [self.CCCameraBundle loadNibNamed:@"CCCameraLayout_iPad" owner:self options:nil];
        }
        else {
            [self.CCCameraBundle loadNibNamed:@"CCCameraLayout" owner:self options:nil];
        }

        [self addSubview:self.view];

        // Setup the view
        [self initView];

        NSLog(@"A CCCameraLayout was just created in initWithCoder!");
    }

    return self;
}

-(id)initWithFrame:(CGRect)frame {

    self = [super initWithFrame:frame];
    if (self) {

        // Load the nib for this view
        self.CCCameraBundle = [NSBundle bundleWithURL:[[NSBundle mainBundle] URLForResource:@"CCCameraResources" withExtension:@"bundle"]];

        if (iPad) {
            [self.CCCameraBundle loadNibNamed:@"CCCameraLayout_iPad" owner:self options:nil];
        }
        else {
            [self.CCCameraBundle loadNibNamed:@"CCCameraLayout" owner:self options:nil];
        }

        [self addSubview:self.view];

        // Setup the view
        [self initView];

        NSLog(@"A CCCameraLayout was just created in initWithFrame!");
    }
    return self;
}

// Override layoutSubviews to get the initial interface sized correctly
-(void)layoutSubviews {
    [super layoutSubviews];
    
    NSLog(@"bottomSubview: layoutSubviews was called");
    
    if (self.motionManager == nil) {
        // Initialize the motionManager
        [self initializeMotionManager];
    }

    // Set the layout constraints for the default orientation
    //[self setLayoutConstraintsForOrientation:self.lastOrientation];
}


// This method does some initial setup of the view
-(void)initView {

    // Make sure all the buttons are enabled by default
    [self enableButtons];

    // Initialize the motionManager
    //[self initializeMotionManager];

    // Set the multipleTouchEnabled property to enable multiple touches
    self.multipleTouchEnabled = YES;

    // Add the pinchRecognizer
    [self addGestureRecognizer:self.pinchRecognizer];

    // Hide the loadingView
    [self hideLoadingView];

    //    // Register to receive a notification when the CCCameraModule is made active
    //    [[NSNotificationCenter defaultCenter] removeObserver:self];
    //    [[NSNotificationCenter defaultCenter] addObserver:self
    //                                             selector:@selector(onSetActive:)
    //                                                 name:@"CCCameraModuleActiveNotification"
    //                                               object:nil];
    //
    //    // Register to receive a notification when the CCCameraModule is made inactive
    //    [[NSNotificationCenter defaultCenter] addObserver:self
    //                                             selector:@selector(onSetInactive:)
    //                                                 name:@"CCCameraModuleInactiveNotification"
    //                                               object:nil];
}

// This method responds to the CCCameraModuleActiveNotification
-(void)onSetActive:(NSNotification *)notification {

    // Initialize the motionManager
    //[self initializeMotionManager];
}

// This method responds to the CCCameraModuleInactiveNotification
-(void)onSetInactive:(NSNotification *)notification {

    // Stop the motionManager updates
    //[self.motionManager stopAccelerometerUpdates];
}

#pragma mark CMMotionManager methods

// This method initializes the motionManager object
-(void)initializeMotionManager {

    self.motionManager = [[CMMotionManager alloc] init];
    self.motionManager.accelerometerUpdateInterval = .2;

    // If the accelerometer is available, then start getting updates.
    self.lastOrientation = UIDeviceOrientationPortrait;
    if ([self.motionManager isAccelerometerAvailable]) {
        [self.motionManager startAccelerometerUpdatesToQueue:[NSOperationQueue currentQueue]
                                                 withHandler:^(CMAccelerometerData  *accelerometerData, NSError *error) {
                                                     if (!error) {

                                                         // Handle the acceleration data
                                                         [self outputAccelertionData:accelerometerData.acceleration];
                                                     }
                                                     else{
                                                         NSLog(@"%@", error);
                                                     }
                                                 }];

    }

    // If the accelerometer isn't available, then set a default value for the lastOrientation
    else {
        self.lastOrientation = UIDeviceOrientationPortrait;
    }
}

// This method processes acceleration data from the motionManager
-(void)outputAccelertionData:(CMAcceleration)acceleration {

    UIDeviceOrientation orientationNew;

    // Determine the device orientation based on the accelerometer data
    if (acceleration.x >= 0.75) {
        orientationNew = UIDeviceOrientationLandscapeRight;
    }
    else if (acceleration.x <= -0.75) {
        orientationNew = UIDeviceOrientationLandscapeLeft;
    }
    else if (acceleration.y <= -0.75) {
        orientationNew = UIDeviceOrientationPortrait;
    }
    else if (acceleration.y >= 0.75) {
        orientationNew = UIDeviceOrientationPortraitUpsideDown;
    }
    else {
        // Consider same as last time
        return;
    }

    // Update the UI if the device orientation has changed
    if (orientationNew != self.lastOrientation) {
        self.lastOrientation = orientationNew;
        [self onOrientationChanged:self.lastOrientation];
    }
}

// This buttons sets the constraints on the UI elements after the device orientation has changed
-(void)onOrientationChanged:(UIDeviceOrientation)orientation {

    // Change the rotation of the interface elements to match the given orientation

    // Hide the resolution layout if it's showing
    double animationDelay = 0.0;
    if (self.resolutionLayoutVisible) {

        // Set the animation delay so that the button rotations don't start until the resolutionSubview is hidden
        animationDelay = 0.2;

        // Since the device has been rotated, use the animation based on the previous orientation to hide the resolution layout
        if (UIDeviceOrientationIsLandscape(self.lastOrientation)) {
            [self hideResolutionLayoutForOrientation:UIDeviceOrientationPortrait];
        }
        else {
            [self hideResolutionLayoutForOrientation:UIDeviceOrientationLandscapeRight];
        }
    }

    // Set the orientations for the UI elements.  If the resolutionSubview was visible, then wait for the animation that hides it to complete before rotating the UI elements
    dispatch_time_t delayTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(animationDelay * NSEC_PER_SEC));
    dispatch_after(delayTime, dispatch_get_main_queue(), ^(void){

        double rotationValue;

        // Landscape orientation
        if (UIDeviceOrientationIsLandscape(self.lastOrientation)) {

            // Set the rotation value to rotate the icons from portrait to landscape
            if (self.lastOrientation == UIDeviceOrientationLandscapeLeft) {
                rotationValue = M_PI/2;
            }
            else {
                rotationValue = -M_PI/2;
            }

            // Set the images for the toggleResolution and closeButton
            [self.toggleResolution setImage:[self getCCImageNamed:@"icon-close-new.png"] forState:UIControlStateNormal];
            [self setResolutionImage:[self.camera getResolutionModeString]];
        }

        // Portrait orientation
        else  {

            // Set the rotation value to rotate the icons from landscape to portrait
            //rotationValue = -M_PI/2;
            rotationValue = 0;

            // Set the images for the toggleResolution and closeButton
            [self setResolutionImage:[self.camera getResolutionModeString]];
            [self.closeButton setImage:[self getCCImageNamed:@"icon-close-new.png"] forState:UIControlStateNormal];
        }

        // Set the layout constraints for this orientation
        [self setLayoutConstraintsForOrientation:self.lastOrientation];

        // Rotate the placeLabelView, the scannerMessageLabel, the resolutionSubview, the loadingView, and the bottomSubview for iPads
        if (iPad) {
            [self animateRotation:self.placeLabelView angle:rotationValue duration:0.0];
            [self animateRotation:self.scannerMessageLabel angle:rotationValue duration:0.0];
            [self animateRotation:self.resolutionSubview angle:rotationValue duration:0.0];
            [self animateRotation:self.loadingView angle:rotationValue duration:0.0];
            [self animateRotation:self.bottomSubview angle:rotationValue duration:0.0];
        }

        // Rotate the buttons, the placeLabelView, the scannerMessageLabel, the loadingView, and the resolutionSubview for iPhones
        else {
            [self animateRotation:self.closeButton angle:rotationValue duration:0.2];
            [self animateRotation:self.placeLabelView angle:rotationValue duration:0.0];
            [self animateRotation:self.scannerMessageLabel angle:rotationValue duration:0.0];
            [self animateRotation:self.toggleResolution angle:rotationValue duration:0.2];
            [self animateRotation:self.toggleFlash angle:rotationValue duration:0.2];
            [self animateRotation:self.toggleCamera angle:rotationValue duration:0.2];
            [self animateRotation:self.loadingView angle:rotationValue duration:0.2];
            [self animateRotation:self.resolutionSubview angle:rotationValue duration:0.2];
        }
    });
}

// This method sets the values for all the variable layout constraints
-(void)setLayoutConstraintsForOrientation:(UIDeviceOrientation)orientation {

    // Landscape orientation
    if (UIDeviceOrientationIsLandscape(orientation)) {
        
        NSLog(@"setLayoutConstraints being called for landscape");

        // Set the constraints for the placeLabelView.  The width of the placeLabelView is a function of the width of the resolution and close buttons and their margins all of which are defined in CCCameraLayout.xib.
        int buttonWidth = 40;
        int buttonMargin = 10;
        if (iPad) {

            // On iPads the place label spans the entire top of the screen, so set buttonWidth and buttonMargin to 0
            buttonWidth = 0;
            buttonMargin = 0;
        }
        int placeLabelHeight = self.placeLabelView.frame.size.height;
        int placeLabelWidth = self.frame.size.height - 2*buttonWidth - 2*buttonMargin;
        self.placeLabelTopConstraint.constant = (self.frame.size.height - placeLabelHeight)/2.0;
        self.placeLabelWidthConstraint.constant = placeLabelWidth;
        if (orientation == UIDeviceOrientationLandscapeRight) {
            self.placeLabelRightConstraint.constant = (self.frame.size.width - buttonWidth - buttonMargin - 10 - placeLabelHeight/2.0 - placeLabelWidth/2.0);
        }
        else {
            self.placeLabelRightConstraint.constant = -(buttonWidth + buttonMargin - 10 - placeLabelHeight/2.0 + placeLabelWidth/2.0);
        }

        // Set the constraints for the scannerMessageLabel
        int topSubviewHeight = self.topSubview.frame.size.height;
        int scannerHeight = self.scannerMessageLabel.frame.size.height;
        self.scannerTopConstraint.constant = self.frame.size.height/2.0 - topSubviewHeight - scannerHeight/2.0;
        if (orientation == UIDeviceOrientationLandscapeRight) {
            self.scannerCenterConstraint.constant = (topSubviewHeight + scannerHeight)/2.0;
        }
        else {
            self.scannerCenterConstraint.constant = -(topSubviewHeight + scannerHeight)/2.0;
        }


        // Set the constraints for the resolutionLayout
        self.resolutionTopConstraint.constant = (self.frame.size.height - RESOLUTION_ANIMATION_DIST)/2.0;;
        self.resolutionWidthConstraint.constant = self.frame.size.height;
        if (orientation == UIDeviceOrientationLandscapeRight) {
            self.resolutionRightConstraint.constant = self.frame.size.width - (self.frame.size.height - RESOLUTION_ANIMATION_DIST)/2.0;
        }
        else {
            self.resolutionRightConstraint.constant = -(self.frame.size.height + RESOLUTION_ANIMATION_DIST)/2.0;
        }

        // Set the constraints for the bottomSubview on iPads
        if (iPad) {
            if (orientation == UIDeviceOrientationLandscapeRight) {
                self.buttonViewTopConstraint.constant = -(self.frame.size.width/2.0 - self.bottomSubview.frame.size.width/2.0);
            }
            else {
                self.buttonViewTopConstraint.constant = self.frame.size.height - self.frame.size.width/2.0 - self.bottomSubview.frame.size.width/2.0;
                
                CGFloat bottomConstant = self.frame.size.height - self.frame.size.width/2.0 - self.bottomSubview.frame.size.width/2.0;
                
                NSLog([NSString stringWithFormat:@"bottomSubview: self.frame.width = %0.f", self.frame.size.width]);
                NSLog([NSString stringWithFormat:@"bottomSubview: self.frame.height = %0.f", self.frame.size.height]);
                NSLog([NSString stringWithFormat:@"bottomSubview width = %0.f", self.bottomSubview.frame.size.width]);
                NSLog([NSString stringWithFormat:@"bottomSubview height = %0.f", self.bottomSubview.frame.size.height]);
                NSLog([NSString stringWithFormat:@"Setting the bottomConstant for bottomSubview as %0.f", bottomConstant]);
                
            }
            self.buttonViewHeightConstraint.constant = self.frame.size.width;
            self.buttonViewRightConstraint.constant = self.frame.size.width/2.0 - self.bottomSubview.frame.size.width/2.0;
        }
    }

    // Portrait orientation
    else {

        // Set the constraints for the placeLabelView.  The width of the placeLabelView is a function of the width of the resolution and close buttons and their margins all of which are defined in CCCameraLayout.xib.
        int buttonWidth = 40;
        int buttonMargin = 10;
        if (iPad) {

            // On iPads the place label spans the entire top of the screen, so set buttonWidth and buttonMargin to 0
            buttonWidth = 0;
            buttonMargin = 0;
        }
        self.placeLabelTopConstraint.constant = 10;
        self.placeLabelWidthConstraint.constant = self.frame.size.width - 2*buttonWidth - 2*buttonMargin;
        self.placeLabelRightConstraint.constant = 0;

        // Set the constraints for the scannerMessageLabel
        self.scannerTopConstraint.constant = 0.0;
        self.scannerCenterConstraint.constant = 0.0;

        // Set the constraints for the resolutionLayout
        self.resolutionTopConstraint.constant = -RESOLUTION_ANIMATION_DIST;
        self.resolutionWidthConstraint.constant = self.frame.size.width;
        self.resolutionRightConstraint.constant = 0;

        // Set the constraints for the bottomSubview on iPads
        if (iPad) {
            self.buttonViewTopConstraint.constant = 0;
            self.buttonViewHeightConstraint.constant = self.frame.size.height;
            self.buttonViewRightConstraint.constant = 0;
        }
    }
}

#pragma mark Button click actions

-(IBAction)closeButtonClick:(id)sender {

    // The action for the close button depends on the orientation of the device because the close button and resolution button are
    // reversed in portrait and landscape in the phone layout
    if (UIDeviceOrientationIsPortrait(self.lastOrientation) || iPad) {

        // This is the close button action
        // Dismiss the view
        [[CCCameraManager getLatestView] finishWithResult:@"close"];
    }
    else {

        // This is the resolution button action
        [self showResolutionLayout];
    }
}

-(IBAction)resolutionButtonClick:(id)sender {

    // Resolution dismiss button
    if (sender == self.resolutionDismissButton) {

        // Hide the resolution subview
        [self hideResolutionLayout];
    }

    // Resolution button
    else if (sender == self.toggleResolution) {

        // The action for the resolution button depends on the orientation of the device because the close button and resolution button are
        // reversed in portrait and landscape in the phone layout
        if (UIDeviceOrientationIsPortrait(self.lastOrientation) || iPad) {

            // This is the resolution button action
            [self showResolutionLayout];
        }
        else {

            // This is the close button action
            // Dismiss the view
            [[CCCameraManager getLatestView] finishWithResult:@"close"];
        }
    }
}

-(IBAction)normalButtonClick:(id)sender {

    // Set the resolution image and set the resolution of the camera
    [self setResolutionImage:@"normal"];
    [self.camera setResolution:@"normal"];
}

-(IBAction)highButtonClick:(id)sender {

    // Set the resolution image and set the resolution of the camera
    [self setResolutionImage:@"high"];
    [self.camera setResolution:@"high"];
}

-(IBAction)superButtonClick:(id)sender {

    // Set the resolution image and set the resolution of the camera
    [self setResolutionImage:@"super"];
    [self.camera setResolution:@"super"];
}

-(IBAction)flashButtonClick:(id)sender {

    // Set the flash mode for the camera and set the flash image
//    [camera toggleFlash];
//    [self setFlashModeImage:[camera getFlashModeString]];
}

-(IBAction)captureButtonClick:(id)sender {

    if (self.camera != nil) {
        [self.camera takePicture];
    }
}

-(IBAction)cameraButtonClick:(id)sender {

    // Set the reference to the camera if necessary
    if (self.camera == nil) {
        self.camera = [[CCCameraManager getLatestView] camera];
    }

    // Toggle the camera between rear- and forward-facing
    [self.camera toggleCamera];
}

-(IBAction)fastCamSubviewClick:(id)sender {

    // Set the camera mode
//    [self setCameraMode:@"fastcam"];
}

-(IBAction)cameraSubviewClick:(id)sender {

    // Set the camera mode
//    [self setCameraMode:@"camera"];
}

-(IBAction)scannerSubviewClick:(id)sender {

    // Set the camera mode
//    [self setCameraMode:@"scanner"];
}

#pragma mark CCCameraLayoutDelegate methods

// This method sets the reference to the CCCamera object
-(void)setCameraObject:(CCCamera *)_camera; {
    self.camera = _camera;

    // Set the visibility of the flash button
//    [self setFlashButtonVisibility];
    
    // Set the flash mode image
    //    [self setFlashModeImage:[self.camera getFlashModeString]];
    
    // Set the resolution mode image
//    [self setResolutionImage:[self.camera getResolutionModeString]];
    
    // Set the visibility of the camera button
//    [self setCameraButtonVisibility];
    
    // Set the camera layout state
//    [self setCameraMode:[self.camera getCameraModeString]];
    
}

// This method sets the place name label
-(void)setPlaceNameLabel:(NSString *)name {
    if (self.placeName != nil) {
        [self.placeName setText:name];
    }
}

// This method sets the visibility of the flash button
-(void)setFlashButtonVisibility {

    // If this isn't being called from the main thread, switch it to the main thread
    if ([NSThread currentThread] != [NSThread mainThread]) {
        [self performSelector:@selector(setFlashButtonVisibility) onThread:[NSThread mainThread] withObject:nil waitUntilDone:NO];
        return;
    }

    // Hide the flash button if the selected camera doesn't support flash
    if ([self.camera hasFlash]) {
        self.toggleFlash.alpha = 1.0;
        [self.toggleFlash setHidden:NO];
    }
    else {
        [self.toggleFlash setHidden:YES];
    }
}
/*
 // This method sets the flash mode and updates the flash button appropriately
 -(void)setFlashModeImage:(NSString *)flashMode {
 
 if ([flashMode isEqualToString:@"auto"]) {
 [toggleFlash setImage:[self getCCImageNamed:@"flashlight-off.png"] forState:UIControlStateNormal];
 }
 else if ([flashMode isEqualToString:@"on"]) {
 [toggleFlash setImage:[self getCCImageNamed:@"flashlight-on.png"] forState:UIControlStateNormal];
 }
 else if ([flashMode isEqualToString:@"torch"]) {
 [toggleFlash setImage:[self getCCImageNamed:@"flashlight-on.png"] forState:UIControlStateNormal];
 }
 else {
 [toggleFlash setImage:[self getCCImageNamed:@"flashlight-off.png"] forState:UIControlStateNormal];
 }
 
 // Persist the flash mode
 [self.camera persistFlashMode:flashMode];
 }
 */

// This method sets the appropriate images resources for all the buttons in the resolution subview and records the resolution mode.
-(void)setResolutionImage:(NSString *)resolutionMode {

    // Determine whether the toggleResolution button or the closeButton is currently controlling the resolution selection based on the
    // device orientation.  If this device is an iPad, then the buttons are never reversed.
    UIButton *resolutionButton = self.toggleResolution;
    if (UIDeviceOrientationIsLandscape(self.lastOrientation) && !(iPad)) {
        resolutionButton = self.closeButton;
    }

    if ([resolutionMode isEqualToString:@"super"]) {

        // Set the button images
        [resolutionButton setImage:[self getCCImageNamed:@"super-fine-size-icon.png"] forState:UIControlStateNormal];
        [self.normalButton setImage:[self getCCImageNamed:@"normal-icon.png"] forState:UIControlStateNormal];
        [self.highButton setImage:[self getCCImageNamed:@"high-icon.png"] forState:UIControlStateNormal];
        [self.superButton setImage:[self getCCImageNamed:@"super-fine-on-icon.png"] forState:UIControlStateNormal];

        // Set the resolution text label
        [self.resolutionLabel setText:@"Best for capturing great details.  Largest file size.  Uses the most data."];
    }
    else if ([resolutionMode isEqualToString:@"high"]) {

        // Set the button images
        [resolutionButton setImage:[self getCCImageNamed:@"high-size-icon.png"] forState:UIControlStateNormal];
        [self.normalButton setImage:[self getCCImageNamed:@"normal-icon.png"] forState:UIControlStateNormal];
        [self.highButton setImage:[self getCCImageNamed:@"high-on-icon.png"] forState:UIControlStateNormal];
        [self.superButton setImage:[self getCCImageNamed:@"super-fine-icon.png"] forState:UIControlStateNormal];

        // Set the resolution text label
        [self.resolutionLabel setText:@"Best for balancing image quality and file size.  Uses more data."];
    }
    else {

        // Set the button images
        [resolutionButton setImage:[self getCCImageNamed:@"normal-size-icon.png"] forState:UIControlStateNormal];
        [self.normalButton setImage:[self getCCImageNamed:@"normal-on-icon.png"] forState:UIControlStateNormal];
        [self.highButton setImage:[self getCCImageNamed:@"high-icon.png"] forState:UIControlStateNormal];
        [self.superButton setImage:[self getCCImageNamed:@"super-fine-icon.png"] forState:UIControlStateNormal];

        // Set the resolution text label
        [self.resolutionLabel setText:@"Best for everyday use.  Smallest file size.  Uses the least data."];
    }

    // Persist the resolution mode
    [self.camera persistResolutionMode:resolutionMode];
}

// This method sets the camera button visibility
-(void)setCameraButtonVisibility {

    // If this isn't being called from the main thread, switch it to the main thread
    if ([NSThread currentThread] != [NSThread mainThread]) {
        [self performSelector:@selector(setCameraButtonVisibility) onThread:[NSThread mainThread] withObject:nil waitUntilDone:NO];
        return;
    }

    // Show the camera button only if the device has both a rear- and forward-facing camera
    if ([self.camera hasRearCamera] && [self.camera hasFrontCamera]) {
        self.toggleCamera.alpha = 1.0;
        [self.toggleCamera setHidden:NO];
    }
    else {
        [self.toggleCamera setHidden:YES];
    }
}

// This method sets the camera mode layout features
-(void)setCameraMode:(NSString *)cameraMode {

    // FastCam mode
    if ([cameraMode isEqualToString:@"fastcam"]) {
        [scannerMessageLabel setHidden:YES];
        [captureButton setImage:[self getCCImageNamed:@"fast-cam-icon.png"] forState:UIControlStateNormal];
        [captureButton setHidden:NO];
        [fastCamIndicator setHidden:NO];
        [fastCamLabel setTextColor:sunYellowColor];
        [fastCamLabel setAlpha:1.0f];
        [cameraIndicator setHidden:YES];
        [cameraLabel setTextColor:[UIColor whiteColor]];
        [cameraLabel setAlpha:0.6f];
        [scannerIndicator setHidden:YES];
        [scannerLabel setTextColor:[UIColor whiteColor]];
        [scannerLabel setAlpha:0.6f];
        [auxModeIndicator setHidden:YES];
        [auxModeLabel setTextColor:[UIColor whiteColor]];
        [auxModeLabel setAlpha:0.6f];
    }

    // Camera mode
    else if ([cameraMode isEqualToString:@"camera"]) {
        [scannerMessageLabel setHidden:YES];
        [captureButton setImage:[self getCCImageNamed:@"icon-capture-new.png"] forState:UIControlStateNormal];
        [captureButton setHidden:NO];
        [fastCamIndicator setHidden:YES];
        [fastCamLabel setTextColor:[UIColor whiteColor]];
        [fastCamLabel setAlpha:0.6f];
        [cameraIndicator setHidden:NO];
        [cameraLabel setTextColor:sunYellowColor];
        [cameraLabel setAlpha:1.0f];
        [scannerIndicator setHidden:YES];
        [scannerLabel setTextColor:[UIColor whiteColor]];
        [scannerLabel setAlpha:0.6f];
        [auxModeIndicator setHidden:YES];
        [auxModeLabel setTextColor:[UIColor whiteColor]];
        [auxModeLabel setAlpha:0.6f];
    }

    // Scanner mode
    else if ([cameraMode isEqualToString:@"scanner"]) {
        [scannerMessageLabel setHidden:NO];
        [captureButton setImage:[self getCCImageNamed:@"icon-capture-new.png"] forState:UIControlStateNormal];
        [captureButton setHidden:YES];
        [fastCamIndicator setHidden:YES];
        [fastCamLabel setTextColor:[UIColor whiteColor]];
        [fastCamLabel setAlpha:0.6f];
        [cameraIndicator setHidden:YES];
        [cameraLabel setTextColor:[UIColor whiteColor]];
        [cameraLabel setAlpha:0.6f];
        [scannerIndicator setHidden:NO];
        [scannerLabel setTextColor:sunYellowColor];
        [scannerLabel setAlpha:1.0f];
        [auxModeIndicator setHidden:YES];
        [auxModeLabel setTextColor:[UIColor whiteColor]];
        [auxModeLabel setAlpha:0.6f];
    }

    // Aux mode
    else if ([cameraMode isEqualToString:@"aux"]) {
        [scannerMessageLabel setHidden:YES];
        [captureButton setImage:[self getCCImageNamed:@"icon-capture-new.png"] forState:UIControlStateNormal];
        [captureButton setHidden:NO];
        [fastCamIndicator setHidden:YES];
        [fastCamLabel setTextColor:[UIColor whiteColor]];
        [fastCamLabel setAlpha:0.6f];
        [cameraIndicator setHidden:YES];
        [cameraLabel setTextColor:[UIColor whiteColor]];
        [cameraLabel setAlpha:0.6f];
        [scannerIndicator setHidden:YES];
        [scannerLabel setTextColor:[UIColor whiteColor]];
        [scannerLabel setAlpha:0.6f];
        [auxModeIndicator setHidden:NO];
        [auxModeLabel setTextColor:sunYellowColor];
        [auxModeLabel setAlpha:1.0f];
    }

    // Persist the camera mode
    [self.camera persistCameraMode:cameraMode];
}

// This method shows the resolution layout
-(void)showResolutionLayout {

    // Show the resolution subview with an amination
    [UIView animateWithDuration:0.2
                     animations:^{

                         // Change the opacity of the resolutionSubview
                         resolutionSubview.alpha = 1.0;

                         // Set the appropriate constraints based on the device orientation
                         if (UIDeviceOrientationIsLandscape(self.lastOrientation)) {

                             // Change the resolutionRightConstraint
                             if (self.lastOrientation == UIDeviceOrientationLandscapeRight) {
                                 resolutionRightConstraint.constant = self.frame.size.width - (self.frame.size.height + RESOLUTION_ANIMATION_DIST)/2.0;
                             }
                             else {
                                 resolutionRightConstraint.constant = -(self.frame.size.height - RESOLUTION_ANIMATION_DIST)/2.0;
                             }

                             // Change the opactity of the top and bottom subviews and the buttons inside them
                             topSubview.alpha = 0.0;
                             placeLabelView.alpha = 0.0;
                             scannerMessageLabel.alpha = 0.0;
                             closeButton.alpha = 0.0;
                             toggleResolution.alpha = 0.0;
                             bottomSubview.alpha = 0.0;
                             captureButton.alpha = 0.0;
                             toggleFlash.alpha = 0.0;
                             toggleCamera.alpha = 0.0;
                         }
                         else {

                             // Change the resolutionTopConstraint
                             resolutionTopConstraint.constant = 0;

                             // Change the opactity of the top subview and the buttons inside it
                             topSubview.alpha = 0.0;
                             placeLabelView.alpha = 0.0;
                             scannerMessageLabel.alpha = 0.0;
                             closeButton.alpha = 0.0;
                             toggleResolution.alpha = 0.0;
                         }

                         [self.view layoutIfNeeded];
                     }
                     completion:^(BOOL finished) {

                         // Set the resolutionLayoutVisible flag
                         resolutionLayoutVisible = YES;
                     }];
}

// This method hides the resolution layout
-(void)hideResolutionLayout {

    // Hide the resolution subview with an amination using the current orientation
    [self hideResolutionLayoutForOrientation:self.lastOrientation];
}

// This method hides the resolution layout for the given device orientation
-(void)hideResolutionLayoutForOrientation:(UIDeviceOrientation)thisOrientation {

    // Hide the resolution subview with an amination
    [UIView animateWithDuration:0.2
                     animations:^{

                         // Change the opacity of the resolutionSubview
                         resolutionSubview.alpha = 0.0;

                         // Set the appropriate constraints based on the device orientation
                         if (UIDeviceOrientationIsLandscape(thisOrientation)) {

                             // Change the resolutionRightConstraint
                             if (thisOrientation == UIDeviceOrientationLandscapeRight) {
                                 resolutionRightConstraint.constant = self.frame.size.width - (self.frame.size.height - RESOLUTION_ANIMATION_DIST)/2.0;
                             }
                             else {
                                 resolutionRightConstraint.constant = -(self.frame.size.height + RESOLUTION_ANIMATION_DIST)/2.0;
                             }

                             // Change the opactity of the top and bottom subviews and the buttons inside them
                             topSubview.alpha = 1.0;
                             placeLabelView.alpha = 1.0;
                             scannerMessageLabel.alpha = 1.0;
                             closeButton.alpha = 1.0;
                             toggleResolution.alpha = 1.0;
                             bottomSubview.alpha = 1.0;
                             captureButton.alpha = 1.0;
                             [self setFlashButtonVisibility];
                             [self setCameraButtonVisibility];
                         }
                         else {

                             // Change the resolutionTopConstraint
                             resolutionTopConstraint.constant = -RESOLUTION_ANIMATION_DIST;

                             // Change the opactity of the top subview and the buttons inside it
                             topSubview.alpha = 1.0;
                             placeLabelView.alpha = 1.0;
                             scannerMessageLabel.alpha = 1.0;
                             closeButton.alpha = 1.0;
                             toggleResolution.alpha = 1.0;
                         }

                         [self.view layoutIfNeeded];
                     }
                     completion:^(BOOL finished) {

                         // Set the resolutionLayoutVisible flag
                         resolutionLayoutVisible = NO;
                     }];
}

// This method shows an auto focus indicator view at the given position while the camera is focusing and/or exposing
-(void)showAutoFocusIndicator:(CGPoint)touchPoint :(BOOL)setRepeating {

    // Get the width and height of the focusIndicatorView
    int width = self.focusIndicatorView.frame.size.width;
    int height = self.focusIndicatorView.frame.size.height;

    // Set the constraints for the focusIndicatorView
    self.focusIndicatorTopConstraint.constant = touchPoint.y - height/2;
    self.focusIndicatorLeftConstraint.constant = touchPoint.x - width/2;

    // Add an animation to the focusIndicatorView
    double animationIncrementTime = 0.03;
    self.focusIndicatorView.radius = 0.0;

    // Start the timer
    self.focusIndicatorTimer = [NSTimer scheduledTimerWithTimeInterval:animationIncrementTime target:self selector:@selector(incrementFocusIndicatorRadius:) userInfo:nil repeats:YES];

    // Show the focusIndicatorView
//    [self.focusIndicatorView setHidden:NO];
}

// This method hides the auto focus indicator view
-(void)hideAutoFocusIndicator {
    [self.focusIndicatorTimer invalidate];
    self.focusIndicatorTimer = nil;
//    [self.focusIndicatorView setHidden:YES];
}

// This method shows the loading view
-(void)showLoadingView {
    [self.loadingView setHidden:NO];
}

// This method hides the loading view
-(void)hideLoadingView {
    [self.loadingView setHidden:YES];
}

// This method shows the layout view
-(void)showCameraLayout {
    [[self topSubview] setHidden:NO];
    [[self bottomSubview] setHidden:NO];
    [[self focusIndicatorView] setHidden:NO];
    [[self toggleCamera] setHidden:NO];
    [[self toggleFlash] setHidden:NO];
    [[self captureButton] setHidden:NO];
}

// This method hides the layout view (but keeps the scanner edges preview)
-(void)hideCameraLayout {
    [[self topSubview] setHidden:YES];
    [[self bottomSubview] setHidden:YES];
    [[self focusIndicatorView] setHidden:YES];
    [[self toggleCamera] setHidden:YES];
    [[self toggleFlash] setHidden:YES];
    [[self captureButton] setHidden:YES];
    [[self scannerMessageLabel] setHidden:YES];
}

// This method enables all the buttons
-(void)enableButtons {
    [self setUserInteractionEnabled:YES];
}

// This method disables all the buttons
-(void)disableButtons {
    [self setUserInteractionEnabled:NO];
}

// This method returns the current orientation of the layout
-(UIDeviceOrientation)getCurrentOrientation {
    return self.lastOrientation;
}

// This method passes the necessary parameters to the layout object to initialize the imageProcessorView
-(void)initImageProcessor:(int)previewWidth :(int)previewHeight :(int)maxOutputDimension {

    // Set the image parameters for the imageProcessorView
    [self.imageProcessorView setImageParams:previewWidth :previewHeight :self.frame.size.width :self.frame.size.height :maxOutputDimension];
}

// This method passes an image to the image processor
-(BOOL)setPreviewBytes:(UIImage *)image :(BOOL)regenerateOutput {
    return [self.imageProcessorView setPreviewBytes:image :regenerateOutput];
}

// This method requests the image output from the image processor
-(UIImage *)getOutputImage {
    return [self.imageProcessorView getOutputImage];
}

// This method requests the image data from the image processor
-(NSData *)getOutputData {
    return [self.imageProcessorView getOutputData];
}

// This method returns a boolean describing whether or not the image processor status is DONE
-(BOOL)isDone {
    return [self.imageProcessorView isDone];
}

// This method returns the center of the perspectiveRect
-(CGPoint)getPerspectiveRectCenter {
    return [self.imageProcessorView getPerspectiveRectCenter];
}

// This method clears the visible preview from the image processor
-(void)clearVisiblePreview {
    [self.imageProcessorView clearVisiblePreview];
}

// This method updates the radius for the focusIndicatorView
-(void)incrementFocusIndicatorRadius:(NSTimer *)timer {

    // If the radius for the focusIndicatorView has reached 1.0, then stop the timer and hide the view again.
    if (self.focusIndicatorView.radius >= 1.0) {
        [self hideAutoFocusIndicator];
    }
    else {
        [self.focusIndicatorView incrementRadius];
    }
}

#pragma mark -
#pragma mark Touch event handling methods

-(IBAction)handleZoom:(UIPinchGestureRecognizer *)pinchGestureRecognizer {

    // Check if this zoom event has ended
    BOOL zoomEnded = NO;
    if ([pinchGestureRecognizer state] == UIGestureRecognizerStateEnded ||
        [pinchGestureRecognizer state] == UIGestureRecognizerStateCancelled ||
        [pinchGestureRecognizer state] == UIGestureRecognizerStateFailed) {
        zoomEnded = YES;
    }

    // Pass the scale of the pinch gesture to the camera
    [self.camera handleZoom:pinchGestureRecognizer.scale :zoomEnded];
}

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {

    // Dismiss the resolution layout if it's showing
    if (self.resolutionLayoutVisible) {
        [self hideResolutionLayout];
    }
}

-(void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event {

}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {

    // Pass the touch event to the camera object and let it handle it
    [self.camera handleTouchEvent:event];
}

#pragma mark Miscellaneous methods

// This method returns a UIImage with a resource from the CCCamera resource bundle
-(UIImage *)getCCImageNamed:(NSString *)imageName {

    return [UIImage imageNamed:imageName inBundle:self.CCCameraBundle compatibleWithTraitCollection:nil];
}

// This method animates the rotation of the given view around it's center point to the given angle (measured in radians) in the given amount of time
-(void)animateRotation:(UIView *)thisView angle:(double)angle duration:(double)duration {

    [UIView animateWithDuration:duration
                     animations:^{

                         // Get the current angle of the transform
                         double currentAngle = atan2(thisView.transform.b, thisView.transform.a);

                         // Rotate the view to the given angle
                         thisView.transform = CGAffineTransformRotate(thisView.transform, angle - currentAngle);

                     }
                     completion:^(BOOL finished) {
                     }];
}

@end
