#ifndef CCIP_DOCSCAN_HPP
#define CCIP_DOCSCAN_HPP

#include <opencv2/core/core.hpp>

#include "geometry.hpp"

class DocScanner
{
    float scaleResizeToOrig;
    cv::Mat imageResized;
    cv::Mat imageGray;
    cv::Mat imageBlur;
    cv::Mat imageEdges;

public:
    DocScanner();
    cv::Mat scan(const cv::Mat& imageOrig);
    cv::Mat perspectiveTransform(const cv::Mat& imageSource, const geom::PerspectiveRect& rect);
    cv::Mat findLines(const cv::Mat& imageOrig, std::vector<cv::Vec4i>& lines);
};

#endif