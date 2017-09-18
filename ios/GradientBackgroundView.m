//
//  GradientBackgroundView.m
//  newcam
//
//  Created by Matt Boyd on 6/6/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "GradientBackgroundView.h"

@implementation GradientBackgroundView

@synthesize topColor;
@synthesize bottomColor;
@synthesize gradientDirection;

// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect {

    // Get the current context and save it.
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextSaveGState(context);
    
    // Define a gradient for the background of the view
    CGPathRef backgroundPath = CGPathCreateWithRect(rect, NULL);
    CGContextAddPath(context, backgroundPath);
    CGColorSpaceRef backColorSpace = CGColorSpaceCreateDeviceRGB();
    size_t num = 2;
    CGFloat loc[2] = { 0.0, 1.0 };
    CGFloat r1, g1, b1, alpha1;
    [[self topColor] getRed:&r1 green:&g1 blue:&b1 alpha:&alpha1];
    CGFloat r2, g2, b2, alpha2;
    [[self bottomColor] getRed:&r2 green:&g2 blue:&b2 alpha:&alpha2];
    
    CGFloat backComponents[8] = { r1, g1, b1, alpha1, r2, g2, b2, alpha2 };
    
    // Set the start and end points for the gradient based on the gradientDirection property
    CGPoint backStart;
    CGPoint backEnd;
    switch (self.gradientDirection) {
        case 1:
            backStart = CGPointMake(CGRectGetMidX(rect), CGRectGetMaxY(rect));
            backEnd = CGPointMake(CGRectGetMidX(rect), CGRectGetMinY(rect));
            break;
        case 2:
            backStart = CGPointMake(CGRectGetMinX(rect), CGRectGetMidY(rect));
            backEnd = CGPointMake(CGRectGetMaxX(rect), CGRectGetMidY(rect));
            break;
        case 3:
            backStart = CGPointMake(CGRectGetMaxX(rect), CGRectGetMidY(rect));
            backEnd = CGPointMake(CGRectGetMinX(rect), CGRectGetMidY(rect));
            break;
        default:
            backStart = CGPointMake(CGRectGetMidX(rect), CGRectGetMinY(rect));
            backEnd = CGPointMake(CGRectGetMidX(rect), CGRectGetMaxY(rect));
            break;
    }
    CGGradientRef backGradient = CGGradientCreateWithColorComponents(backColorSpace, backComponents, loc, num);
    CGContextDrawLinearGradient(context, backGradient, backStart, backEnd, kCGGradientDrawsBeforeStartLocation);
}


@end
