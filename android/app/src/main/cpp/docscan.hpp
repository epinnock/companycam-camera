#ifndef CCIP_DOCSCAN_HPP
#define CCIP_DOCSCAN_HPP

#include <chrono>

#include <opencv2/core/core.hpp>

#include "geometry.hpp"

class DocScanner
{
    protected:
        const int optWorkingSize;
        const int optMaxOutputDim;
        unsigned long optStableDurationMS;

        bool didGenerateOutput;

        // Updated every scan (i.e., every camera frame)
        cv::Mat imageResized;
        cv::Mat imageGray;
        cv::Mat imageBlur;
        cv::Mat imageCanny;
        cv::Mat imageEdges;
        geom::PerspectiveRect pRect;
        std::vector<geom::PerspectiveRect> recentRects;
        int recentRectsIndex;
        std::chrono::time_point<std::chrono::high_resolution_clock> timeLastUnstable;

        // Updated only on output generation
        cv::Mat imageOutputContainer;
        cv::Mat imageOutput;
        geom::PerspectiveRect pRectOutput;

    public:
        DocScanner();
        DocScanner(const int optWorkingSize, const int optMaxOutputDim);

        enum ScanStatus { UNSTABLE, STABLE, DONE };
        ScanStatus smartScan(const cv::Mat& imageOrig);

        void setStableDurationMS(const unsigned long ms);
        void reset();
        bool scan(const cv::Mat& imageOrig, const bool doGenerateOutput);

        geom::PerspectiveRect getPerspectiveRect() const;
        cv::Mat getDebugImage() const;

        geom::PerspectiveRect getOutputPerspectiveRect() const;
        cv::Mat getOutputImage() const;
};

#endif
