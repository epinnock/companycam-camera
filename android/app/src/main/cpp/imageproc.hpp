#ifndef CCIP_IMAGEPROC_HPP
#define CCIP_IMAGEPROC_HPP

#include <opencv2/core/core.hpp>

#include "geometry.hpp"

namespace imageproc
{
    // struct PerspectiveRect{
    //     bool valid;
    //     float correctedWidth;
    //     float correctedHeight;
    //     cv::Point2f p00;
    //     cv::Point2f p10;
    //     cv::Point2f p11;
    //     cv::Point2f p01;
    // };

    // fourPoint(
    //     const geom::PerspectiveRect& pRect
    // )

    cv::Mat magicColor(const cv::Mat& imageIn);
}

#endif
