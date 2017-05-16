package com.newcam.imageprocessing.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import boofcv.alg.filter.binary.Contour;
import georegression.metric.Intersection2D_F32;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;
import georegression.struct.point.Vector2D_F32;

public class GeomUtils {

	public static Contour findBiggestContour(List<Contour> contours){
		int maxAABBArea = 0;
		Contour maxAABBContour = null;
		ExpandingBox box = new ExpandingBox();

		for(Contour c : contours){
			box.reset();
			for(Point2D_I32 p : c.external) {
				box.put(p.getX(), p.getY());
			}
			int area = box.area();
			if(area > maxAABBArea){
				maxAABBArea = area;
				maxAABBContour = c;
			}
		}

		return maxAABBContour;
	}

	public static Point2D_F32 maxInDirection(List<Point2D_F32> points, float vx, float vy){
		float maxdot = 0.0f;
		Point2D_F32 maxp = null;

		int i=0;
		for(Point2D_F32 p : points){
			float x = p.getX();
			float y = p.getY();

			float dot = x*vx + y*vy;
			if(i == 0 || dot > maxdot){
				maxdot = dot;
				maxp = p;
			}
			i++;
		}

		return maxp;
	}

	public static Point2D_F32 intersectLines(LineParametric2D_F32 l, LineParametric2D_F32 m){
		float t = Intersection2D_F32.intersection(l, m);
		if(Float.isNaN(t)){
			BoofLogUtil.v("No intersection: parallel lines?");
			return null;
		}

		Point2D_F32 p = l.getPointOnLine(t);
		BoofLogUtil.v("Found intersection: " + p.getX() + " " + p.getY());
		return p;
	}

	private static class PointWithAngle{
		Point2D_F32 p;
		float angle;
	}

	public static LinkedList<Point2D_F32> sortInCCWOrder(List<Point2D_F32> points){

		//find a key point; compute the angle of every other point relative to key
		Point2D_F32 keyPoint = GeomUtils.maxInDirection(points, -1,1);

		LinkedList<PointWithAngle> pointsWithAngles = new LinkedList<PointWithAngle>();
		for(Point2D_F32 p : points){
			if(p == keyPoint){ continue; }

			float dx = p.getX() - keyPoint.getX();
			float dy = p.getY() - keyPoint.getY();
			float angle = (float)Math.atan2(dy, dx);

			PointWithAngle pa = new PointWithAngle();
			pa.p = p;
			pa.angle = angle;
			pointsWithAngles.add(pa);
		}

		//sort the non-key points by the computed angle
		Collections.sort(pointsWithAngles, new Comparator<PointWithAngle>(){
			@Override
			public int compare(PointWithAngle pa1, PointWithAngle pa2) {
				return pa1.angle < pa2.angle ? 1 : -1;
			}
		});

		//assemble final collection of sorted points in CCW order
		LinkedList<Point2D_F32> pointsSorted = new LinkedList<Point2D_F32>();
		pointsSorted.add(keyPoint);

		for(PointWithAngle pa : pointsWithAngles){
			pointsSorted.add(pa.p);
		}

		return pointsSorted;
	}

	// NOTE: sortLinesByAreaAbove discards lines which hit top and bottom only, or
	// lines which form a corner triangle with width < THRESHOLD*rectW
	// (in other words, the line needs to cut across horizontally, or at least nearly do so)
	// sortLinesByAreaToLeft discards lines in an analogous way.
	//==========================================================
	private static final float LINE_DISCARD_THRESHOLD = 0.8f;

	public static class LineWithArea{
		public LineParametric2D_F32 line;
		public float area;
	}

	public static List<LineParametric2D_F32> sortLinesByAreaAbove(List<LineParametric2D_F32> lines, float rectW, float rectH){
		Point2D_F32 p00 = new Point2D_F32(0, 0);
		Point2D_F32 p10 = new Point2D_F32(rectW, 0);
		Point2D_F32 p01 = new Point2D_F32(0, rectH);
		Point2D_F32 p11 = new Point2D_F32(rectW, rectH);

		LineSegment2D_F32 ymin = new LineSegment2D_F32(p00, p10);
		LineSegment2D_F32 ymax = new LineSegment2D_F32(p01, p11);
		LineSegment2D_F32 xmin = new LineSegment2D_F32(p00, p01);
		LineSegment2D_F32 xmax = new LineSegment2D_F32(p10, p11);

		List<LineWithArea> linesWithArea = new LinkedList<LineWithArea>();

		for(LineParametric2D_F32 l : lines){
			float area = -1;

			float txmin = Intersection2D_F32.intersection(l, xmin);
			float txmax = Intersection2D_F32.intersection(l, xmax);
			boolean hitxmin = !Float.isNaN(txmin);
			boolean hitxmax = !Float.isNaN(txmax);

			if(hitxmin && hitxmax){
				Point2D_F32 pxmin = l.getPointOnLine(txmin);
				Point2D_F32 pxmax = l.getPointOnLine(txmax);

				area = rectW * 0.5f * (pxmin.getY() + pxmax.getY());
			}else{
				float tymin = Intersection2D_F32.intersection(l, ymin);
				float tymax = Intersection2D_F32.intersection(l, ymax);
				boolean hitymin = !Float.isNaN(tymin);
				boolean hitymax = !Float.isNaN(tymax);

				if(hitxmin){
					Point2D_F32 pxmin = l.getPointOnLine(txmin);
					if(hitymin){
						Point2D_F32 pymin = l.getPointOnLine(tymin);
						float triW = pymin.getX();
						float triH = pxmin.getY();
						if(triW > LINE_DISCARD_THRESHOLD * rectW){
							area = 0.5f * triW * triH;
						}
					}else if(hitymax){
						Point2D_F32 pymax = l.getPointOnLine(tymax);
						float triW = pymax.getX();
						float triH = rectH - pxmin.getY();
						if(triW > LINE_DISCARD_THRESHOLD * rectW){
							area = 0.5f * triW * triH;
						}
					}
				}
				else if(hitxmax){
					Point2D_F32 pxmax = l.getPointOnLine(txmax);
					if(hitymin){
						Point2D_F32 pymin = l.getPointOnLine(tymin);
						float triW = rectW - pymin.getX();
						float triH = pxmax.getY();
						if(triW > LINE_DISCARD_THRESHOLD * rectW){
							area = 0.5f * triW * triH;
						}
					}else if(hitymax){
						Point2D_F32 pymax = l.getPointOnLine(tymax);
						float triW = rectW - pymax.getX();
						float triH = rectH - pxmax.getY();
						if(triW > LINE_DISCARD_THRESHOLD * rectW){
							area = 0.5f * triW * triH;
						}
					}
				}
			}

			if(area > 0){
				LineWithArea augmentedLine = new LineWithArea();
				augmentedLine.line = l;
				augmentedLine.area = area;
				linesWithArea.add(augmentedLine);
			}
		}

		Collections.sort(linesWithArea, new Comparator<LineWithArea>(){
			@Override
			public int compare(LineWithArea lwa1, LineWithArea lwa2) {
				return lwa1.area < lwa2.area ? 1 : -1;
			}
		});

		List<LineParametric2D_F32> sortedLines = new LinkedList<LineParametric2D_F32>();
		for(LineWithArea lwa : linesWithArea){
			sortedLines.add(lwa.line);
		}
		return sortedLines;
	}

	public static List<LineParametric2D_F32> sortLinesByAreaToLeft(List<LineParametric2D_F32> lines, float rectW, float rectH){

		//reflect lines by swapping X and Y
		List<LineParametric2D_F32> refLines = new LinkedList<LineParametric2D_F32>();
		for(LineParametric2D_F32 line : lines){
			Point2D_F32 p = line.getPoint();
			Vector2D_F32 v = line.getSlope();
			refLines.add(new LineParametric2D_F32(p.getY(), p.getX(), v.getY(), v.getX()));
		}

		//sort the reflected lines
		List<LineParametric2D_F32> refLinesSorted = sortLinesByAreaAbove(refLines, rectH, rectW);

		//un-reflect the reflected lines
		List<LineParametric2D_F32> linesSorted = new LinkedList<LineParametric2D_F32>();
		for(LineParametric2D_F32 refLine : refLinesSorted){
			Point2D_F32 p = refLine.getPoint();
			Vector2D_F32 v = refLine.getSlope();
			linesSorted.add(new LineParametric2D_F32(p.getY(), p.getX(), v.getY(), v.getX()));
		}

		return linesSorted;
	}
	//==========================================================
}
