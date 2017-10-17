#include <math.h>

#include <opencv2/core/core.hpp>

#include "geometry.hpp"

/** Assorted geometry utility functions. */
namespace geom
{
    const float ORIENTATION_THRESHOLD = 0.5;
    const float CAM_FOVY_DEG = 50;

    /** Determines whether a line is vertical or horizontal.
     * The x-value (resp. y-value) of the unit tangent vector must be less than
     * {@link ORIENTATION_THRESHOLD} to be considered vertical (resp. horizontal).
     */
    LineOrientation getLineOrientation(const cv::Vec4i& l)
    {
        const float dy = l[3] - l[1];
        const float dx = l[2] - l[0];
        const float ang = atan2(dy, dx);

        const float vx = fabs(cos(ang));
        if (vx < ORIENTATION_THRESHOLD) { return VERTICAL; }

        const float vy = fabs(sin(ang));
        if (vy < ORIENTATION_THRESHOLD) { return HORIZONTAL; }

        return NEITHER;
    }

    /**
     * Linear interpolation along a line l.
     * For the line [x1,y1,x2,y2], t=0 returns (x1,y1) and t=1 returns (x2,y2).
     */
    cv::Point2f pointAlongLine(const cv::Vec4i& l, const float t)
    {
        return cv::Point2f(
            l[0] + t * (l[2] - l[0]),
            l[1] + t * (l[3] - l[1])
        );
    }

    /**
     * Returns the intersection of two lines.
     */
    cv::Point2f intersectLines(const cv::Vec4i& l, const cv::Vec4i& m)
    {
        const float dpx = m[0] - l[0], dpy = m[1] - l[1];
        const float vlx = l[2] - l[0], vly = l[3] - l[1];
        const float vmx = m[2] - m[0], vmy = m[3] - m[1];

        const float num = dpx*vmy - dpy*vmx;
        const float det = vlx*vmy - vly*vmx;
        return pointAlongLine(l, num/det);
    }

    /**
     * Returns the area of the region above the line and within the
      * box [0,containerW]x[0,containerH].
     */
    float intersectHalfPlaneWithBox(
        const cv::Vec4i& l,
        int containerW,
        int containerH)
    {
        // Intersect the line 'l' with the left and right edges of container
        const cv::Vec4i edgeX0(0, 0, 0, containerH);
        const cv::Vec4i edgeX1(containerW, 0, containerW, containerH);

        const cv::Point2f pointX0 = intersectLines(l, edgeX0);
        const cv::Point2f pointX1 = intersectLines(l, edgeX1);

        const bool hitX0 = (0 < pointX0.y) && (pointX0.y < containerH);
        const bool hitX1 = (0 < pointX1.y) && (pointX1.y < containerH);

        // If hit both left and right edges, return area of quadrilateral above
        if (hitX0 && hitX1) {
            return containerW * 0.5 * (pointX0.y + pointX1.y);
        }

        // Otherwise, check for intersections with top and bottom of container
        const cv::Vec4i edgeY0(0, 0, containerW, 0);
        const cv::Vec4i edgeY1(0, containerH, containerW, containerH);

        const cv::Point2f pointY0 = intersectLines(l, edgeY0);
        const cv::Point2f pointY1 = intersectLines(l, edgeY1);

        const bool hitY0 = (0 < pointY0.x) && (pointY0.x < containerW);
        const bool hitY1 = (0 < pointY1.x) && (pointY1.x < containerW);

        // Case 1/4: Hit top and left
        if (hitX0 && hitY0) {
            float triW = pointY0.x;
            float triH = pointX0.y;
            return 0.5 * triW * triH;
        }

        // Case 2/4: Hit top and right
        if (hitX1 && hitY0) {
            float triW = containerW - pointY0.x;
            float triH = pointX1.y;
            return 0.5 * triW * triH;
        }

        // Case 3/4: Hit bottom and left
        if (hitX0 && hitY1) {
            float triW = pointY1.x;
            float triH = containerH - pointX0.y;
            return containerW * containerH - 0.5 * triW * triH;
        }

        // Case 4/4: Hit bottom and right
        if (hitX1 && hitY1) {
            float triW = containerW - pointY1.x;
            float triH = containerH - pointX1.y;
            return containerW * containerH - 0.5 * triW * triH;
        }

        // If no case has matched so far, intersection is all or nothing
        return (pointX0.y < 0) ? 0 : containerW * containerH;
    }

    /** Returns the area above a horizontal line. */
    float computeAreaY(const cv::Vec4i& l, const int containerW, const int containerH)
    {
        return intersectHalfPlaneWithBox(l, containerW, containerH);
    }

    /** Returns the area to the left of a vertical line. */
    float computeAreaX(const cv::Vec4i& l, const int containerW, const int containerH)
    {
        const cv::Vec4i lFlippedXY(l[1], l[0], l[3], l[2]);
        return intersectHalfPlaneWithBox(lFlippedXY, containerH, containerW);
    }

    /** Returns a PerspectiveRect marked as invalid */
    PerspectiveRect invalidPerspectiveRect()
    {
        PerspectiveRect rect;
        rect.valid = false;
        return rect;
    }

    /** Returns the ray that would get projected onto the point p by a camera
     * pointing in the direction [0,0,-1] and with vertical FOV {@link CAM_FOVY_DEG}.
     */
    cv::Vec3f screenToRay(const cv::Point2f& p, const int screenW, const int screenH)
    {
        const float tx = p.x / screenW;
        const float ty = p.y / screenH;
        const float camAspect = (float)screenW/(float)screenH;

        const float h = tan(CAM_FOVY_DEG * 0.5 * M_PI/180);
        return cv::Vec3f(
            (-1.0f + 2.0f*tx) * h*camAspect,
            (-1.0f + 2.0f*ty) * h,
            -1.0f
        );
    }

    /** Returns a 3x3 determinant. */
    float det3(const cv::Vec3f& u, const cv::Vec3f& v, const cv::Vec3f& w)
    {
        return (
            u[0] * ( v[1]*w[2] - v[2]*w[1] ) -
            u[1] * ( v[0]*w[2] - v[2]*w[0] ) +
            u[2] * ( v[0]*w[1] - v[1]*w[0] )
        );
    }

    /** Finds the rectangle with corners that would get projected onto
     * the 4 specified points by the camera described in {@link screenToRay}.
     * Returns a {@link PerspectiveRect} with the specified points and with
     * correctedWidth and correctedHeight equal to the dimensions of that rectangle.
     */
    PerspectiveRect perspectiveRectFromPoints(
        const cv::Point2f& p00,
        const cv::Point2f& p10,
        const cv::Point2f& p11,
        const cv::Point2f& p01,
        const int screenW,
        const int screenH)
    {
        PerspectiveRect rect;
        rect.valid = true;
        rect.p00 = p00;
        rect.p10 = p10;
        rect.p11 = p11;
        rect.p01 = p01;

        // Each point ti*vi will be a corner of the 3D rectangle
        const cv::Vec3f v0 = screenToRay(p00, screenW, screenH);
        const cv::Vec3f v1 = screenToRay(p10, screenW, screenH);
        const cv::Vec3f v2 = screenToRay(p11, screenW, screenH);
        const cv::Vec3f v3 = screenToRay(p01, screenW, screenH);

        const float d130 = det3(v1, v3, v0);
        const float d132 = det3(v1, v3, v2);
        const float d230 = det3(v2, v3, v0);
        const float d201 = det3(v2, v0, v1);

        const float t3 = 1;
        const float t1 =     d230/d201;
        const float t0 = -t1*d132/d230;
        const float t2 =  t1*d130/d230;

        // Get the dimensions of the 3D rectangle
        rect.correctedWidth = norm(t1*v1 - t0*v0);
        rect.correctedHeight = norm(t3*v3 - t0*v0);

        return rect;
    }

    /** Finds the horizontal (resp. vertical) lines that are closest to
     * the top and bottom (resp. left and right) of the container, as determined
     * by {@link computeAreaX} (resp. {@link computeAreaY}).
     * The corners of the resulting rectangle are supplied to
     * {@link perspectiveRectFromPoints} and the result is returned.
     */
    PerspectiveRect perspectiveRectFromLines(
        const std::vector<cv::Vec4i>& lines,
        const int containerW,
        const int containerH)
    {
        bool xValid = false;
        float minAreaX, maxAreaX;
        cv::Vec4i lineX0, lineX1;

        bool yValid = false;
        float minAreaY, maxAreaY;
        cv::Vec4i lineY0, lineY1;

        for (const auto& l : lines){
            const LineOrientation orientation = getLineOrientation(l);

            if (orientation == VERTICAL) {
                const float areaX = computeAreaX(l, containerW, containerH);

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
                const float areaY = computeAreaY(l, containerW, containerH);

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

        const cv::Point2f p00 = intersectLines(lineX0, lineY0);
        const cv::Point2f p10 = intersectLines(lineX1, lineY0);
        const cv::Point2f p11 = intersectLines(lineX1, lineY1);
        const cv::Point2f p01 = intersectLines(lineX0, lineY1);
        return perspectiveRectFromPoints(
            p00, p10, p11, p01,
            containerW,
            containerH);
    }

    /** Returns the screen-space bounding box of a {@link PerpsectiveRect}. */
    cv::Rect perspectiveRectBoundingBox(const PerspectiveRect& rect)
    {
        if (!rect.valid) {
            return cv::Rect(0,0,0,0);
        }

        float minX = fmin(fmin(fmin(rect.p00.x, rect.p10.x), rect.p11.x), rect.p01.x);
        float maxX = fmax(fmax(fmax(rect.p00.x, rect.p10.x), rect.p11.x), rect.p01.x);
        float minY = fmin(fmin(fmin(rect.p00.y, rect.p10.y), rect.p11.y), rect.p01.y);
        float maxY = fmax(fmax(fmax(rect.p00.y, rect.p10.y), rect.p11.y), rect.p01.y);

        return cv::Rect(minX, minY, maxX-minX, maxY-minY);
    }

    /** Return the smallest distance from p to one of the points of rect,
     * or return -1 if rect is not valid.
     */
    float minDist(const PerspectiveRect& rect, const cv::Point2f& p)
    {
        if (!rect.valid){ return -1; }

        float d00 = norm(p - rect.p00);
        float d01 = norm(p - rect.p01);
        float d11 = norm(p - rect.p11);
        float d10 = norm(p - rect.p10);
        return fmin(fmin(fmin(d00, d01), d11), d10);
    }

    /** Each point of rectA is matched up with the closest point of rectB.
     * The largest distance among the 4 pairs is returned.
     */
    float dist(const PerspectiveRect& rectA, const PerspectiveRect& rectB)
    {
        if (!rectA.valid || !rectB.valid) { return -1; }

        float d00 = minDist(rectA, rectB.p00);
        float d01 = minDist(rectA, rectB.p01);
        float d11 = minDist(rectA, rectB.p11);
        float d10 = minDist(rectA, rectB.p10);
        return fmax(fmax(fmax(d00, d01), d11), d10);
    }
}
