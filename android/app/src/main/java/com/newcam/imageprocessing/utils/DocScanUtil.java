package com.newcam.imageprocessing.utils;

import java.util.LinkedList;
import java.util.List;

import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import georegression.struct.line.LineParametric2D_F32;

public class DocScanUtil {

	private GrayU8 imageU8;
	private GrayU8 blurU8;
	private int IMAGE_W, IMAGE_H;

	private static final int BLUR_RADIUS = 10;

	private	DetectLineHoughPolar<GrayU8,GrayS16> lineDetector;

	//keep this around for debug rendering
	private List<LineParametric2D_F32> linesV;
	private List<LineParametric2D_F32> linesH;
	private List<LineParametric2D_F32> linesAll;


	public DocScanUtil(GrayU8 imageU8){
		this.imageU8 = imageU8;
		IMAGE_W = imageU8.getWidth();
		IMAGE_H = imageU8.getHeight();

		blurU8 = imageU8.createSameShape();

		//Radius for local maximum suppression.
		int localMaxRadius = 10;
		//Maximum number of lines to return.
		int maxLines = 1000;
		//Minimum number of counts for detected line.
		int minCounts = 60;
		//Resolution of line angle in radius.
		double resolutionAngle = Math.PI / 180;
		//Resolution of line range in pixels.
		double resolutionRange = 2;
		//Edge detection threshold.
		float thresholdEdge = 25;

		ConfigHoughPolar configHough = new ConfigHoughPolar(localMaxRadius, minCounts, resolutionRange, resolutionAngle, thresholdEdge, maxLines);
		lineDetector = FactoryDetectLineAlgs.houghPolar(configHough, GrayU8.class, GrayS16.class);
	}

	/** tempCanvas should have same size as the imageU8 passed into constructor **/
	public PerspectiveRect scan(){

		long startBlur = System.currentTimeMillis();

		BlurImageOps.gaussian(imageU8, blurU8, -1, BLUR_RADIUS, null);

		long endBlur = System.currentTimeMillis();
		BoofLogUtil.d("Blur: " + (endBlur - startBlur) + " ms");

		long startHough = System.currentTimeMillis();

		linesAll = lineDetector.detect(blurU8);
		BoofLogUtil.v("FOUND LINES: " + linesAll.size());

		long endHough = System.currentTimeMillis();
		BoofLogUtil.d("Hough: " + (endHough - startHough) + " ms");

		//find rect edges from all hough lines
		//------------------------------------
		long startSort = System.currentTimeMillis();

		linesV = new LinkedList<LineParametric2D_F32>();
		linesH = new LinkedList<LineParametric2D_F32>();
		for(LineParametric2D_F32 line : linesAll){
			float angle = line.getAngle();
			float absx = (float)Math.abs(Math.cos(angle));
			float absy = (float)Math.abs(Math.sin(angle));
			if(absx < 0.5f){ linesV.add(line); }
			if(absy < 0.5f){ linesH.add(line); }
		}
		linesH = GeomUtils.sortLinesByAreaAbove(linesH, IMAGE_W, IMAGE_H);
		linesV = GeomUtils.sortLinesByAreaToLeft(linesV, IMAGE_W, IMAGE_H);

		if(linesH.size() < 2 || linesV.size() < 2){
			BoofLogUtil.v("Did not find enough horizontal or vertical lines!");
			return null;
		}

		List<LineParametric2D_F32> linesRect = new LinkedList<LineParametric2D_F32>();
		linesRect.add(linesH.get(0));
		linesRect.add(linesH.get(linesH.size()-1));
		linesRect.add(linesV.get(0));
		linesRect.add(linesV.get(linesV.size()-1));

		long endSort = System.currentTimeMillis();
		BoofLogUtil.d("Sort: " + (endSort - startSort) + " ms");
		//------------------------------------

		PerspectiveRect rect = new PerspectiveRect(IMAGE_W, IMAGE_H);
		rect.setLines(linesRect);

		return rect;
	}

	/** Draw the lines making up the boundary of the perspective rectangle. **/
	public void drawLines(DrawingUtil drawutil){
		if(linesAll == null){ return; }
		drawutil.drawLines(linesAll);
	}
}
