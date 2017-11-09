//
//  CCCameraManager.h
//  newcam
//
//  Created by Matt Boyd on 5/10/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <React/RCTViewManager.h>
#import "CCCameraView.h"
#import "CCCamera.h"
#import "CCCameraEnums.h"
#import "CCCameraLayout.h"

@interface CCCameraManager : RCTViewManager {

}

@property (nonatomic, retain) CCCameraView *latestView;

+(CCCameraView *)getLatestView;

@end
