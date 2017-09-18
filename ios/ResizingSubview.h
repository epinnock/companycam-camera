//
//  ResizingSubview.h
//  newcam
//
//  Created by Matt Boyd on 5/20/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

// This superclass can be subclassed for any view that needs to be loaded from a nib and may be instantiated in different sizes in different places

#import <UIKit/UIKit.h>

@interface ResizingSubview : UIView {
  
  IBOutlet UIView *view;
}

@property (retain, nonatomic) IBOutlet UIView *view;

-(void)updateViewSize;

@end
