#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>

#include "geometry.hpp"

#include "imageproc.hpp"

const int DEFAULT_MIN_OUTPUT_DIM = 100;
const int DEFAULT_MAX_OUTPUT_DIM = 1024;

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

    /** Apply four point perspective transform to imageIn.
     * The points are assumed to form a CCW convex quadrilateral; the coords
     * are normalized to [0,1]^2 (i.e., they are not pixel positions!)
     */
    cv::Mat fourPoint(
        const cv::Mat& imageIn,
        const std::vector<cv::Point2f>& points)
    {
        const float optMaxOutputDim = DEFAULT_MAX_OUTPUT_DIM;
        const float optMinOutputDim = DEFAULT_MIN_OUTPUT_DIM;

        // Find top-left point and re-order
        float minDiag;
        int minIndex;
        for (int i = 0; i < 4; i++) {
            const cv::Point2f& p = points[i];
            float valDiag = p.x + p.y;
            if (i == 0 || valDiag < minDiag) {
                minDiag = valDiag;
                minIndex = i;
            }
        }

        std::cout << "Min index: " << minIndex << std::endl;

        // TODO Scale to image size
        const int imgH = imageIn.rows;
        const int imgW = imageIn.cols;

        std::cout << "Image size: " << imgW << " x " << imgH << std::endl;

        const int i = minIndex, j = (i + 1) % 4, k = (i + 2) % 4, l = (i + 3) % 4;
        const cv::Point2f p00 = geom::scaleElementwise(points[i], imgW, imgH);
        const cv::Point2f p01 = geom::scaleElementwise(points[j], imgW, imgH);
        const cv::Point2f p11 = geom::scaleElementwise(points[k], imgW, imgH);
        const cv::Point2f p10 = geom::scaleElementwise(points[l], imgW, imgH);

        // Get perspectiveRect
        const geom::PerspectiveRect pRect = geom::perspectiveRectFromPoints(
            p00, p10, p11, p01,
            imgW, imgH);

        std::cout << "Corrected size: " << pRect.correctedWidth << " x " << pRect.correctedHeight << std::endl;

        // Don't bother creating image bigger than the bounding box; also limit to optMaxOutputDim
        cv::Rect rectBounds = perspectiveRectBoundingBox(pRect);
        const float outputSize = fmin(optMaxOutputDim, fmax(rectBounds.width, rectBounds.height));

        // Now scale the ratio [corrW:corrH] to have max dim 'outputSize'
        const float correctedScale = outputSize / fmax(pRect.correctedWidth, pRect.correctedHeight);
        int outputW = correctedScale * pRect.correctedWidth;
        int outputH = correctedScale * pRect.correctedHeight;

        std::cout << "Output size: " << outputW << " x " << outputH << std::endl;

        // If something has gone wrong with the rect, just clamp it
        outputW = geom::clamp(outputW, optMinOutputDim, optMaxOutputDim);
        outputH = geom::clamp(outputH, optMinOutputDim, optMaxOutputDim);

        std::vector<cv::Point2f> rectPerspective = {
            pRect.p00,
            pRect.p01,
            pRect.p11,
            pRect.p10 };
        std::vector<cv::Point2f> rectTarget = {
            cv::Point2f(0,0),
            cv::Point2f(0,outputH),
            cv::Point2f(outputW,outputH),
            cv::Point2f(outputW,0)
        };

        // TODO option to use existing Mat container for output
        cv::Mat imageOutputContainer;
        imageOutputContainer.create(cv::Size(outputW,outputH), imageIn.type());
        cv::Rect outputTarget(0,0,outputW,outputH);
        cv::Mat imageOut = imageOutputContainer(outputTarget);

        const cv::Mat perspectiveTransform = cv::getPerspectiveTransform(rectPerspective, rectTarget);
        cv::warpPerspective(
            imageIn,
            imageOut,
            perspectiveTransform,
            cv::Size(outputW, outputH));

        return imageOut;
    }
}
