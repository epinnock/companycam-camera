//
//  NSMutableDictionary+ImageMetadata.h
//  CompanyCam
//
//  Created by Chad Wilken on 1/7/16.
//  Copyright © 2016 Company Cam. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreMedia/CoreMedia.h>
#import <CoreLocation/CoreLocation.h>
#import <UIKit/UIKit.h>

@interface NSMutableDictionary (CCImageMetadata)

@property (nonatomic) CLLocation *location;
@property (nonatomic) CLLocationDirection trueHeading;

- (id)initWithImageSampleBuffer:(CMSampleBufferRef) imageDataSampleBuffer;
- (void)setUserComment:(NSString*)comment;
- (void)setDateOriginal:(NSDate *)date;
- (void)setDateDigitized:(NSDate *)date;
- (void)setMake:(NSString*)make model:(NSString*)model software:(NSString*)software;
- (void)setDescription:(NSString*)description;
- (void)setKeywords:(NSString*)keywords;
- (void)setImageOrientation:(UIImageOrientation)orientation;
- (void)setDigitalZoom:(CGFloat)zoom;
- (void)setHeading:(CLHeading*)heading;

@end
