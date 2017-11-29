//
//  CCCameraView.m
//  newcam
//
//  Created by Matt Boyd on 5/10/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "CCCameraView.h"

@implementation CCCameraView

@synthesize manager;
@synthesize bridge;
@synthesize camera;
@synthesize cameraLayout;
@synthesize previewView;
@synthesize volumeButtonHandler;
@synthesize placeName;
@synthesize placeAddress;
@synthesize hideNativeUI;
@synthesize appPhotoDirectory;
@synthesize propExifLocationLatitude;
@synthesize propExifLocationLongitude;
@synthesize propExifLocationTimestamp;
@synthesize propAuxModeCaption;
@synthesize isActive;

BOOL _multipleTouches;

#pragma mark Initialization methods

-(id)initWithManager:(CCCameraManager*)_manager bridge:(RCTBridge *)_bridge {

    if (self = [super init]) {
        self.manager = _manager;
        self.bridge = _bridge;

        // Set up pinch-to-zoom handler
        UIPinchGestureRecognizer *pinchGesture = [[UIPinchGestureRecognizer alloc] initWithTarget:self action:@selector(handlePinchToZoomRecognizer:)];
        [self addGestureRecognizer:pinchGesture];
        _multipleTouches = NO;
        self.tag = 1234;
        
        // Load the nib for this view
        NSBundle *bundle = [NSBundle bundleWithURL:[[NSBundle mainBundle] URLForResource:@"CCCameraResources" withExtension:@"bundle"]];

        [bundle loadNibNamed:@"CCCameraView" owner:self options:nil];
        [self addSubview:self.view];

        // Initialize the volumeButtonHandler
        self.volumeButtonHandler = [JPSVolumeButtonHandler volumeButtonHandlerWithUpBlock:^{

            // Initiate the takePicture method
            if (self.camera != nil) {
                id<CCCameraDelegate> cameraDelegate = (id<CCCameraDelegate>)self.camera;
                [cameraDelegate takePicture];
            }

        } downBlock:^{
            // Volume Down Button Pressed
        }];

        // Setup the view
        [self setupView];

        NSLog(@"A CCCameraView was just created!");
    }

    return self;
}

// This method sets up the view
-(void)setupView {

    // Start the volumeButtonHandler
    [self.volumeButtonHandler startHandler:YES];

    // Register to receive a notification when the CCCameraModule is made active
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onSetActive:)
                                                 name:@"CCCameraModuleActiveNotification"
                                               object:nil];

    // Register to receive a notification when the CCCameraModule is made inactive
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onSetInactive:)
                                                 name:@"CCCameraModuleInactiveNotification"
                                               object:nil];

    // Register to receive a notification when the CCCameraModule is force released
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onRelease:)
                                                 name:@"CCCameraModuleReleaseNotification"
                                               object:nil];

    // Register to receive a notification when the capture function is called
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onCapture:)
                                                 name:@"CCCameraModuleCaptureNotification"
                                               object:nil];

    // Register to receive a notification when the capture function is called
    [[NSNotificationCenter defaultCenter] addObserver:self
                                            selector:@selector(onCameraFlip:)
                                                name:@"CCCameraModuleFlipNotification"
                                              object:nil];

    // Register to receive notifications when the app is sent to the background or enters the foreground
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onSetActive:) name:UIApplicationWillEnterForegroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onSetInactive:) name:UIApplicationDidEnterBackgroundNotification object:nil];

    self.isActive = YES;
}

// This method can be used to do any necessary cleanup before closing the view
-(void)finishWithResult:(NSString *)button {

    // Remove any notification listeners if the camera view is being closed
    if ([button isEqualToString:@"close"]) {
        [[NSNotificationCenter defaultCenter] removeObserver:self];
        [[NSNotificationCenter defaultCenter] removeObserver:self.camera];

        // Stop the volumeButtonHandler
        [self.volumeButtonHandler stopHandler];

        // Release the camera
        id<CCCameraDelegate> cameraDelegate = (id<CCCameraDelegate>)self.camera;
        [cameraDelegate releaseCamera];
    }

    self.isActive = NO;

    [self propOnClose:@"" :button];
}

// This method responds to the CCCameraModuleActiveNotification
-(void)onSetActive:(NSNotification *)notification {

    // skip if already active
    if (!self.isActive) {

      // Start the volumeButtonHandler
      [self.volumeButtonHandler startHandler:YES];

      self.isActive = YES;
    }
}

// This method responds to the CCCameraModuleInactiveNotification
-(void)onSetInactive:(NSNotification *)notification {

    // Stop the volumeButtonHandler
    //[self.volumeButtonHandler stopHandler];

    // Remove any observers
    //[[NSNotificationCenter defaultCenter] removeObserver:self];

    self.isActive = NO;
}

// This method responds to the CCCameraModuleReleaseNotification
-(void)onRelease:(NSNotification *)notification {

    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [[NSNotificationCenter defaultCenter] removeObserver:self.camera];

    // Stop the volumeButtonHandler
    [self.volumeButtonHandler stopHandler];

    // Release the camera
    id<CCCameraDelegate> cameraDelegate = (id<CCCameraDelegate>)self.camera;
    [cameraDelegate releaseCamera];

    // remove any react subviews
    NSArray<id<RCTComponent>> *childSubviews = [self reactSubviews];
    for (int i = 0; i < childSubviews.count; i++) {
        [self removeReactSubview:(UIView *)childSubviews[i]];
    }

    self.isActive = NO;
}

// This method responds to the CCCameraModuleReleaseNotification
-(void)onCapture:(NSNotification *)notification {

    // Release the camera
    id<CCCameraDelegate> cameraDelegate = (id<CCCameraDelegate>)self.camera;
    [cameraDelegate takePicture];
}

// This method responds to the CCCameraModuleReleaseNotification
-(void)onCameraFlip:(NSNotification *)notification {

    // Release the camera
    id<CCCameraDelegate> cameraDelegate = (id<CCCameraDelegate>)self.camera;
    [cameraDelegate toggleCamera];
}

#pragma mark Touches

- (void) touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    // How many touches we got here?
    if ([[event allTouches] count] > 1) {
        _multipleTouches = YES;
    }
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    BOOL allTouchesEnded = ([touches count] == [[event allTouches] count]);

    if (allTouchesEnded && !_multipleTouches) {
        if (self.camFocus)
        {
            [self.camFocus removeFromSuperview];
        }
        
        UITouch *touch = [[event allTouches] anyObject];
        CGPoint touchPoint = [touch locationInView:touch.view];
    
        //   TODO: Find a better solution to determining if a button was touched or not
        //
        //   The rub is that since we are listening to all touches, that includes
        //   when the user touches a button. This solution takes into account that
        //   the touch point on a button has a very low y-coord value, while a touch
        //   point in the center of the screen will have a y-coord higher than 64
        
        if (touchPoint.y > 64) {
            id<CCCameraDelegate> cameraDelegate = (id<CCCameraDelegate>)self.camera;
            [cameraDelegate handleTouchEvent:event];
            
        
            // animate at the focus point
            self.camFocus = [[CCCameraFocusSquare alloc] initWithFrame:CGRectMake(touchPoint.x-40, touchPoint.y-40, 80, 80)];
            [self.camFocus setBackgroundColor:[UIColor clearColor]];
            [self addSubview:self.camFocus];
            [self.camFocus setNeedsDisplay];
            
            [UIView beginAnimations:nil context:NULL];
            [UIView setAnimationDuration:1.0];
            [self.camFocus setAlpha:0.0];
            [UIView commitAnimations];
        }
    }
    
    if (allTouchesEnded) {
        _multipleTouches = NO;
    }
}

-(void) handlePinchToZoomRecognizer:(UIPinchGestureRecognizer*)pinchRecognizer {
    // Check if this zoom event has ended
    BOOL zoomEnded = NO;
    if ([pinchRecognizer state] == UIGestureRecognizerStateEnded ||
        [pinchRecognizer state] == UIGestureRecognizerStateCancelled ||
        [pinchRecognizer state] == UIGestureRecognizerStateFailed) {
        zoomEnded = YES;
    }
    
    id<CCCameraDelegate> cameraDelegate = (id<CCCameraDelegate>)self.camera;
    [cameraDelegate handleZoom:pinchRecognizer.scale :zoomEnded];
}


#pragma mark Component props - functions

// This method invokes the onClose prop
-(void)propOnClose:(NSString *)errmsg :(NSString *)button {

    // Invoke onClose prop
    id event = @{
                 @"errmsg": errmsg,
                 @"button": button
                 };

    if (self.onClose) {
        self.onClose(event);
    }
}

// This method invokes the onAuxModeClicked prop
-(void)propOnAuxModeClicked {

    // Invoke onAuxModeClicked prop
    id event = @{};

    if (self.onAuxModeClicked) {
        self.onAuxModeClicked(event);
    }
}

// This method can be used to send an event with the given name and body
-(void)doEvent:(NSString *)eventName :(NSDictionary *)event {

    CCCameraModuleIOS *thisModule = (CCCameraModuleIOS *)[self.bridge moduleForName:@"CCCameraModuleIOS"];
    [thisModule sendEventWithName:eventName body:event];

    // Create a CCCameraEvent object
    /*CCCameraEvent *thisEvent = [[CCCameraEvent alloc] init];
     thisEvent.eventName = eventName;

     [self.bridge.eventDispatcher sendEvent:<#(id<RCTEvent>)#>]*/

}

-(void)doPhotoTaken:(NSString *)imgFile :(int)imgWidth :(int)imgHeight :(NSString *)photoOrigin completion:(void(^)(void))callback{

    if (imgFile == nil) {
        [self propOnClose:@"There was an error saving the photo file" :@"error"];
        return;
    }

    // Invoke the onPhotoTaken prop
    id event = @{
                 @"filename": imgFile,
                 @"imgWidth": [NSNumber numberWithInt:imgWidth],
                 @"imgHeight": [NSNumber numberWithInt:imgHeight],
                 @"photoOrigin": photoOrigin
                 };
    if (self.onPhotoTaken) {
        self.onPhotoTaken(event);
        // [self finishWithResult:@"capture"];
        callback();
    }
}

-(void)doPhotoAccepted:(NSString *)imgFile :(int)imgWidth :(int)imgHeight :(NSString *)photoOrigin {

    if (imgFile == nil) {
        [self propOnClose:@"There was an error saving the photo file" :@"error"];
        return;
    }

    // Invoke the onPhotoAccepted prop
    id event = @{
                 @"filename": imgFile,
                 @"imgWidth": [NSNumber numberWithInt:imgWidth],
                 @"imgHeight": [NSNumber numberWithInt:imgHeight],
                 @"photoOrigin": photoOrigin
                 };
    if (self.onPhotoAccepted) {
        self.onPhotoAccepted(event);
    }
}

-(CLLocation *)getExifLocation {

    CLLocationCoordinate2D locCoordinate = CLLocationCoordinate2DMake(self.propExifLocationLatitude, self.propExifLocationLongitude);
    CLLocation *loc = [[CLLocation alloc] initWithCoordinate:locCoordinate altitude:0 horizontalAccuracy:kCLLocationAccuracyBest verticalAccuracy:kCLLocationAccuracyBest timestamp:[NSDate dateWithTimeIntervalSince1970:self.propExifLocationTimestamp]];
    return loc;
}

#pragma mark Component props - values

-(void)setStoragePath:(NSString *)str {

}

-(void)setProjectName:(NSString *)str {
    self.placeName = str;
    [self.cameraLayout setPlaceNameLabel:self.placeName];
}

-(void)setProjectAddress:(NSString *)str {
    self.placeAddress = str;

    // TODO: Set the placeAddress label
}

-(void)setExifLat:(double)val {
    self.propExifLocationLatitude = val;
}

-(void)setExifLon:(double)val {
    self.propExifLocationLatitude = val;
}

-(void)setExifLocTimestamp:(double)val {
    self.propExifLocationTimestamp = (long)val;
}

-(void)setAuxModeCaption:(NSString *)val {
    self.propAuxModeCaption = val;
    [self.cameraLayout setAuxModeLabelText:self.propAuxModeCaption];
}

-(void)setHideNativeUI:(BOOL)val {
  val ?
    [self.cameraLayout hideCameraLayout] :
    [self.cameraLayout showCameraLayout];
}

@end
