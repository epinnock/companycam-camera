#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "imageproc.hpp"

/** Assorted image processing functions. */
namespace imageproc
{
    cv::Mat magicColor(const cv::Mat& imageIn)
    {
        cv::Mat imageGray;
        cv::cvtColor(imageIn, imageGray, CV_BGR2GRAY);

        cv::Mat imageGray2;
        const int blockSize = 13;
        const double meanAdjust = 10;
        const int adaptiveMethod = cv::ADAPTIVE_THRESH_MEAN_C;
        const int thresholdType = cv::THRESH_BINARY;
        cv::adaptiveThreshold(imageGray, imageGray2, 255, adaptiveMethod, thresholdType, blockSize, meanAdjust);

        cv::Mat imageOut;
        cv::cvtColor(imageGray2, imageOut, CV_GRAY2BGRA);

        return imageOut;
    }
}
