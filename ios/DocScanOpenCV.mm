//
//  DocScanOpenCV.m
//  newcam
//
//  Created by Matt Boyd on 7/31/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "DocScanOpenCV.h"
#import "docscan.hpp"

@interface DocScanOpenCV()

// The docScanner object uses the OpenCV framework to perform the actual image processing
@property (nonatomic) DocScanner *docScanner;

// The lastScanTimestampMS is the time at which the last scan was performed
@property (nonatomic) long lastScanTimestampMS;

// The widthOrig and heightOrig are the width and height of the preview image
@property (nonatomic) int widthOrig;
@property (nonatomic) int heightOrig;

@end

@implementation DocScanOpenCV

-(id)initWithCoder:(NSCoder *)aDecoder {
    
    self = [super initWithCoder:aDecoder];
    if (self) {
        
    }
    
    // This is also a test
    
    return self;
}

-(id)initWithFrame:(CGRect)frame {
    
    self = [super initWithFrame:frame];
    if (self) {
        
    }
    return self;
}

// This method does some initial setup of the view
-(void)initView {
    
}

#pragma mark CCCameraImageProcessor methods

-(void)setImageParams:(int)widthOrig :(int)heightOrig :(int)widthContainer :(int)heightContainer :(int)MAX_OUTPUT_DIM {
    
}


-(void)clearVisiblePreview {
    
}

/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect {
    // Drawing code
}
*/

@end
