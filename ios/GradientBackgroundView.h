//
//  GradientBackgroundView.h
//  newcam
//
//  Created by Matt Boyd on 6/6/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface GradientBackgroundView : UIView {
  
    UIColor *topColor;
    UIColor *bottomColor;
    
    // The gradientDirection property specifies the direction in which the gradient should run
    // 0 = along the Y-axis from topColor to bottomColor
    // 1 = along the Y-axis from bottomColor to topColor
    // 2 = along the X-axis from topColor to bottomColor
    // 3 = along the X-axis from bottomColor to topColor
    int gradientDirection;
}

@property (retain, nonatomic) UIColor *topColor;
@property (retain, nonatomic) UIColor *bottomColor;
@property (assign, nonatomic) int gradientDirection;

@end
