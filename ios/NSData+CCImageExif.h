//
//  NSData+ImageEXIF.h
//  CompanyCam
//
//  Created by Chad Wilken on 1/7/16.
//  Copyright © 2016 Company Cam. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSData (CCImageEXIF)

- (NSDictionary *)exifDataForImage;

@end
