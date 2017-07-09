//
//  FileWriter.m
//  CompanyCam
//
//  Created by Chad Wilken on 9/15/16.
//  Copyright © 2016 Company Cam. All rights reserved.
//

#import "StorageUtility.h"

@implementation StorageUtility

+ (NSString *)writeDataToFile:(NSData *)data
{
  NSString *uniqueIdentifier = [NSUUID UUID].UUIDString;
  NSString *filename = [uniqueIdentifier stringByAppendingString:@".jpg"];
  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSURL *url = [NSURL fileURLWithPath:[paths objectAtIndex:0]];
  NSURL *fileURL = [url URLByAppendingPathComponent:filename];
  BOOL success = [data writeToURL:fileURL atomically:YES];
  
  if (success) {
    return filename;
  }
  
  return nil;
}

@end
