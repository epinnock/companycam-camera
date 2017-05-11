package com.newcam.imageprocessing.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import boofcv.alg.filter.binary.Contour;
import georegression.metric.Intersection2D_F32;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;

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
}
