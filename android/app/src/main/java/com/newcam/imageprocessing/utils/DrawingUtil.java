package com.newcam.imageprocessing.utils;

import java.util.List;

import boofcv.alg.feature.detect.edge.EdgeSegment;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;

public interface DrawingUtil {
	public abstract void drawContourHeavy(List<Point2D_I32> points);
	public abstract void drawContourLight(List<Point2D_I32> points);
	public abstract void drawPoints(List<Point2D_F32> points);
	public abstract void drawLine(Point2D_F32 p, Point2D_F32 q);
	public abstract void drawLines(List<LineParametric2D_F32> lines);
	public abstract void drawEdgesLight(List<EdgeSegment> segments);
}
