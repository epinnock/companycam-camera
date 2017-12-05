//
//  CCCameraFocusSquare.m
//  CCCamera
//
//  Created by Jason Gaare on 11/28/17.
//
//  Emphatically copied from https://github.com/lwansbrough/react-native-camera/blob/master/ios/CameraFocusSquare.m

#import "CCCameraFocusSquare.h"
#import <QuartzCore/QuartzCore.h>

@implementation CCCameraFocusSquare

- (id)initWithFrame:(CGRect)frame
{
  self = [super initWithFrame:frame];
  if (self) {
    // Initialization code

    [self setBackgroundColor:[UIColor clearColor]];
    [self.layer setBorderWidth:2.0];
    [self.layer setCornerRadius:4.0];
    [self.layer setBorderColor:[UIColor colorWithRed:(255/255.0)
                                               green:(179/255.0)
                                                blue:(0/255.0)
                                               alpha:1].CGColor]; // hex #ffb300

  }
  return self;
}
@end
