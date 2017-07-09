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
- (UIImage *)rotateUIImage;
- (UIImage *)rotate;
- (UIImage *)scaledToSize:(CGSize)newSize;

@end
