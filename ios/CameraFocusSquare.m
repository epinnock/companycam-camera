//
//  CameraFocusSquare.m
//  CCCamera
//
//  Created by Jason Gaare on 11/28/17.
//
//  Emphatically copied from https://github.com/lwansbrough/react-native-camera/blob/master/ios/CameraFocusSquare.m

#import "CameraFocusSquare.h"
#import <QuartzCore/QuartzCore.h>

const float squareLength = 80.0f;
@implementation CCCameraFocusSquare

- (id)initWithFrame:(CGRect)frame
{
  self = [super initWithFrame:frame];
  if (self) {
    // Initialization code
    
    [self setBackgroundColor:[UIColor clearColor]];
    [self.layer setBorderWidth:2.0];
    [self.layer setCornerRadius:4.0];
    [self.layer setBorderColor:[UIColor yellowColor].CGColor];

  }
  return self;
}
@end
