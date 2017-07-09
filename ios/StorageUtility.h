//
//  FileWriter.h
//  CompanyCam
//
//  Created by Chad Wilken on 9/15/16.
//  Copyright © 2016 Company Cam. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface StorageUtility : NSObject

+ (NSString *)writeDataToFile:(NSData *)data;

@end
