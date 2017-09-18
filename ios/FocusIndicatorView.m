//
//  FocusIndicatorView.m
//  newcam
//
//  Created by Matt Boyd on 7/8/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "FocusIndicatorView.h"

@implementation FocusIndicatorView

@synthesize radius;

// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect {
    
    // Get the current graphics context
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    // Define a stroke width for the indicator
    double strokeWidth = 5.0;
    
    CGRect indicatorRect = CGRectMake(strokeWidth/2.0, strokeWidth/2.0, rect.size.width - strokeWidth, rect.size.height - strokeWidth);
    
    // Draw the main circle
    CGMutablePathRef mainCircle = CGPathCreateMutable();
    CGPathAddEllipseInRect(mainCircle, NULL, indicatorRect);
    CGContextSetStrokeColorWithColor(context, [UIColor whiteColor].CGColor);
    CGContextSetLineCap(context, kCGLineCapRound);
    CGContextSetLineWidth(context, strokeWidth);
    CGContextAddPath(context, mainCircle);
    CGContextStrokePath(context);
    
    // Draw the interior circle
    if (radius > 0) {
        size_t gradLocationsNum = 2;
        CGFloat gradLocations[2] = {0.0f, 1.0f};
        CGFloat gradColors[8] = {1.0f,1.0f,1.0f,0.7f,1.0f,1.0f,1.0f,0.0f};
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        CGGradientRef gradient = CGGradientCreateWithColorComponents(colorSpace, gradColors, gradLocations, gradLocationsNum);
        CGColorSpaceRelease(colorSpace);
        CGPoint gradCenter= CGPointMake(CGRectGetMidX(rect), CGRectGetMidY(rect));
        CGContextDrawRadialGradient (context, gradient, gradCenter, 0, gradCenter, (radius*rect.size.width)/2.0, kCGGradientDrawsBeforeStartLocation);
        CGGradientRelease(gradient);
    }
}

// This method increments the radius value and then redraws the view
-(void)incrementRadius {
    
    // If the radius is greater than or equal to 1, then reset it
    if (radius >= 1.0) {
        radius = 0.0;
    }
    
    // Otherwise, increment the radius value
    else {
        radius += 0.05;
    }
        
    [self setNeedsDisplay];
}

@end
