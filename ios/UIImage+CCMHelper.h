//
//  UIImage+CCMHelper.h
//  newcam
//
//  Created by Matt Boyd on 7/8/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface UIImage (CCHMelper)

- (UIImage *)CCMTintWithColor:(UIColor *)color;
- (UIImage *)CCMTintWhite;
+ (UIImage *)CCMImageFromColor:(UIColor *)color forSize:(CGSize)size;
- (UIImage *)CCMCroppedImage:(CGRect)cropRect;
- (UIImage *)CCMCroppedImage:(CGRect)cropRect forOrientation:(UIImageOrientation)orientation;
- (UIImage *)CCMRotateUIImage;
- (UIImage *)CCMRotateForImageOrientation:(UIImageOrientation)orientation;
- (UIImage *)CCMRotate;
- (UIImage *)CCMImageRotatedByDegrees:(CGFloat)degrees;
- (UIImage *)CCMScaledToSize:(CGSize)newSize;

@end
