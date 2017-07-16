//
//  UIImage+CCHelper.h
//  newcam
//
//  Created by Matt Boyd on 7/8/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface UIImage (CCHelper)

- (UIImage *)tintWithColor:(UIColor *)color;
- (UIImage *)tintWhite;
+ (UIImage *)imageFromColor:(UIColor *)color forSize:(CGSize)size;
- (UIImage *)croppedImage:(CGRect)cropRect;
- (UIImage *)croppedImage:(CGRect)cropRect forOrientation:(UIImageOrientation)orientation;
- (UIImage *)rotateUIImage;
- (UIImage *)rotateForImageOrientation:(UIImageOrientation)orientation;
- (UIImage *)rotate;
- (UIImage *)scaledToSize:(CGSize)newSize;

@end
