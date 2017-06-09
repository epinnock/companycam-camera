#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "docscan.hpp"
#include "geometry.hpp"

const int WORKING_SIZE = 384;

DocScanner::DocScanner()
{

}

cv::Mat DocScanner::scan(const cv::Mat& imageOrig)
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
        this->imageResized,
        cv::Size(wResize, hResize),
        0,
        0,
        CV_INTER_LINEAR);
    cv::cvtColor(this->imageResized, this->imageGray, CV_BGR2GRAY);
    cv::GaussianBlur(this->imageGray, this->imageBlur, cv::Size(9,9), 0, 0);

    // Canny
    //--------------------------------
    //threshold1 – first threshold for the hysteresis procedure.
    //threshold2 – second threshold for the hysteresis procedure.
    const double cannyThresh1 = 24;
    const double cannyThresh2 = 3*cannyThresh1;
    cv::Canny(this->imageBlur, this->imageEdges, cannyThresh1, cannyThresh2);

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
        this->imageEdges,
        lines,
        1,
        CV_PI/180,
        houghThreshold,
        houghMinLength,
        houghMaxGap);

    // Find perspective rect
    //--------------------------------
    const geom::PerspectiveRect rect = geom::rectFromLines(lines, wResize, hResize);
    if (!rect.valid) {
        //TODO
        return this->imageResized;
    }

    // Perspective correction
    //--------------------------------
    const int outputW = 500 * rect.correctedWidth;
    const int outputH = 500 * rect.correctedHeight;

    std::vector<cv::Point2f> rectPerspective;
    rectPerspective.push_back( rect.p00 / scale );
    rectPerspective.push_back( rect.p01 / scale );
    rectPerspective.push_back( rect.p11 / scale );
    rectPerspective.push_back( rect.p10 / scale );

    std::vector<cv::Point2f> rectTarget;
    rectTarget.push_back( cv::Point2f(0,0) );
    rectTarget.push_back( cv::Point2f(0,outputH) );
    rectTarget.push_back( cv::Point2f(outputW,outputH) );
    rectTarget.push_back( cv::Point2f(outputW,0) );

    const cv::Mat m = cv::getPerspectiveTransform(rectPerspective, rectTarget);
    cv::warpPerspective(
        imageOrig,
        this->imageOutput,
        m,
        cv::Size(outputW, outputH));

    return this->imageOutput;
}