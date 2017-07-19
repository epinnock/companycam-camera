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
@synthesize appPhotoDirectory;
@synthesize propExifLocationLatitude;
@synthesize propExifLocationLongitude;
@synthesize propExifLocationTimestamp;
@synthesize propAuxModeCaption;

#pragma mark Initialization methods

-(id)initWithManager:(CCCameraManager*)_manager bridge:(RCTBridge *)_bridge {
  
    if (self = [super init]) {
        self.manager = _manager;
        self.bridge = _bridge;
        
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
        
        // Register to receive notifications when the app is sent to the background or enters the foreground
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onSetActive:) name:UIApplicationDidBecomeActiveNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onSetInactive:) name:UIApplicationDidEnterBackgroundNotification object:nil];


        NSLog(@"A CCCameraView was just created!");
    }
    
    return self;
}

// This method can be used to do any necessary cleanup before closing the view
-(void)finishWithResult:(NSString *)button {
    
    // Remove any notification listeners
    //[[NSNotificationCenter defaultCenter] removeObserver:self];
    
    [self propOnClose:@"" :button];
}

// This method responds to the CCCameraModuleActiveNotification
-(void)onSetActive:(NSNotification *)notification {
    
    // Start the volumeButtonHandler
    [self.volumeButtonHandler startHandler:YES];
}

// This method responds to the CCCameraModuleInactiveNotification
-(void)onSetInactive:(NSNotification *)notification {
    
    // Stop the volumeButtonHandler
    [self.volumeButtonHandler stopHandler];
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
    
    CCCameraModule *thisModule = (CCCameraModule *)[self.bridge moduleForName:@"CCCameraModule"];
    [thisModule sendEventWithName:eventName body:event];
    
    // Create a CCCameraEvent object
    /*CCCameraEvent *thisEvent = [[CCCameraEvent alloc] init];
    thisEvent.eventName = eventName;
    
    [self.bridge.eventDispatcher sendEvent:<#(id<RCTEvent>)#>]*/
    
}

-(void)doPhotoTaken:(NSString *)imgFile :(int)imgWidth :(int)imgHeight {
    
    if (imgFile == nil) {
        [self propOnClose:@"There was an error saving the photo file" :@"error"];
        return;
    }
    
    // Invoke the onPhotoTaken prop
    id event = @{
        @"filename": imgFile,
        @"imgWidth": [NSNumber numberWithInt:imgWidth],
        @"imgHeight": [NSNumber numberWithInt:imgHeight]
    };
    if (self.onPhotoTaken) {
        self.onPhotoTaken(event);
        [self finishWithResult:@"capture"];
    }
}

-(void)doPhotoAccepted:(NSString *)imgFile :(int)imgWidth :(int)imgHeight {
    
    if (imgFile == nil) {
        [self propOnClose:@"There was an error saving the photo file" :@"error"];
        return;
    }
    
    // Invoke the onPhotoAccepted prop
    id event = @{
                 @"filename": imgFile,
                 @"imgWidth": [NSNumber numberWithInt:imgWidth],
                 @"imgHeight": [NSNumber numberWithInt:imgHeight]
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
    [self.cameraLayout setAuxModeLabel:self.propAuxModeCaption];
}

@end
