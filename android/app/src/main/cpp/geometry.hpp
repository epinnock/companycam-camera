#ifndef CCIP_GEOMETRY_HPP
#define CCIP_GEOMETRY_HPP

#include <vector>

#include <opencv2/core/core.hpp>

namespace geom
{
    struct PerspectiveRect{
        bool valid;
        float correctedWidth;
        float correctedHeight;
        cv::Point2f p00;
        cv::Point2f p10;
        cv::Point2f p11;
        cv::Point2f p01;
    };

    enum LineOrientation { VERTICAL, HORIZONTAL, NEITHER };
    LineOrientation getLineOrientation(const cv::Vec4i& l);

    cv::Point2f pointAlongLine(const cv::Vec4i& l, const float t);
    cv::Point2f intersectLines(const cv::Vec4i& l, const cv::Vec4i& m);
    float intersectHalfPlaneWithBox(const cv::Vec4i& l, int containerW, int containerH);
    float computeAreaX(const cv::Vec4i& l, const int containerW, const int containerH);
    float computeAreaY(const cv::Vec4i& l, const int containerW, const int containerH);

    cv::Vec3f screenToRay(const cv::Point2f& p, const int screenW, const int screenH);
    float det3(const cv::Vec3f& u, const cv::Vec3f& v, const cv::Vec3f& w);
    PerspectiveRect invalidPerspectiveRect();
    PerspectiveRect perspectiveRectFromPoints(
        const cv::Point2f& p00,
        const cv::Point2f& p10,
        const cv::Point2f& p11,
        const cv::Point2f& p01,
        const int screenW,
        const int screenH);
    PerspectiveRect perspectiveRectFromLines(
        const std::vector<cv::Vec4i>& lines,
        const int containerW,
        const int containerH);

    PerspectiveRect getSmoothedRects(
        const std::vector<PerspectiveRect>& rects,
        const int containerW,
        const int containerH);

    cv::Rect perspectiveRectBoundingBox(const PerspectiveRect& rect);
    float minDist(const PerspectiveRect& rect, const cv::Point2f& p);
    float dist(const PerspectiveRect& rectA, const PerspectiveRect& rectB);
}

#endif
