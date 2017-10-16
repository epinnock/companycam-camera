//
//  NSData+ImageEXIF.m
//  CompanyCam
//
//  Created by Chad Wilken on 1/7/16.
//  Copyright © 2016 Company Cam. All rights reserved.
//

#import "NSData+CCImageExif.h"
#import <ImageIO/ImageIO.h>

@implementation NSData (CCImageEXIF)

- (NSDictionary *)exifDataForImage
{
  CGImageSourceRef source = CGImageSourceCreateWithData((__bridge CFDataRef)self, NULL);
  CFDictionaryRef props = CGImageSourceCopyPropertiesAtIndex(source,0, NULL);
  
  NSDictionary *exif = [(__bridge NSDictionary *)props objectForKey : (NSString *)kCGImagePropertyExifDictionary];
  
  CFRelease(props);
  CFRelease(source);
  
  return exif;
}

@end
