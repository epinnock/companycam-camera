#ifndef CCIP_IMAGEPROC_HPP
#define CCIP_IMAGEPROC_HPP

#include <opencv2/core/core.hpp>

namespace imageproc
{
    cv::Mat magicColor(const cv::Mat& imageIn);
    cv::Mat fourPoint(
        const cv::Mat& imageIn,
        const std::vector<cv::Point2f>& points);
}

#endif
