#include <math.h>
#include <memory>
#include <iostream>
#include <chrono>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "docscan.hpp"
#include "geometry.hpp"

const int DEFAULT_WORKING_SIZE = 384;
const int DEFAULT_MAX_OUTPUT_DIM = 1024;
const float MAX_STABLE_DEVIATION_PCT = 0.03;
const unsigned long REQUIRED_STABLE_DURATION_MS = 500;

DocScanner::DocScanner(const int optWorkingSize, const int optMaxOutputDim) :
    optWorkingSize(optWorkingSize),
    optMaxOutputDim(optMaxOutputDim),
    didGenerateOutput(false),
    timeLastUnstable(std::chrono::high_resolution_clock::now()),
    pRect(geom::invalidPerspectiveRect())
{ }

DocScanner::DocScanner() :
    DocScanner(DEFAULT_WORKING_SIZE, DEFAULT_MAX_OUTPUT_DIM)
{ }

/** Returns the most recent debug image. */
cv::Mat DocScanner::getDebugImage() const
{
    return imageResized;
}

/** Returns the most recent output image. */
cv::Mat DocScanner::getOutputImage() const
{
    return imageOutput;
}

/** Returns the most recent {@link PerspectiveRect}. */
geom::PerspectiveRect DocScanner::getPerspectiveRect() const
{
    return pRect;
}

/** Resets {@link smartScan}; i.e., set the status back to UNSTABLE. */
void DocScanner::reset()
{
    didGenerateOutput = false;
    timeLastUnstable = std::chrono::high_resolution_clock::now();
    pRect = geom::invalidPerspectiveRect();
}

/** Scans imageOrig and determines whether the new {@link PerspectiveRect}
 * differs much from the previous one:  Returns UNSTABLE if they differ and
 * STABLE if they are similar, unless the result has been STABLE for a threshold
 * amount of time, in which case DONE is returned and {@link getOutputImage} is updated.
 */
DocScanner::ScanStatus DocScanner::smartScan(const cv::Mat& imageOrig)
{
    const auto timeNow = std::chrono::high_resolution_clock::now();

    const geom::PerspectiveRect pRectOld = pRect;
    scan(imageOrig, false);

    bool isStable = false;
    if (pRect.valid && pRectOld.valid) {
        const float maxDeviation = fmax(imageOrig.rows, imageOrig.cols) * MAX_STABLE_DEVIATION_PCT;
        isStable = geom::dist(pRect, pRectOld) < maxDeviation;
    }

    if (!isStable) {
        didGenerateOutput = false;
        timeLastUnstable = timeNow;
        return DocScanner::UNSTABLE;
    }

    const unsigned long msStable = std::chrono::duration_cast<std::chrono::milliseconds>(timeNow-timeLastUnstable).count();
    if (msStable > REQUIRED_STABLE_DURATION_MS) {
        if (!didGenerateOutput) {
            scan(imageOrig, true);
            didGenerateOutput = true;
        }
        return DocScanner::DONE;
    } else {
        return DocScanner::STABLE;
    }
}

/** Performs a single scan of imageOrig.  The results of
 * {@link getPerspectiveRect} and {@link getDebugImage} will be updated, even
 * if the scan was unsuccessful.  The result of {@link getOutputImage} will
 * only be updated if doGenerateOutput is true.
 */
void DocScanner::scan(const cv::Mat& imageOrig, const bool doGenerateOutput)
{
    // Determine proportional working size
    //--------------------------------
    const int wOrig = imageOrig.cols;
    const int hOrig = imageOrig.rows;

    const float scaleX = (float)optWorkingSize/(float)wOrig;
    const float scaleY = (float)optWorkingSize/(float)hOrig;
    const float scale = (scaleX < scaleY) ? scaleX : scaleY;
    const int wResize = scale * wOrig;
    const int hResize = scale * hOrig;

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

    pRect = geom::perspectiveRectFromLines(lines, wResize, hResize);
    // Quit early if no perspective rect found
    if (!pRect.valid) { return; }

    cv::line(imageResized, pRect.p00, pRect.p10, colorPink, 2);
    cv::line(imageResized, pRect.p10, pRect.p11, colorPink, 2);
    cv::line(imageResized, pRect.p11, pRect.p01, colorPink, 2);
    cv::line(imageResized, pRect.p01, pRect.p00, colorPink, 2);

    // pRect is scaled for imageResized; scale it for imageOrig instead
    pRect.p00 /= scale;
    pRect.p10 /= scale;
    pRect.p11 /= scale;
    pRect.p01 /= scale;

    // Perspective correction
    //--------------------------------
    // Only carry out this final step if requested
    if (!doGenerateOutput) { return; }

    // Don't bother creating image bigger than the bounding box; also limit to optMaxOutputDim
    cv::Rect rectBounds = perspectiveRectBoundingBox(pRect);
    const float outputSize = fmin(optMaxOutputDim, fmax(rectBounds.width, rectBounds.height) / scale);

    // Now scale the ratio [corrW:corrH] to have max dim 'outputSize'
    const float correctedScale = outputSize / fmax(pRect.correctedWidth, pRect.correctedHeight);
    const int outputW = correctedScale * pRect.correctedWidth;
    const int outputH = correctedScale * pRect.correctedHeight;

    std::vector<cv::Point2f> rectPerspective = { pRect.p00, pRect.p01, pRect.p11, pRect.p10 };
    std::vector<cv::Point2f> rectTarget = {
        cv::Point2f(0,0),
        cv::Point2f(0,outputH),
        cv::Point2f(outputW,outputH),
        cv::Point2f(outputW,0)
    };

    // Since this method will be called many times with different imageOutput
    // sizes, create a fixed-size imageOutputContainer which will *not* be
    // re-allocated every frame, and then write to an ROI of the desired size.
    imageOutputContainer.create(cv::Size(optMaxOutputDim,optMaxOutputDim), imageOrig.type());
    cv::Rect outputTarget(0,0,outputW,outputH);
    imageOutput = imageOutputContainer(outputTarget);

    const cv::Mat perspectiveTransform = cv::getPerspectiveTransform(rectPerspective, rectTarget);
    cv::warpPerspective(
        imageOrig,
        imageOutput,
        perspectiveTransform,
        cv::Size(outputW, outputH));
}
