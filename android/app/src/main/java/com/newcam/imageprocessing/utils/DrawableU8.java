package com.newcam.imageprocessing.utils;

import boofcv.struct.image.GrayU8;

public interface DrawableU8 {

	public DrawingUtil getDrawingUtil();
	public GrayU8 getGrayU8();
	public void clearBlack();
}
