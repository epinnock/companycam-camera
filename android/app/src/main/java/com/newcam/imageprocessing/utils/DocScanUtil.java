package com.newcam.imageprocessing.utils;

import java.util.List;

import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import georegression.struct.line.LineParametric2D_F32;

public class DocScanUtil {

	private GrayU8 imageU8;
	private int IMAGE_W, IMAGE_H;
	
	private GrayU8 edgeImageU8;
	
	private CannyEdge<GrayU8,GrayS16> canny;
	private	DetectLineHoughPolar<GrayU8,GrayS16> lineDetector;
	
	//keep this around for debug rendering the contour
	private Contour maxAABBContour;
	
	// adjusts edge threshold for identifying pixels belonging to a line
	private static final float edgeThreshold = 25;
	// adjust the maximum number of found lines in the image
	private static final int maxLines = 4;
	
	
	public DocScanUtil(GrayU8 imageU8){
		this.imageU8 = imageU8;
		IMAGE_W = imageU8.getWidth();
		IMAGE_H = imageU8.getHeight();
		
		edgeImageU8 = imageU8.createSameShape();
		
		canny = FactoryEdgeDetectors.canny(1, false, true, GrayU8.class, GrayS16.class);
		
		ConfigHoughPolar configHough = new ConfigHoughPolar(3, 30, 2, Math.PI / 180, edgeThreshold, maxLines);
		lineDetector = FactoryDetectLineAlgs.houghPolar(configHough, GrayU8.class, GrayS16.class);
	}
	
	/** tempCanvas should have same size as the imageU8 passed into constructor **/
	public PerspectiveRect scan(DrawableU8 tempCanvas){

		long timeCanny1 = System.currentTimeMillis();
		
		// The edge image is actually an optional parameter.  If you don't need it just pass in null
		canny.process(imageU8, 0.1f, 0.3f, edgeImageU8);

		long timeCanny2 = System.currentTimeMillis();

		// First get the contour created by canny
		//List<EdgeContour> edgeContours = canny.getContours();
		List<Contour> contours = BinaryImageOps.contour(edgeImageU8, ConnectRule.EIGHT, null);

		long timeCanny3 = System.currentTimeMillis();

		//find contour with biggest-area AABB
		maxAABBContour = GeomUtils.findBiggestContour(contours);

		long timeCanny4 = System.currentTimeMillis();
		BoofLogUtil.d("Canny 1: " + (timeCanny2 - timeCanny1) + " ms");
		BoofLogUtil.d("Canny 2: " + (timeCanny3 - timeCanny2) + " ms");
		BoofLogUtil.d("Canny 3: " + (timeCanny4 - timeCanny3) + " ms");
		
		if(maxAABBContour == null){
			BoofLogUtil.d("MAX AABB NULL!");
			return null;
		}
		
		// display the results
		//BufferedImage visualBinary = VisualizeBinaryData.renderBinary(edgeImageU8, false, null);
		//BufferedImage visualCannyContour = VisualizeBinaryData.renderContours(edgeContours, null, imageU8.width, imageU8.height, null);

		long startHough = System.currentTimeMillis();
		
		DrawingUtil tempDrawUtil = tempCanvas.getDrawingUtil();
		tempDrawUtil.drawContourLight(maxAABBContour.external);
		GrayU8 outlineImageU8 = tempCanvas.getGrayU8();
		
		List<LineParametric2D_F32> lines = lineDetector.detect(outlineImageU8);
		BoofLogUtil.v("FOUND LINES: " + lines.size());
		
		PerspectiveRect rect = new PerspectiveRect(IMAGE_W, IMAGE_H);
		rect.setLines(lines);
		
		long endHough = System.currentTimeMillis();
		BoofLogUtil.d("Hough: " + (endHough - startHough) + " ms");
		
		return rect;
	}
	
	public void drawLastMaxContour(DrawingUtil drawutil){
		if(maxAABBContour == null){ return; }
		drawutil.drawContourHeavy(maxAABBContour.external);
	}
}
