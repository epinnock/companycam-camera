//
//  ResizingSubview.m
//  newcam
//
//  Created by Matt Boyd on 5/20/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "ResizingSubview.h"

@implementation ResizingSubview

@synthesize view;

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
    }
    return self;
}

// This method updates the size of the view loaded from the nib to the size of the frame
-(void)updateViewSize {
    
    // Update the view size
    self.view.frame = CGRectMake(0, 0, self.frame.size.width, self.frame.size.height);
    
    if (self.view != nil) {
        [self addConstraint:[NSLayoutConstraint constraintWithItem:self.view
                                                         attribute:NSLayoutAttributeLeft
                                                         relatedBy:NSLayoutRelationEqual
                                                            toItem:self
                                                         attribute:NSLayoutAttributeLeft
                                                        multiplier:1
                                                          constant:0]];
        [self addConstraint:[NSLayoutConstraint constraintWithItem:self.view
                                                         attribute:NSLayoutAttributeTop
                                                         relatedBy:NSLayoutRelationEqual
                                                            toItem:self
                                                         attribute:NSLayoutAttributeTop
                                                        multiplier:1
                                                          constant:0]];
        [self addConstraint:[NSLayoutConstraint constraintWithItem:self.view
                                                         attribute:NSLayoutAttributeRight
                                                         relatedBy:NSLayoutRelationEqual
                                                            toItem:self
                                                         attribute:NSLayoutAttributeRight
                                                        multiplier:1
                                                          constant:0]];
        [self addConstraint:[NSLayoutConstraint constraintWithItem:self.view
                                                         attribute:NSLayoutAttributeBottom
                                                         relatedBy:NSLayoutRelationEqual
                                                            toItem:self
                                                         attribute:NSLayoutAttributeBottom
                                                        multiplier:1
                                                          constant:0]];
    }
}

// Override layoutSubviews in order to update the view size after the frame width and height have been set.
-(void)layoutSubviews {
    [super layoutSubviews];
    [self updateViewSize];
}

-(void)setNeedsDisplay {
    
    [self updateViewSize];
    [super setNeedsDisplay];
}

@end
