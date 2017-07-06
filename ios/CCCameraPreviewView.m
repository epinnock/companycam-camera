//
//  CCCameraPreviewView.m
//  newcam
//
//  Created by Matt Boyd on 6/1/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "CCCameraPreviewView.h"

@implementation CCCameraPreviewView

@synthesize previewLayer;

-(id)initWithCoder:(NSCoder *)aDecoder {
    
    self = [super initWithCoder:aDecoder];
    if (self) {
        self.previewLayer = (AVCaptureVideoPreviewLayer *)self.layer;
        self.previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    }
    
    return self;
}

-(id)initWithFrame:(CGRect)frame {
    
    self = [super initWithFrame:frame];
    if (self) {
        self.previewLayer = (AVCaptureVideoPreviewLayer *)self.layer;
        self.previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    }
    return self;
}

+(Class)layerClass {
    return [AVCaptureVideoPreviewLayer class];
}

-(AVCaptureVideoPreviewLayer *)previewLayer {
    return (AVCaptureVideoPreviewLayer *)self.layer;
}

@end
