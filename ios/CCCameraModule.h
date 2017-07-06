//
//  CCCameraModule.h
//  newcam
//
//  Created by Matt Boyd on 5/18/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RCTBridgeModule.h"
#import "RCTEventEmitter.h"

@interface CCCameraModule : RCTEventEmitter <RCTBridgeModule> {
  
    BOOL isActive;
}

@property (nonatomic, assign) BOOL isActive;

@end
