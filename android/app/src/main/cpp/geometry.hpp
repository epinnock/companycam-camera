#ifndef CCIP_GEOMETRY_HPP
#define CCIP_GEOMETRY_HPP

#include <opencv2/core/core.hpp>

namespace geom
{
    enum LineOrientation {
        VERTICAL,
        HORIZONTAL,
        NEITHER
    };

    typedef struct {
        bool valid;
        cv::Point2f p00;
        cv::Point2f p10;
        cv::Point2f p11;
        cv::Point2f p01;
    } PerspectiveRect;

    LineOrientation getLineOrientation(const cv::Vec4i& l);
    cv::Point2f pointAlongLine(const cv::Vec4i& l, const float t);
    cv::Point2f intersectLines(const cv::Vec4i& l, const cv::Vec4i& m);
    float computeAreaX(const cv::Vec4i& l);
    float computeAreaY(const cv::Vec4i& l);
    PerspectiveRect rectFromLines(const std::vector<cv::Vec4i>& lines);
}

#endif