//
//  CCCameraImageProcessor.h
//  newcam
//
//  Created by Matt Boyd on 7/31/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

@protocol CCCameraImageProcessor <NSObject>

@required
-(void)setImageParams:(int)widthOrig :(int)heightOrig :(int)widthContainer :(int)heightContainer :(int)MAX_OUTPUT_DIM;

// TODO: setPreviewBytes

@required
-(void)clearVisiblePreview;

// TODO: getOutputImage


@end
