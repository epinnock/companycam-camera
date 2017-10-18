//
//  DocScanOpenCV.h
//  newcam
//
//  Created by Matt Boyd on 7/31/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "CCCameraImageProcessor.h"
#import "UIImage+CCMHelper.h"

@class CCCameraManager;

#define SCAN_STATUS_UNSTABLE 0
#define SCAN_STATUS_STABLE 1
#define SCAN_STATUS_DONE 2
#define COLOR_UNSTABLE [UIColor colorWithRed:(64.0/255.0) green:1 blue:0 alpha:0].CGColor;
#define COLOR_STABLE [UIColor colorWithRed:(128.0/255.0) green:0 blue:(128.0/255.0) alpha:1].CGColor;
#define COLOR_DONE [UIColor colorWithRed:(128.0/255.0) green:0 blue:1 alpha:0].CGColor;

@interface DocScanOpenCV : UIView <CCCameraImageProcessor>

@end
