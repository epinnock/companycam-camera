//
//  CCCameraPreviewView.h
//  newcam
//
//  Created by Matt Boyd on 6/1/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>

@interface CCCameraPreviewView : UIView {
  
    // The preview layer contains the camera preview
    AVCaptureVideoPreviewLayer *previewLayer;
  
}

@property (nonatomic) AVCaptureVideoPreviewLayer *previewLayer;

@end


