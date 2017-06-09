#include <opencv2/core/core.hpp>
#include <math.h>

#include "geometry.hpp"

namespace geom
{
    const float ORIENTATION_THRESHOLD = 0.5;
    const float CAM_FOVY_DEG = 50;

    LineOrientation getLineOrientation(const cv::Vec4i& l)
    {
        float dy = l[3] - l[1];
        float dx = l[2] - l[0];
        float ang = atan2(dy, dx);

        float vx = fabs(cos(ang));
        if (vx < ORIENTATION_THRESHOLD) { return VERTICAL; }

        float vy = fabs(sin(ang));
        if (vy < ORIENTATION_THRESHOLD) { return HORIZONTAL; }

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

    // Intersect the region above the line 'l' and the interior of
    // the box [0,containerW]x[0,containerH].  Return the area.
    float intersectHalfPlaneWithBox(
        const cv::Vec4i& l,
        int containerW,
        int containerH)
    {
        // Intersect the line 'l' with the left and right edges of container
        cv::Vec4i edgeX0(0, 0, 0, containerH);
        cv::Vec4i edgeX1(containerW, 0, containerW, containerH);

        cv::Point2f pointX0 = intersectLines(l, edgeX0);
        cv::Point2f pointX1 = intersectLines(l, edgeX1);

        bool hitX0 = (0 < pointX0.y) && (pointX0.y < containerH);
        bool hitX1 = (0 < pointX1.y) && (pointX1.y < containerH);

        // If hit both left and right edges, return area of quadrilateral above
        if (hitX0 && hitX1) {
            return containerW * 0.5 * (pointX0.y + pointX1.y);
        }

        // Otherwise, check for intersections with top and bottom of container
        cv::Vec4i edgeY0(0, 0, containerW, 0);
        cv::Vec4i edgeY1(0, containerH, containerW, containerH);

        cv::Point2f pointY0 = intersectLines(l, edgeY0);
        cv::Point2f pointY1 = intersectLines(l, edgeY1);

        bool hitY0 = (0 < pointY0.x) && (pointY0.x < containerW);
        bool hitY1 = (0 < pointY1.x) && (pointY1.x < containerW);

        // Case 1/4: Hit top and left
        if (hitX0 && hitY0) {
            float triW = pointY0.x;
            float triH = pointX0.y;
            return 0.5 * triW * triH;
        }

        // Case 2/4: Hit top and right
        if (hitX1 && hitY0) {
            float triW = containerW - pointY0.x;
            float triH = pointX0.y;
            return 0.5 * triW * triH;
        }

        // Case 3/4: Hit bottom and left
        if (hitX0 && hitY1) {
            float triW = pointY0.x;
            float triH = containerH - pointX0.y;
            return containerW * containerH - 0.5 * triW * triH;
        }

        // Case 4/4: Hit bottom and right
        if (hitX1 && hitY1) {
            float triW = containerW - pointY0.x;
            float triH = containerH - pointX0.y;
            return containerW * containerH - 0.5 * triW * triH;
        }

        // If no case has matched so far, intersection is all or nothing
        return (pointX0.y < 0) ? 0 : containerW * containerH;
    }

    // The horizontal lines with min and max value returned by this function
    // are regarded as the top and bottom sides of the document to scan.
    float computeAreaY(const cv::Vec4i& l, int containerW, int containerH)
    {
        return intersectHalfPlaneWithBox(l, containerW, containerH);
    }

    // The vertical lines with min and max value returned by this function
    // are regarded as the left and right sides of the document to scan.
    float computeAreaX(const cv::Vec4i& l, int containerW, int containerH)
    {
        cv::Vec4i lFlippedXY(l[1], l[0], l[3], l[2]);
        return intersectHalfPlaneWithBox(lFlippedXY, containerH, containerW);
    }

    PerspectiveRect invalidPerspectiveRect()
    {
        PerspectiveRect rect;
        rect.valid = false;
        return rect;
    }

    cv::Vec3f screenToRay(const cv::Point2f& p, int screenW, int screenH)
    {
        float tx = p.x / screenW;
		float ty = p.y / screenH;
        float camAspect = (float)screenW/(float)screenH;

		float h = tan(CAM_FOVY_DEG * 0.5 * M_PI/180);
		float rx = (-1.0f + 2.0f*tx) * h*camAspect;
		float ry = (-1.0f + 2.0f*ty) * h;
		float rz = -1.0f;

        return cv::Vec3f(rx, ry, rz);
    }

    float det3(const cv::Vec3f& u, const cv::Vec3f& v, const cv::Vec3f& w)
    {
        return (
			u[0] * ( v[1]*w[2] - v[2]*w[1] ) -
			u[1] * ( v[0]*w[2] - v[2]*w[0] ) +
			u[2] * ( v[0]*w[1] - v[1]*w[0] )
        );
    }

    PerspectiveRect perspectiveRectFromPoints(
        const cv::Point2f& p00,
        const cv::Point2f& p10,
        const cv::Point2f& p11,
        const cv::Point2f& p01,
        int screenW,
        int screenH)
    {
        PerspectiveRect rect = {true, 0, 0, p00, p10, p11, p01};

        // Each point ti*vi will be a corner of the 3D rectangle
        cv::Vec3f v0 = screenToRay(p00, screenW, screenH);
        cv::Vec3f v1 = screenToRay(p10, screenW, screenH);
        cv::Vec3f v2 = screenToRay(p11, screenW, screenH);
        cv::Vec3f v3 = screenToRay(p01, screenW, screenH);

		float d130 = det3(v1, v3, v0);
		float d132 = det3(v1, v3, v2);
		float d230 = det3(v2, v3, v0);
		float d201 = det3(v2, v0, v1);

        float t3 = 1;
		float t1 =     d230/d201;
		float t0 = -t1*d132/d230;
		float t2 =  t1*d130/d230;

        // Get the dimensions of the 3D rectangle
        rect.correctedWidth = norm(t1*v1 - t0*v0);
        rect.correctedHeight = norm(t3*v3 - t0*v0);

        return rect;
    }

    PerspectiveRect rectFromLines(
        const std::vector<cv::Vec4i>& lines,
        int containerW,
        int containerH)
    {
        bool xValid = false;
        float minAreaX, maxAreaX;
        cv::Vec4i lineX0, lineX1;

        bool yValid = false;
        float minAreaY, maxAreaY;
        cv::Vec4i lineY0, lineY1;

        for (size_t i=0; i<lines.size(); i++) {
            cv::Vec4i l = lines[i];
            LineOrientation orientation = getLineOrientation(l);

            if (orientation == VERTICAL) {
                float areaX = computeAreaX(l, containerW, containerH);

                if (!xValid || (areaX < minAreaX)) {
                    minAreaX = areaX;
                    lineX0 = l;
                }
                if (!xValid || (areaX > maxAreaX)) {
                    maxAreaX = areaX;
                    lineX1 = l;
                }
                xValid = true;

            }else if (orientation == HORIZONTAL) {
                float areaY = computeAreaY(l, containerW, containerH);

                if (!yValid || (areaY < minAreaY)) {
                    minAreaY = areaY;
                    lineY0 = l;
                }
                if (!yValid || (areaY > maxAreaY)) {
                    maxAreaY = areaY;
                    lineY1 = l;
                }
                yValid = true;
            }
        }

        // If not successful, return 'invalid' PerspectiveRect
        if (!xValid || !yValid || (lineX0 == lineX1) && (lineY0 == lineY1)) {
            return invalidPerspectiveRect();
        }

        cv::Point2f p00 = intersectLines(lineX0, lineY0);
        cv::Point2f p10 = intersectLines(lineX1, lineY0);
        cv::Point2f p11 = intersectLines(lineX1, lineY1);
        cv::Point2f p01 = intersectLines(lineX0, lineY1);
        return perspectiveRectFromPoints(
            p00, p10, p11, p01,
            containerW,
            containerH);
    }
}