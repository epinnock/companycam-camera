//
//  CCCameraLayout.h
//  newcam
//
//  Created by Matt Boyd on 5/20/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "CoreMotion/CoreMotion.h"
#import "ResizingSubview.h"
#import "CCCameraView.h"
#import "CCCameraManager.h"
#import "CCCameraLayoutDelegate.h"
#import "CCCamera.h"
#import "FocusIndicatorView.h"
#import "DocScanOpenCV.h"

// Define some constant colors
#define sunYellowColor [UIColor colorWithRed:253.0/255.0 green:216.0/255.0 blue:53.0/255.0 alpha:1.0]

// This is the animation distance for the resolution layout in dp
#define RESOLUTION_ANIMATION_DIST 150

// Set a flag to define if this device is an iPad
#define iPad UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad

@interface CCCameraLayout : ResizingSubview <CCCameraLayoutDelegate> {
    
    // The CCCamera object maintains a reference to the camera and implements the necessary camera API methods
    CCCamera *camera;
    
    // The motionManager is used to update the UI elements after major device orientation changes
    CMMotionManager *motionManager;
    UIDeviceOrientation lastOrientation;
  
    // The top subview contains the place labels, close button, and resolution button
    IBOutlet UIView *topSubview;
    IBOutlet UIButton *closeButton;
    IBOutlet NSLayoutConstraint *placeLabelTopConstraint;
    IBOutlet NSLayoutConstraint *placeLabelWidthConstraint;
    IBOutlet NSLayoutConstraint *placeLabelRightConstraint;
    IBOutlet UIView *placeLabelView;
    IBOutlet UILabel *placeName;
    IBOutlet UILabel *scannerMessageLabel;
    IBOutlet NSLayoutConstraint *scannerTopConstraint;
    IBOutlet NSLayoutConstraint *scannerCenterConstraint;
    IBOutlet UIButton *toggleResolution;
    
    // The bottom subview contains the camera buttons and camera mode labels
    IBOutlet UIView *bottomSubview;
    
    // The toggleCamera button allows the user to switch between rear- and forward-facing cameras
    IBOutlet UIButton *toggleCamera;
    
    // The captureButton allows the user to capture a photo
    IBOutlet UIButton *captureButton;
    
    // The toggleFlash button allows the user to turn the flash on or off
    IBOutlet UIButton *toggleFlash;
    
    // These views and text labels are for the camera options labels
    IBOutlet UIView *fastCamSubview;
    IBOutlet UIImageView *fastCamIndicator;
    IBOutlet UILabel *fastCamLabel;
    IBOutlet UIView *cameraSubview;
    IBOutlet UIImageView *cameraIndicator;
    IBOutlet UILabel *cameraLabel;
    IBOutlet UIView *scannerSubview;
    IBOutlet UIImageView *scannerIndicator;
    IBOutlet UILabel *scannerLabel;
    IBOutlet UIView *auxModeSubview;
    IBOutlet UIImageView *auxModeIndicator;
    IBOutlet UILabel *auxModeLabel;
    
    // These views and text labels are for the resolution selection layout
    IBOutlet NSLayoutConstraint *resolutionTopConstraint;
    IBOutlet NSLayoutConstraint *resolutionWidthConstraint;
    IBOutlet NSLayoutConstraint *resolutionRightConstraint;
    IBOutlet UIView *resolutionSubview;
    IBOutlet UIButton *normalButton;
    IBOutlet UIButton *highButton;
    IBOutlet UIButton *superButton;
    IBOutlet UILabel *resolutionLabel;
    IBOutlet UIButton *resolutionDismissButton;
    BOOL resolutionLayoutVisible;
    
    // These constraints are for the button layout on iPads
    IBOutlet NSLayoutConstraint *buttonViewTopConstraint;
    IBOutlet NSLayoutConstraint *buttonViewHeightConstraint;
    IBOutlet NSLayoutConstraint *buttonViewRightConstraint;
    
    // The focusIndicatorView is used to indicate that the camera is in the process of focusing
    IBOutlet FocusIndicatorView *focusIndicatorView;
    NSTimer *focusIndicatorTimer;
    IBOutlet NSLayoutConstraint *focusIndicatorTopConstraint;
    IBOutlet NSLayoutConstraint *focusIndicatorLeftConstraint;
    
    // The loading view is used to present feedback to the user that a photo was taken
    IBOutlet UIView *loadingView;
    
    // The screenFlashView is used to provide a flash effect when the user snaps a photo
    IBOutlet UIView *screenFlashView;
    
    // The imageProcessorView is used to display image processing feedback while in scanner mode
    IBOutlet DocScanOpenCV *imageProcessorView;
    
    // The pinch gesture recognizer is used to handle the pinch to zoom action
    IBOutlet UIPinchGestureRecognizer *pinchRecognizer;
    
    // This is a reference to the resource bundle for this project
    NSBundle *CCCameraBundle;
}

@property (nonatomic, retain) CCCamera *camera;
@property (nonatomic, retain) CMMotionManager *motionManager;
@property (nonatomic, assign) UIDeviceOrientation lastOrientation;
@property (nonatomic, retain) IBOutlet UIView *topSubview;
@property (nonatomic, retain) IBOutlet UIButton *closeButton;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *placeLabelTopConstraint;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *placeLabelWidthConstraint;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *placeLabelRightConstraint;
@property (nonatomic, retain) IBOutlet UIView *placeLabelView;
@property (nonatomic, retain) IBOutlet UILabel *placeName;
@property (nonatomic, retain) IBOutlet UILabel *scannerMessageLabel;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *scannerTopConstraint;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *scannerCenterConstraint;
@property (nonatomic, retain) IBOutlet UIButton *toggleResolution;
@property (nonatomic, retain) IBOutlet UIView *bottomSubview;
@property (nonatomic, retain) IBOutlet UIButton *toggleCamera;
@property (nonatomic, retain) IBOutlet UIButton *captureButton;
@property (nonatomic, retain) IBOutlet UIButton *toggleFlash;
@property (nonatomic, retain) IBOutlet UIView *fastCamSubview;
@property (nonatomic, retain) IBOutlet UIImageView *fastCamIndicator;
@property (nonatomic, retain) IBOutlet UILabel *fastCamLabel;
@property (nonatomic, retain) IBOutlet UIView *cameraSubview;
@property (nonatomic, retain) IBOutlet UIImageView *cameraIndicator;
@property (nonatomic, retain) IBOutlet UILabel *cameraLabel;
@property (nonatomic, retain) IBOutlet UIView *scannerSubview;
@property (nonatomic, retain) IBOutlet UIImageView *scannerIndicator;
@property (nonatomic, retain) IBOutlet UILabel *scannerLabel;
@property (nonatomic, retain) IBOutlet UIView *auxModeSubview;
@property (nonatomic, retain) IBOutlet UIImageView *auxModeIndicator;
@property (nonatomic, retain) IBOutlet UILabel *auxModeLabel;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *resolutionTopConstraint;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *resolutionWidthConstraint;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *resolutionRightConstraint;
@property (nonatomic, retain) IBOutlet UIView *resolutionSubview;
@property (nonatomic, retain) IBOutlet UIButton *normalButton;
@property (nonatomic, retain) IBOutlet UIButton *highButton;
@property (nonatomic, retain) IBOutlet UIButton *superButton;
@property (nonatomic, retain) IBOutlet UILabel *resolutionLabel;
@property (nonatomic, retain) IBOutlet UIButton *resolutionDismissButton;
@property (nonatomic, assign) BOOL resolutionLayoutVisible;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *buttonViewTopConstraint;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *buttonViewHeightConstraint;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *buttonViewRightConstraint;
@property (nonatomic, retain) IBOutlet FocusIndicatorView *focusIndicatorView;
@property (nonatomic, retain) NSTimer *focusIndicatorTimer;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *focusIndicatorTopConstraint;
@property (nonatomic, retain) IBOutlet NSLayoutConstraint *focusIndicatorLeftConstraint;
@property (nonatomic, retain) IBOutlet UIView *loadingView;
@property (nonatomic, retain) IBOutlet UIView *screenFlashView;
@property (nonatomic, retain) IBOutlet DocScanOpenCV *imageProcessorView;
@property (nonatomic, retain) IBOutlet UIPinchGestureRecognizer *pinchRecognizer;
@property (nonatomic, retain) NSBundle *CCCameraBundle;

-(IBAction)closeButtonClick:(id)sender;
-(IBAction)resolutionButtonClick:(id)sender;
-(IBAction)normalButtonClick:(id)sender;
-(IBAction)highButtonClick:(id)sender;
-(IBAction)superButtonClick:(id)sender;
-(IBAction)flashButtonClick:(id)sender;
-(IBAction)captureButtonClick:(id)sender;
-(IBAction)cameraButtonClick:(id)sender;
-(IBAction)fastCamSubviewClick:(id)sender;
-(IBAction)cameraSubviewClick:(id)sender;
-(IBAction)scannerSubviewClick:(id)sender;
-(IBAction)auxModeSubviewClick:(id)sender;
-(void)setFlashButtonVisibility;
-(void)setCameraButtonVisibility;
-(void)incrementFocusIndicatorRadius:(NSTimer *)timer;
-(IBAction)handleZoom:(UIPinchGestureRecognizer *)pinchGestureRecognizer;

@end
