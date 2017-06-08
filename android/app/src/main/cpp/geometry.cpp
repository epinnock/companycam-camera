#include <opencv2/core/core.hpp>

#include "geometry.hpp"

namespace geom
{
    const float ORIENTATION_THRESHOLD = 0.5;

    LineOrientation getLineOrientation(const cv::Vec4i& l)
    {
        float dy = l[3] - l[1];
        float dx = l[2] - l[0];
        float ang = atan2(dy, dx);

        float vx = fabs(cos(ang));
        if(vx < ORIENTATION_THRESHOLD){ return VERTICAL; }

        float vy = fabs(sin(ang));
        if(vy < ORIENTATION_THRESHOLD){ return HORIZONTAL; }

        return NEITHER;
    }

    cv::Point2f pointAlongLine(const cv::Vec4i& l, const float t)
    {
        return cv::Point2f(
            l[0] + t * (l[2] - l[0]),
            l[1] + t * (l[3] - l[1])
        );
    }

    cv::Point2f intersectLines(const cv::Vec4i& l, const cv::Vec4i& m)
    {
        float dpx = m[0] - l[0], dpy = m[1] - l[1];
        float vlx = l[2] - l[0], vly = l[3] - l[1];
        float vmx = m[2] - m[0], vmy = m[3] - m[1];

        float num = dpx*vmy - dpy*vmx;
        float det = vlx*vmy - vly*vmx;
        return pointAlongLine(l, num/det);
    }

    float computeAreaX(const cv::Vec4i& l)
    {
        //TODO
        return (float)(l[0] + l[2]) * 0.5;
    }

    float computeAreaY(const cv::Vec4i& l)
    {
        //TODO
        return (float)(l[1] + l[3]) * 0.5;
    }

    PerspectiveRect rectFromLines(const std::vector<cv::Vec4i>& lines)
    {
        bool xValid = false;
        float minAreaX, maxAreaX;
        cv::Vec4i lineX0, lineX1;

        bool yValid = false;
        float minAreaY, maxAreaY;
        cv::Vec4i lineY0, lineY1;

        for(size_t i=0; i<lines.size(); i++){
            cv::Vec4i l = lines[i];
            LineOrientation orientation = getLineOrientation(l);

            if(orientation == VERTICAL){
                float areaX = computeAreaX(l);

                if(!xValid || (areaX < minAreaX)){
                    minAreaX = areaX;
                    lineX0 = l;
                }
                if(!xValid || (areaX > maxAreaX)){
                    maxAreaX = areaX;
                    lineX1 = l;
                }
                xValid = true;

            }else if(orientation == HORIZONTAL){
                float areaY = computeAreaY(l);

                if(!yValid || (areaY < minAreaY)){
                    minAreaY = areaY;
                    lineY0 = l;
                }
                if(!yValid || (areaY > maxAreaY)){
                    maxAreaY = areaY;
                    lineY1 = l;
                }
                yValid = true;
            }
        }

        // Return invalid PerspectiveRect if not successful
        if(!xValid || !yValid || (lineX0 == lineX1) && (lineY0 == lineY1)){
            cv::Point2f p(0,0);
            return (PerspectiveRect){ false, p, p, p, p };
        }

        cv::Point2f p00 = intersectLines(lineX0, lineY0);
        cv::Point2f p10 = intersectLines(lineX1, lineY0);
        cv::Point2f p11 = intersectLines(lineX1, lineY1);
        cv::Point2f p01 = intersectLines(lineX0, lineY1);
        return (PerspectiveRect){ true, p00, p10, p11, p01 };
    }
}