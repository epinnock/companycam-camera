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

@required
-(BOOL)setPreviewBytes:(UIImage *)image;

@required
-(void)clearVisiblePreview;

@required
-(UIImage *)getOutputImage;

@required
-(NSData *)getOutputData;


@end
