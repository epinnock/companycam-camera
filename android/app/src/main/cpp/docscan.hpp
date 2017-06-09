#ifndef CCIP_DOCSCAN_HPP
#define CCIP_DOCSCAN_HPP

#include <opencv2/core/core.hpp>

#include "geometry.hpp"

class DocScanner
{
    cv::Mat imageResized;
    cv::Mat imageGray;
    cv::Mat imageBlur;
    cv::Mat imageEdges;
    cv::Mat imageOutput;

public:
    DocScanner();
    cv::Mat scan(const cv::Mat& imageOrig);
};

#endif