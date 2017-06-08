#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "docscan.hpp"
#include "geometry.hpp"

const int WORKING_SIZE = 384;

DocScanner::DocScanner()
{

}

cv::Mat DocScanner::findLines(const cv::Mat& imageOrig, std::vector<cv::Vec4i>& lines)
{
    // Determine proportional working size
    //--------------------------------
    int wOrig = imageOrig.cols;
    int hOrig = imageOrig.rows;

    float scaleX = (float)WORKING_SIZE/(float)wOrig;
    float scaleY = (float)WORKING_SIZE/(float)hOrig;
    float scale = (scaleX < scaleY) ? scaleX : scaleY;
    float wResize = (int)(scale * (float)wOrig);
    float hResize = (int)(scale * (float)hOrig);
    this->scaleResizeToOrig = 1/scale;

    // Resize, grayscale and blur
    //--------------------------------
    cv::resize(imageOrig, this->imageResized, cv::Size(wResize,hResize), 0, 0, CV_INTER_LINEAR);
    cv::cvtColor(this->imageResized, this->imageGray, CV_BGR2GRAY);
    cv::GaussianBlur(this->imageGray, this->imageBlur, cv::Size(9,9), 0, 0);

    // Canny
    //--------------------------------
    //threshold1 – first threshold for the hysteresis procedure.
    //threshold2 – second threshold for the hysteresis procedure.
    double cannyThresh1 = 24;
    double cannyThresh2 = 3*cannyThresh1;
    cv::Canny(this->imageBlur, this->imageEdges, cannyThresh1, cannyThresh2);

    // Hough
    //--------------------------------
    //threshold – Accumulator threshold parameter. Only those lines are returned that get enough votes ( >\texttt{threshold} ).
    //minLineLength – Minimum line length. Line segments shorter than that are rejected.
    //maxLineGap – Maximum allowed gap between points on the same line to link them.
    int houghThreshold = 50;
    double houghMinLength = 50;
    double houghMaxGap = 40;
    cv::HoughLinesP(this->imageEdges, lines, 1, CV_PI/180, houghThreshold, houghMinLength, houghMaxGap);

    return this->imageResized;
}

cv::Mat DocScanner::perspectiveTransform(const cv::Mat& imageSource, const geom::PerspectiveRect& rect)
{
    cv::Mat imageTransformed;
    const int targetW = 800;
    const int targetH = 500;

    std::vector<cv::Point2f> rectPerspective;
    rectPerspective.push_back( rect.p00 * this->scaleResizeToOrig );
    rectPerspective.push_back( rect.p01 * this->scaleResizeToOrig );
    rectPerspective.push_back( rect.p11 * this->scaleResizeToOrig );
    rectPerspective.push_back( rect.p10 * this->scaleResizeToOrig );

    std::vector<cv::Point2f> rectTarget;
    rectTarget.push_back( cv::Point2f(0,0) );
    rectTarget.push_back( cv::Point2f(0,targetH) );
    rectTarget.push_back( cv::Point2f(targetW,targetH) );
    rectTarget.push_back( cv::Point2f(targetW,0) );

    cv::Mat m = cv::getPerspectiveTransform(rectPerspective, rectTarget);
    cv::warpPerspective(imageSource, imageTransformed, m, cv::Size(targetW,targetH));

    return imageTransformed;
}

cv::Mat DocScanner::scan(const cv::Mat& imageOrig){

    std::vector<cv::Vec4i> lines;
    this->findLines(imageOrig, lines);

    geom::PerspectiveRect rect = geom::rectFromLines(lines);

    return this->perspectiveTransform(imageOrig, rect);
}
