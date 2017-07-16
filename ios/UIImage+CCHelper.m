//
//  UIImage+CCHelper.m
//  newcam
//
//  Created by Matt Boyd on 7/8/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "UIImage+CCHelper.h"

@implementation UIImage (CCHelper)

- (UIImage *)tintWithColor:(UIColor *)color
{
    UIGraphicsBeginImageContextWithOptions(self.size, NO, 0.0);
    
    CGRect rect = CGRectZero;
    rect.size = self.size;
    
    [color set];
    UIRectFill(rect);
    
    [self drawInRect:rect blendMode:kCGBlendModeDestinationIn alpha:1.0];
    
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return newImage;
}

- (UIImage *)tintWhite
{
    return [self tintWithColor:[UIColor whiteColor]];
}

+ (UIImage *)imageFromColor:(UIColor *)color forSize:(CGSize)size
{
    CGRect rect = CGRectMake(0, 0, size.width, size.height);
    UIGraphicsBeginImageContext(rect.size);
    
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextSetFillColorWithColor(context, [color CGColor]);
    CGContextFillRect(context, rect);
    
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    // Begin a new image that will be the new image with the rounded corners
    // (here with the size of an UIImageView)
    UIGraphicsBeginImageContext(size);
    
    // Add a clip before drawing anything, in the shape of an rounded rect
    [[UIBezierPath bezierPathWithRoundedRect:rect cornerRadius:0] addClip];
    // Draw your image
    [image drawInRect:rect];
    
    // Get the image, here setting the UIImageView image
    image = UIGraphicsGetImageFromCurrentImageContext();
    
    // Lets forget about that we were drawing
    UIGraphicsEndImageContext();
    
    return image;
}

- (UIImage *) croppedImage:(CGRect)cropRect
{
    return [self croppedImage:cropRect forOrientation:self.imageOrientation];
}

- (UIImage *)croppedImage:(CGRect)cropRect forOrientation:(UIImageOrientation)orientation {
    
    CGImageRef imageRef = CGImageCreateWithImageInRect( [self CGImage], cropRect );
    UIImage *croppedImage = [UIImage imageWithCGImage:imageRef scale:1.0f orientation:orientation];
    CGImageRelease(imageRef);
    return croppedImage;
}

- (UIImage *) rotateUIImage
{
    // No-op if the orientation is already correct
    if (self.imageOrientation == UIImageOrientationUp) {
        return self;
    }
    
    // Otherwise, rotate the image based on it's imageOrientation
    else {
        return [self rotateForImageOrientation:self.imageOrientation];
    }
}

- (UIImage *)rotateForImageOrientation:(UIImageOrientation)orientation {
    
    // We need to calculate the proper transformation to make the image upright.
    // We do it in 2 steps: Rotate if Left/Right/Down, and then flip if Mirrored.
    CGAffineTransform transform = CGAffineTransformIdentity;
    
    switch (orientation) {
        case UIImageOrientationDown:
        case UIImageOrientationDownMirrored:
            transform = CGAffineTransformTranslate(transform, (self.size.width + (fabs(self.size.height - self.size.width))/2), (self.size.height - (fabs(self.size.height - self.size.width))/2));
            transform = CGAffineTransformRotate(transform, M_PI);
            break;
            
        case UIImageOrientationLeft:
        case UIImageOrientationLeftMirrored:
            transform = CGAffineTransformTranslate(transform, self.size.width, 0);
            transform = CGAffineTransformRotate(transform, M_PI_2);
            break;
            
        case UIImageOrientationRight:
        case UIImageOrientationRightMirrored:
            transform = CGAffineTransformTranslate(transform, 0, self.size.height);
            transform = CGAffineTransformRotate(transform, -M_PI_2);
            break;
        case UIImageOrientationUp:
        case UIImageOrientationUpMirrored:
            transform = CGAffineTransformTranslate(transform, -(fabs(self.size.height - self.size.width))/2, (fabs(self.size.height - self.size.width))/2);
            transform = CGAffineTransformScale(transform, 0.9, 0.9);
            break;
    }
    
    switch (orientation) {
        case UIImageOrientationUpMirrored:
        case UIImageOrientationDownMirrored:
            transform = CGAffineTransformTranslate(transform, self.size.width, 0);
            transform = CGAffineTransformScale(transform, -1, 1);
            break;
            
        case UIImageOrientationLeftMirrored:
        case UIImageOrientationRightMirrored:
            transform = CGAffineTransformTranslate(transform, self.size.height, 0);
            transform = CGAffineTransformScale(transform, -1, 1);
            break;
        case UIImageOrientationUp:
        case UIImageOrientationDown:
        case UIImageOrientationLeft:
        case UIImageOrientationRight:
            break;
    }
    
    // Now we draw the underlying CGImage into a new context, applying the transform
    // calculated above.
    CGContextRef ctx = CGBitmapContextCreate(NULL, self.size.width, self.size.height,
                                             CGImageGetBitsPerComponent(self.CGImage), 0,
                                             CGImageGetColorSpace(self.CGImage),
                                             CGImageGetBitmapInfo(self.CGImage));
    
    CGContextConcatCTM(ctx, transform);
    CGContextDrawImage(ctx, CGRectMake(0,0,self.size.height,self.size.width), self.CGImage);
    
    // And now we just create a new UIImage from the drawing context
    CGImageRef cgimg = CGBitmapContextCreateImage(ctx);
    UIImage *img = [UIImage imageWithCGImage:cgimg];
    CGContextRelease(ctx);
    CGImageRelease(cgimg);
    return img;
}

- (UIImage *)rotate
{
    UIImageOrientation currentOrientation = self.imageOrientation;
    UIImageOrientation destinationOrientation = currentOrientation;
    
    if (currentOrientation == UIImageOrientationUp)
    {
        destinationOrientation = UIImageOrientationRight;
    }
    else if (currentOrientation == UIImageOrientationRight)
    {
        destinationOrientation = UIImageOrientationDown;
    }
    else if (currentOrientation == UIImageOrientationDown)
    {
        destinationOrientation = UIImageOrientationLeft;
    }
    else if (currentOrientation == UIImageOrientationLeft)
    {
        destinationOrientation = UIImageOrientationUp;
    }
    else
    {
        destinationOrientation = currentOrientation;
    }
    
    return [[UIImage alloc] initWithCGImage:[self CGImage] scale:1.f orientation:destinationOrientation];
}

- (UIImage *)scaledToSize:(CGSize)newSize
{
    UIGraphicsBeginImageContextWithOptions(newSize, NO, 1.0);
    [self drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return newImage;
}

@end
