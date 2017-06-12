#include <math.h>
#include <memory>
#include <iostream>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "docscan.hpp"
#include "geometry.hpp"

const int WORKING_SIZE = 384;
const int MAX_OUTPUT_DIM = 1024;

DocScanner::DocScanner()
{

}

cv::Mat DocScanner::getDebugImage()
{
    return imageResized;
}

cv::Mat DocScanner::getOutputImage()
{
    return imageOutput;
}

geom::PerspectiveRect DocScanner::scan(const cv::Mat& imageOrig, const bool doGenerateOutput)
{
    // Determine proportional working size
    //--------------------------------
    const int wOrig = imageOrig.cols;
    const int hOrig = imageOrig.rows;

    const float scaleX = (float)WORKING_SIZE/(float)wOrig;
    const float scaleY = (float)WORKING_SIZE/(float)hOrig;
    const float scale = (scaleX < scaleY) ? scaleX : scaleY;
    const int wResize = (int)(scale * (float)wOrig);
    const int hResize = (int)(scale * (float)hOrig);

    // Resize, grayscale and blur
    //--------------------------------
    cv::resize(
        imageOrig,
        imageResized,
        cv::Size(wResize, hResize),
        0,
        0,
        CV_INTER_LINEAR);
    cv::cvtColor(imageResized, imageGray, CV_BGR2GRAY);
    cv::GaussianBlur(imageGray, imageBlur, cv::Size(9,9), 0, 0);

    // Canny
    //--------------------------------
    //threshold1 – first threshold for the hysteresis procedure.
    //threshold2 – second threshold for the hysteresis procedure.
    const double cannyThresh1 = 24;
    const double cannyThresh2 = 3*cannyThresh1;
    cv::Canny(imageBlur, imageEdges, cannyThresh1, cannyThresh2);

    // Hough
    //--------------------------------
    std::vector<cv::Vec4i> lines;

    //threshold – Accumulator threshold parameter. Only those lines are returned that get enough votes ( >\texttt{threshold} ).
    //minLineLength – Minimum line length. Line segments shorter than that are rejected.
    //maxLineGap – Maximum allowed gap between points on the same line to link them.
    const int houghThreshold = 50;
    const double houghMinLength = 50;
    const double houghMaxGap = 40;
    cv::HoughLinesP(
        imageEdges,
        lines,
        1,
        CV_PI/180,
        houghThreshold,
        houghMinLength,
        houghMaxGap);

    // Find perspective rect and debug draw
    //--------------------------------
    cv::Scalar colorRed(0,0,255);
    cv::Scalar colorPink(255,0,255);

    for (const auto& l : lines) {
        cv::line(imageResized, cv::Point(l[0], l[1]), cv::Point(l[2], l[3]), colorRed, 1);
    }

    const geom::PerspectiveRect pRect = geom::perspectiveRectFromLines(lines, wResize, hResize);
    // Quit early if no perspective rect found
    if (!pRect.valid) { return pRect; }

    cv::line(imageResized, pRect.p00, pRect.p10, colorPink, 2);
    cv::line(imageResized, pRect.p10, pRect.p11, colorPink, 2);
    cv::line(imageResized, pRect.p11, pRect.p01, colorPink, 2);
    cv::line(imageResized, pRect.p01, pRect.p00, colorPink, 2);

    // Perspective correction
    //--------------------------------
    // Only carry out this final step if requested
    if (!doGenerateOutput) {
        return pRect;
    }

    // Don't bother creating image bigger than the bounding box; also limit to MAX_OUTPUT_DIM
    cv::Rect rectBounds = perspectiveRectBoundingBox(pRect);
    const float outputSize = fmin(MAX_OUTPUT_DIM, fmax(rectBounds.width, rectBounds.height) / scale);

    // Now scale the ratio [corrW:corrH] to have max dim 'outputSize'
    const float correctedScale = outputSize / fmax(pRect.correctedWidth, pRect.correctedHeight);
    const int outputW = correctedScale * pRect.correctedWidth;
    const int outputH = correctedScale * pRect.correctedHeight;

    std::vector<cv::Point2f> rectPerspective = {
        pRect.p00 / scale,
        pRect.p01 / scale,
        pRect.p11 / scale,
        pRect.p10 / scale
    };
    std::vector<cv::Point2f> rectTarget = {
        cv::Point2f(0,0),
        cv::Point2f(0,outputH),
        cv::Point2f(outputW,outputH),
        cv::Point2f(outputW,0)
    };

    // Since this method will be called many times with different imageOutput
    // sizes, create a fixed-size imageOutputContainer which will *not* be
    // re-allocated every frame, and then write to an ROI of the desired size.
    imageOutputContainer.create(cv::Size(MAX_OUTPUT_DIM,MAX_OUTPUT_DIM), imageOrig.type());
    cv::Rect outputTarget(0,0,outputW,outputH);
    imageOutput = imageOutputContainer(outputTarget);

    const cv::Mat perspectiveTransform = cv::getPerspectiveTransform(rectPerspective, rectTarget);
    cv::warpPerspective(
        imageOrig,
        imageOutput,
        perspectiveTransform,
        cv::Size(outputW, outputH));

    return pRect;
}