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

        cv::Mat imageResized;
        cv::Mat imageGray;
        cv::Mat imageBlur;
        cv::Mat imageCanny;
        cv::Mat imageEdges;
        cv::Mat imageOutputContainer;
        cv::Mat imageOutput;
        geom::PerspectiveRect pRect;

        std::vector<geom::PerspectiveRect> recentRects;
        int recentRectsIndex;

        bool didGenerateOutput;
        std::chrono::time_point<std::chrono::high_resolution_clock> timeLastUnstable;

    public:
        DocScanner();
        DocScanner(const int optWorkingSize, const int optMaxOutputDim);

        enum ScanStatus { UNSTABLE, STABLE, DONE };
        ScanStatus smartScan(const cv::Mat& imageOrig);

        void setStableDurationMS(const unsigned long ms);
        void reset();

        void scan(const cv::Mat& imageOrig, const bool doGenerateOutput);
        cv::Mat getDebugImage() const;
        cv::Mat getOutputImage() const;
        geom::PerspectiveRect getPerspectiveRect() const;
};

#endif
