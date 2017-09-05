//
//  DocScanOpenCV.m
//  newcam
//
//  Created by Matt Boyd on 7/31/17.
//  Copyright © 2017 Facebook. All rights reserved.
//

#import "DocScanOpenCV.h"
#import "docscan.hpp"
#import <opencv2/core/core.hpp>
#import <opencv2/imgproc/imgproc.hpp>

@interface DocScanOpenCV()

// The docScanner object uses the OpenCV framework to perform the actual image processing
@property (nonatomic) DocScanner *docScanner;

// The lastScanTimestampMS is the time at which the last scan was performed
@property (nonatomic) long long lastScanTimestampMS;

// The widthOrig and heightOrig are the width and height of the preview image
@property (nonatomic) int widthOrig;
@property (nonatomic) int heightOrig;

// The widthOverlay and heightOverlay are the width and height of the preview view
@property (nonatomic) int widthOverlay;
@property (nonatomic) int heightOverlay;

// The perspectiveRectArray contains the points that make up the current perspective rectangle
@property (nonatomic) NSMutableArray *perspectiveRectArray;

// The latestStatus is the status of the most recent scan
@property (nonatomic) DocScanner::ScanStatus latestStatus;

// The imageOutput is the latest image output from the scanner
@property (nonatomic) UIImage *imageOutput;

// The imageData is the latest image data from the scanner
@property (nonatomic) NSData *imageData;

@end

@implementation DocScanOpenCV

-(id)initWithCoder:(NSCoder *)aDecoder {
    
    self = [super initWithCoder:aDecoder];
    if (self) {
        
    }
    
    // Initialize the view
    [self initView];
    
    return self;
}

-(id)initWithFrame:(CGRect)frame {
    
    self = [super initWithFrame:frame];
    if (self) {
        
    }
    
    // Initialize the view
    [self initView];
    
    return self;
}

// This method does some initial setup of the view
-(void)initView {
    
    // Initialize the docScanner object
    self.docScanner = new DocScanner();
    
    // Initialize the perspectiveRectArray
    self.perspectiveRectArray = [[NSMutableArray alloc] initWithCapacity:1];
}

#pragma mark CCCameraImageProcessor methods

-(void)setImageParams:(int)widthOrig :(int)heightOrig :(int)widthContainer :(int)heightContainer :(int)MAX_OUTPUT_DIM {
    
    // Set the original width and height of the image
    self.widthOrig = widthOrig;
    self.heightOrig = heightOrig;
    
    // Set the width and height of the preview view
    self.widthOverlay = widthContainer;
    self.heightOverlay = heightContainer;
}

-(BOOL)setPreviewBytes:(UIImage *)image {
    
    // Check how much time has elsapsed since the last the last image was processed
    long long currentUNIXTimeMS = [[NSDate date] timeIntervalSince1970] * 1000.0;
    
    if (self.lastScanTimestampMS == 0) {
        self.lastScanTimestampMS = currentUNIXTimeMS;
        return YES;
    }
    else if (currentUNIXTimeMS - self.lastScanTimestampMS < 500.0) {
        return YES;
    }
    
    self.lastScanTimestampMS = currentUNIXTimeMS;
    
    BOOL requestNextFrame = YES;
    
    // Convert the image to a Mat object
    cv::Mat thisMat = [self cvMatFromUIImage:image];
    
    NSLog(@"Got a matrix!");
    
    // Pass the matrix to the DocScanner and get the scan status
    self.latestStatus = self.docScanner->smartScan(thisMat);
    
    // Get the current rect from the DocScanner
    const geom::PerspectiveRect pRect = self.docScanner->getPerspectiveRect();
    
    // Update the perspectiveRectArray
    [self updatePerspecitveRectArray:pRect];
    
    switch (self.latestStatus) {
        case DocScanner::UNSTABLE:
            NSLog(@"The scan was UNSTABLE");
            break;
        case DocScanner::STABLE:
            NSLog(@"The scan was STABLE");
            break;
        case DocScanner::DONE: {
            
            // Set the requestNextFrame flag so that the camera will stop sending frames to the scanner
            requestNextFrame = NO;
            
            NSLog(@"The scan was DONE");
            break;
        }
        default:
            NSLog(@"The scan was none of the above");
            break;
    }
    
    // Redraw the updated perspective rect
    [self performSelector:@selector(setNeedsDisplay) onThread:[NSThread mainThread] withObject:nil waitUntilDone:NO];
    
    return requestNextFrame;
}


-(void)clearVisiblePreview {
    
    // Reset the perspectiveRectArray
    self.perspectiveRectArray = [[NSMutableArray alloc] initWithCapacity:1];
    
    // Redraw the updated perspective rect
    [self performSelector:@selector(setNeedsDisplay) onThread:[NSThread mainThread] withObject:nil waitUntilDone:NO];
}

-(UIImage *)getOutputImage {
    if (self.docScanner != nil) {
        self.imageOutput = [self UIImageFromCVMat:self.docScanner->getOutputImage()];
        return self.imageOutput;
    }
    else return [[UIImage alloc] init];
}

#ifdef __cplusplus
-(NSData *)getOutputData {
    if (self.docScanner != nil) {
        cv::Mat imageMat = self.docScanner->getOutputImage();
        self.imageData = [NSData dataWithBytes:imageMat.data length:imageMat.elemSize()*imageMat.total()];
        return self.imageData;
    }
    else {
        return [[NSData alloc] init];
    }
}

#pragma mark Miscellaneous

// This method for converting a UIImage into a Mat object comes from the OpenCV documentation at:
// http://docs.opencv.org/2.4/doc/tutorials/ios/image_manipulation/image_manipulation.html
-(cv::Mat)cvMatFromUIImage:(UIImage *)image {
    
    CGColorSpaceRef colorSpace = CGImageGetColorSpace(image.CGImage);
    CGFloat cols = image.size.width;
    CGFloat rows = image.size.height;
    
    cv::Mat cvMat(rows, cols, CV_8UC4); // 8 bits per component, 4 channels (color channels + alpha)
    
    CGContextRef contextRef = CGBitmapContextCreate(cvMat.data,                 // Pointer to  data
                                                    cols,                       // Width of bitmap
                                                    rows,                       // Height of bitmap
                                                    8,                          // Bits per component
                                                    cvMat.step[0],              // Bytes per row
                                                    colorSpace,                 // Colorspace
                                                    kCGImageAlphaNoneSkipLast |
                                                    kCGBitmapByteOrderDefault); // Bitmap info flags
    
    CGContextDrawImage(contextRef, CGRectMake(0, 0, cols, rows), image.CGImage);
    CGContextRelease(contextRef);
    
    return cvMat;
}

// This method for converting a Mat into a UIImage comes from the OpenCV documentation at:
// http://docs.opencv.org/2.4/doc/tutorials/ios/image_manipulation/image_manipulation.html
-(UIImage *)UIImageFromCVMat:(cv::Mat)cvMat {
    
    // If the width of cvMat is less than the height, then transpose it because the subsequent UIImage operations (rotation, scaling, cropping, JPEG conversion) will fail otherwise
    BOOL shouldPostRotate = NO;
    if (cvMat.cols < cvMat.rows) {
        
        // Transpose the matrix
        cvMat = cvMat.t();
        
        // Reverse the columns after the transpose so the image isn't mirrored
        cv::Mat tempMat;
        cv::flip(cvMat, tempMat, 1);
        cvMat = tempMat;
        
        // If the matrix had to be transposed, then set the flag to post rotate the image after it's created to reset it to the original orientation
        shouldPostRotate = YES;
    }
    
    NSData *data = [NSData dataWithBytes:cvMat.data length:cvMat.elemSize()*cvMat.total()];
    CGColorSpaceRef colorSpace;
    
    if (cvMat.elemSize() == 1) {
        colorSpace = CGColorSpaceCreateDeviceGray();
    }
    else {
        colorSpace = CGColorSpaceCreateDeviceRGB();
    }
    
    CGDataProviderRef provider = CGDataProviderCreateWithCFData((__bridge CFDataRef)data);
    
    // Creating CGImage from cv::Mat
    CGImageRef imageRef = CGImageCreate(cvMat.cols,                                 //width
                                        cvMat.rows,                                 //height
                                        8,                                          //bits per component
                                        8 * cvMat.elemSize(),                       //bits per pixel
                                        cvMat.step[0],                            //bytesPerRow
                                        colorSpace,                                 //colorspace
                                        kCGImageAlphaNone|kCGBitmapByteOrderDefault,// bitmap info
                                        provider,                                   //CGDataProviderRef
                                        NULL,                                       //decode
                                        false,                                      //should interpolate
                                        kCGRenderingIntentDefault                   //intent
                                        );
    
    // Getting UIImage from CGImage
    UIImage *finalImage = [UIImage imageWithCGImage:imageRef];
    CGImageRelease(imageRef);
    CGDataProviderRelease(provider);
    CGColorSpaceRelease(colorSpace);
    
    // If the matrix had to be transposed, then rotate the finalImage back to it's original orientation
    if (shouldPostRotate) {
        finalImage = [finalImage imageRotatedByDegrees:-90.0f];
    }
    
    return finalImage;
}

// This method updates the perspectiveRectArray based on the given geom::PerspectiveRect object
-(void)updatePerspecitveRectArray:(geom::PerspectiveRect)pRect {
    
    // Reset the perspectiveRectArray
    self.perspectiveRectArray = [[NSMutableArray alloc] initWithCapacity:1];
    
    // Get each of the original points from the PerspectiveRect object
    CGPoint point00 = CGPointMake(pRect.p00.x, pRect.p00.y);
    CGPoint point10 = CGPointMake(pRect.p10.x, pRect.p10.y);
    CGPoint point11 = CGPointMake(pRect.p11.x, pRect.p11.y);
    CGPoint point01 = CGPointMake(pRect.p01.x, pRect.p01.y);
    
    // Convert the points from the pixel reference frame to the screen reference frame
    
    // Calculate the scale factor between the original image and the view size
    int dimOverlayLarge = MAX(self.widthOverlay, self.heightOverlay);
    int dimOverlaySmall = MIN(self.widthOverlay, self.heightOverlay);
    int dimOrigLarge = MAX(self.widthOrig, self.heightOrig);
    int dimOrigSmall = MIN(self.widthOrig, self.heightOrig);
    float scaleL = (float)dimOverlayLarge/(float)dimOrigLarge;
    float scaleS = (float)dimOverlaySmall/(float)dimOrigSmall;
    float scale = MAX(scaleL, scaleS);
    
    // First translate the points to the common center point of the screen
    CGPoint point00_t1 = CGPointMake(point00.x - self.widthOrig/2.0, point00.y - self.heightOrig/2.0);
    CGPoint point10_t1 = CGPointMake(point10.x - self.widthOrig/2.0, point10.y - self.heightOrig/2.0);
    CGPoint point11_t1 = CGPointMake(point11.x - self.widthOrig/2.0, point11.y - self.heightOrig/2.0);
    CGPoint point01_t1 = CGPointMake(point01.x - self.widthOrig/2.0, point01.y - self.heightOrig/2.0);
    
    // Scale the points to match the size of the screen
    CGPoint point00_s1 = CGPointMake(scale*point00_t1.x, scale*point00_t1.y);
    CGPoint point10_s1 = CGPointMake(scale*point10_t1.x, scale*point10_t1.y);
    CGPoint point11_s1 = CGPointMake(scale*point11_t1.x, scale*point11_t1.y);
    CGPoint point01_s1 = CGPointMake(scale*point01_t1.x, scale*point01_t1.y);
    
    // Rotate the points 90 degrees to match the orientation of the screen reference frame
    CGPoint point00_r1 = CGPointMake(-point00_s1.y, point00_s1.x);
    CGPoint point10_r1 = CGPointMake(-point10_s1.y, point10_s1.x);
    CGPoint point11_r1 = CGPointMake(-point11_s1.y, point11_s1.x);
    CGPoint point01_r1 = CGPointMake(-point01_s1.y, point01_s1.x);
    
    // Translate the points to the origin of the screen reference frame
    CGPoint point00_t2 = CGPointMake(point00_r1.x + self.widthOverlay/2.0, point00_r1.y + self.heightOverlay/2.0);
    CGPoint point10_t2 = CGPointMake(point10_r1.x + self.widthOverlay/2.0, point10_r1.y + self.heightOverlay/2.0);
    CGPoint point11_t2 = CGPointMake(point11_r1.x + self.widthOverlay/2.0, point11_r1.y + self.heightOverlay/2.0);
    CGPoint point01_t2 = CGPointMake(point01_r1.x + self.widthOverlay/2.0, point01_r1.y + self.heightOverlay/2.0);
    
    // Add the points to the perspectiveRectArray after they've been converted to screen points
    [self.perspectiveRectArray addObject:[NSValue valueWithCGPoint:point00_t2]];
    [self.perspectiveRectArray addObject:[NSValue valueWithCGPoint:point10_t2]];
    [self.perspectiveRectArray addObject:[NSValue valueWithCGPoint:point11_t2]];
    [self.perspectiveRectArray addObject:[NSValue valueWithCGPoint:point01_t2]];
}
#endif


// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect {
    
    // If the perspectiveRectArray is defined, then draw it on the screen
    if (self.perspectiveRectArray != nil && [self.perspectiveRectArray count] > 3) {
        
        // Get the current graphics context
        CGContextRef context = UIGraphicsGetCurrentContext();
        
        // Get the points from the perspectiveRectArray
        CGPoint point00 = [[self.perspectiveRectArray objectAtIndex:0] CGPointValue];
        CGPoint point10 = [[self.perspectiveRectArray objectAtIndex:1] CGPointValue];
        CGPoint point11 = [[self.perspectiveRectArray objectAtIndex:2] CGPointValue];
        CGPoint point01 = [[self.perspectiveRectArray objectAtIndex:3] CGPointValue];
        
        CGMutablePathRef perspectivePath = CGPathCreateMutable();
        CGPathMoveToPoint(perspectivePath, NULL, point00.x, point00.y);
        CGPathAddLineToPoint(perspectivePath, NULL, point10.x, point10.y);
        CGPathAddLineToPoint(perspectivePath, NULL, point11.x, point11.y);
        CGPathAddLineToPoint(perspectivePath, NULL, point01.x, point01.y);
        CGPathCloseSubpath(perspectivePath);
        CGContextSetFillColorWithColor(context, [self getColorForScanStatus:self.latestStatus].CGColor);
        CGContextAddPath(context, perspectivePath);
        CGContextFillPath(context);
    }
}

// This method retuns the color for the given scan status
-(UIColor *)getColorForScanStatus:(DocScanner::ScanStatus)thisStatus {
    
    switch (thisStatus) {
        case DocScanner::STABLE:
            return [UIColor colorWithRed:(128.0/255.0) green:0 blue:(128.0/255.0) alpha:0.5];
        case DocScanner::DONE:
            return [UIColor colorWithRed:(128.0/255.0) green:0 blue:1 alpha:0.5];
        default:
            return [UIColor colorWithRed:(64.0/255.0) green:1 blue:0 alpha:0.5];
    }
}


@end
