//
//  CCCameraView.h
//  newcam
//
//  Created by Matt Boyd on 5/10/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <CoreLocation/CoreLocation.h>
#import <React/RCTBridge.h>
#import "CCCameraModule.h"
#import <React/RCTUIManager.h>
#import "ResizingSubview.h"
#import "CCCameraLayoutDelegate.h"
#import "CCCameraDelegate.h"
#import "CCCameraPreviewView.h"
#import "JPSVolumeButtonHandler/JPSVolumeButtonHandler.h"

@class CCCameraManager;
@class CCCamera;

@interface CCCameraView : ResizingSubview {

		CCCameraManager *manager;
		RCTBridge *bridge;

    // The camera object implements the camera-related behavior
    CCCamera *camera;

    // This is the subview that makes up the entire content of a CCCameraView
    id<CCCameraLayoutDelegate> cameraLayout;

    // The CCCameraPreviewView contains the camera preview
    IBOutlet CCCameraPreviewView *previewView;

    // The volumeButtonHandler is used to snap a photo using the volume button
    JPSVolumeButtonHandler *volumeButtonHandler;

    // Component prop values
    NSString *placeName;
    NSString *placeAddress;
    NSString *appPhotoDirectory;
    double propExifLocationLatitude;
    double propExifLocationLongitude;
    long propExifLocationTimestamp;
    NSString *propAuxModeCaption;
		BOOL hideNativeUI;

    BOOL isActive;
}

@property (nonatomic, retain) CCCameraManager *manager;
@property (nonatomic, retain) RCTBridge *bridge;
@property (nonatomic, retain) CCCamera *camera;
@property (nonatomic, retain) id<CCCameraLayoutDelegate> cameraLayout;
@property (nonatomic, retain) IBOutlet CCCameraPreviewView *previewView;
@property (strong, nonatomic) JPSVolumeButtonHandler *volumeButtonHandler;
@property (nonatomic, retain) NSString *placeName;
@property (nonatomic, retain) NSString *placeAddress;
@property (nonatomic, retain) NSString *appPhotoDirectory;
@property (nonatomic, assign) double propExifLocationLatitude;
@property (nonatomic, assign) double propExifLocationLongitude;
@property (nonatomic, assign) long propExifLocationTimestamp;
@property (nonatomic, retain) NSString *propAuxModeCaption;
@property (nonatomic, assign) BOOL hideNativeUI;
@property (nonatomic, assign) BOOL isActive;

@property (nonatomic, copy) RCTDirectEventBlock onClose;
@property (nonatomic, copy) RCTDirectEventBlock onPhotoTaken;
@property (nonatomic, copy) RCTDirectEventBlock onPhotoAccepted;
@property (nonatomic, copy) RCTDirectEventBlock onAuxModeClicked;

-(id)initWithManager:(CCCameraManager*)_manager bridge:(RCTBridge *)_bridge;
-(void)setupView;
-(void)setCamera:(CCCamera *)thisCamera;
-(void)finishWithResult:(NSString *)button;

/////////////////////////////////
// Component props - Functions //
/////////////////////////////////

-(void)propOnClose:(NSString *)errmsg :(NSString *)button;
-(void)propOnAuxModeClicked;
-(void)doEvent:(NSString *)eventName :(NSDictionary *)event;
-(void)doPhotoTaken:(NSString *)imgFile :(int)imgWidth :(int)imgHeight :(NSString *)photoOrigin completion:(void(^)(void))callback;
-(void)doPhotoAccepted:(NSString *)imgFile :(int)imgWidth :(int)imgHeight :(NSString *)photoOrigin;
-(CLLocation *)getExifLocation;

//////////////////////////////
// Component props - Values //
//////////////////////////////

-(void)setStoragePath:(NSString *)str;
// TODO: getStoragePath
-(void)setProjectName:(NSString *)str;
-(void)setProjectAddress:(NSString *)str;
-(void)setExifLat:(double)val;
-(void)setExifLon:(double)val;
-(void)setExifLocTimestamp:(double)val;
-(void)setAuxModeCaption:(NSString *)val;
-(void)setHideNativeUI:(BOOL)val;

@end
