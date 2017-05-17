package com.newcam.imageprocessing.utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;

public class PerspectiveRect {

	private List<LineParametric2D_F32> lines;
	private LinkedList<Point2D_F32> points;
	private LinkedList<Point2D_F32> pointsAsPercent;
	private int sourceImageW;
	private int sourceImageH;
	
	private CameraParams cam;
	private float[] p3d, u3d, v3d;
	
	private boolean isReady = false;
	
	
	public PerspectiveRect(int sourceImageW, int sourceImageH){
		points = new LinkedList<Point2D_F32>();
		pointsAsPercent = new LinkedList<Point2D_F32>();
		
		this.sourceImageW = sourceImageW;
		this.sourceImageH = sourceImageH;
		
		cam = new CameraParams();
		cam.imgw = (float)sourceImageW;
		cam.imgh = (float)sourceImageH;
		cam.aspect = (float)sourceImageW/(float)sourceImageH;
	}

	private void addPointIfWithinBounds(Collection<Point2D_F32> target, Point2D_F32 p){
		if(p == null){ return; }
		
		float x = p.getX();
		float y = p.getY();
		
		if(x < 0.0f){ return; }
		if(y < 0.0f){ return; }
		if(x > (float)sourceImageW){ return; }
		if(y > (float)sourceImageH){ return; }
		if(Float.isNaN(x)){ return; }
		if(Float.isNaN(y)){ return; }
		
		BoofLogUtil.v("Added point: " + p.getX() + " " + p.getY());
		
		target.add(p);
	}
	
	public void setLines(List<LineParametric2D_F32> lines){
		this.lines = lines;
		if(lines.size() != 4){
			BoofLogUtil.e("Number of lines is " + lines.size() + ", should be 4!");
			return;
		}
		
		//lazy, but good enough for now: just compute all 6 and grab the ones that are within bounds
		LinkedList<Point2D_F32> pointsTemp = new LinkedList<Point2D_F32>();
		addPointIfWithinBounds(pointsTemp, GeomUtils.intersectLines(lines.get(0), lines.get(1)));
		addPointIfWithinBounds(pointsTemp, GeomUtils.intersectLines(lines.get(0), lines.get(2)));
		addPointIfWithinBounds(pointsTemp, GeomUtils.intersectLines(lines.get(0), lines.get(3)));
		addPointIfWithinBounds(pointsTemp, GeomUtils.intersectLines(lines.get(1), lines.get(2)));
		addPointIfWithinBounds(pointsTemp, GeomUtils.intersectLines(lines.get(1), lines.get(3)));
		addPointIfWithinBounds(pointsTemp, GeomUtils.intersectLines(lines.get(2), lines.get(3)));
		
		if(pointsTemp.size() != 4){
			BoofLogUtil.e("Number of temp points is " + pointsTemp.size() + ", should be 4!");
			return;
		}
		
		//rearrange in predictable order
		points = GeomUtils.sortInCCWOrder(pointsTemp);

		pointsAsPercent.clear();
		for(Point2D_F32 point : points){
			float xpct = point.getX() / (float)sourceImageW;
			float ypct = point.getY() / (float)sourceImageH;
			pointsAsPercent.add(new Point2D_F32(xpct, ypct));
		}
		
		if(points.size() != 4){
			BoofLogUtil.e("Number of ordered points is " + points.size() + ", should be 4!");
			return;
		}
		
		//now determine 3D rectangle
		set3DRectFromPoints();
		isReady = true;
	}
	
	private static float[] screenToRay(CameraParams cam, float x, float y){
		float tx = x/cam.imgw;
		float ty = y/cam.imgh;
		
		float h = (float)Math.tan(cam.fovy*0.5f * 3.14159f/180.0f);
		float rx = (-1.0f + 2.0f*tx)*h*cam.aspect;
		float ry = (-1.0f + 2.0f*ty)*h;
		float rz = -1.0f;
		
		return new float[]{ rx, ry, rz };
	}
	
	private void set3DRectFromPoints(){		
		float[][] v = new float[4][3];
		for(int i=0; i<4; i++){
			v[i] = screenToRay(cam, points.get(i).getX(), points.get(i).getY());
		}
		
		float d130 = VectorUtils.det3(v[1],v[3],v[0]);
		float d132 = VectorUtils.det3(v[1],v[3],v[2]);
		float d230 = VectorUtils.det3(v[2],v[3],v[0]);
		float d201 = VectorUtils.det3(v[2],v[0],v[1]);
		
		float[] t = new float[]{ 1.0f, 1.0f, 1.0f, 1.0f };
		t[3] = 1.0f;
		t[1] =       d230/d201;
		t[0] = -t[1]*d132/d230;
		t[2] =  t[1]*d130/d230;
		
		p3d = VectorUtils.linearCombination(t[0], v[0]);
		u3d = VectorUtils.linearCombination(t[1], v[1], -t[0], v[0]);
		v3d = VectorUtils.linearCombination(t[3], v[3], -t[0], v[0]);
	}

	/** Draw the lines making up the boundary of the perspective rectangle. **/
	public void drawLines(DrawingUtil drawutil){
		if(!isReady){ return; }
		drawutil.drawLines(lines);
	}

	/** Draw the corner points of the perspective rectangle. **/
	public void drawPoints(DrawingUtil drawutil){
		if(!isReady){ return; }
		drawutil.drawPoints(points);
	}

	/**
	 * Map the coordinates on the 3D rectangle to the original image which has
	 * the perspective-distorted rectangle drawn onto it.
	 * <br>  
	 * That is, if you are perspective-correcting an image, (xdst,ydst) is
	 * the position on the destination image, and psrc will hold the position
	 * on the original image to sample from.
	 * @param xdst x coordinate on destination image; range [0,1]
	 * @param ydst y coordinate on destination image; range [0,1]
	 * @param psrc Array to hold position on original image, coords in [0,1]
	 * @return True if successful, false if error/not ready
	 */
	public boolean project3DRectToTexture(float xdst, float ydst, float[] psrc){
		if(!isReady){ return false; }
		
		float[] curPos3d = VectorUtils.linearCombination(1.0f, p3d, xdst, u3d, ydst, v3d);
		
		float projx = -curPos3d[0]/curPos3d[2];
		float projy = -curPos3d[1]/curPos3d[2];
		float hy = (float)Math.tan(cam.fovy*0.5f * 3.14159f/180.0f);
		float hx = cam.aspect * hy;
		
		psrc[0] = 0.5f + 0.5f*projx/hx;
		psrc[1] = 0.5f + 0.5f*projy/hy;
		return true;
	}
	
	/**
	 * Get the dimensions of the 3D rectangle.
	 * @param dims Array which is to hold the dimensions
	 * @return True if successful, false if error/not ready
	 */
	public boolean get3DRectDims(float[] dims){
		if(!isReady){ return false; }
		
		dims[0] = VectorUtils.len(u3d);
		dims[1] = VectorUtils.len(v3d);
		return true;
	}

	/**
	 * Get the points, in screen space, at the corners of the 3D rectangle projected onto
	 * the image.  Coordinates are in [0,1]x[0,1], for use in drawing on scaled images.
	 * @return List of points, or null if not ready yet.
	 */
	public List<Point2D_F32> getPointsAsPercent(){
		if(!isReady){ return null; }
		return pointsAsPercent;
	}

	/**
	 * Returns the max distance (in screen space) between corresponding points of PerspectiveRects.
     * @return Returns NaN if either PerspectiveRect is null or is not ready.
     */
	public static float maxScreenSpacePointDistance(PerspectiveRect pr1, PerspectiveRect pr2){
		boolean rectsInvalid = (pr1 == null) || (pr2 == null) || !pr1.isReady || !pr2.isReady;
		if(rectsInvalid){ return Float.NaN; }

		List<Point2D_F32> points1 = pr1.points;
		List<Point2D_F32> points2 = pr2.points;
		boolean pointsInvalid = (points1 == null) || (points2 == null) || (points1.size() != 4) || (points2.size() != 4);
		if(pointsInvalid){ return Float.NaN; }

		float maxdist = Float.NaN;
		for(Point2D_F32 p : points1){
			float dist = GeomUtils.getDistance(p, points2);
			if(Float.isNaN(dist)){ continue; }
			if(Float.isNaN(maxdist) || (dist > maxdist)){
				maxdist = dist;
			}
		}
		return maxdist;
	}
}
