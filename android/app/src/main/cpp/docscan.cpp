#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "docscan.hpp"

const int WORKING_SIZE = 384;

DocScanner::DocScanner()
{

}

void DocScanner::test(int &x)
{
    x += 1;
}

cv::Mat DocScanner::scan(const cv::Mat& imageOrig, std::vector<cv::Vec4i>& lines)
{
    // Determine proportional working size
    //--------------------------------
    int wOrig = imageOrig.cols;
    int hOrig = imageOrig.rows;

    float scaleX = (float)WORKING_SIZE/(float)wOrig;
    float scaleY = (float)WORKING_SIZE/(float)hOrig;
    float scale = (scaleX < scaleY) ? scaleX : scaleY;
    int wResize = (int)(scale * (float)wOrig);
    int hResize = (int)(scale * (float)hOrig);

    // Resize, grayscale and blur
    //--------------------------------
    cv::resize(imageOrig, this->imageResized, cv::Size(wResize, hResize), 0, 0, CV_INTER_LINEAR);
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