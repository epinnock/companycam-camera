package com.newcam.imageprocessing.utils;

public class BoofLogUtil {
	
	private static final String TAG = "Boof";

	public static void v(String msg){
		System.out.println("[" + TAG + "] (v) " + msg);
	}
	
	public static void d(String msg){
		System.out.println("[" + TAG + "] (d) " + msg);
	}
	
	public static void e(String msg){
		System.err.println("[" + TAG + "] (e) " + msg);
	}
}
