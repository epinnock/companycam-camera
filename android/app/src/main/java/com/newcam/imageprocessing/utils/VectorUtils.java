package com.newcam.imageprocessing.utils;

public class VectorUtils {
	
	public static float det3(float[] u, float[] v, float[] w){
		return (
			u[0] * ( v[1]*w[2] - v[2]*w[1] ) -
			u[1] * ( v[0]*w[2] - v[2]*w[0] ) +
			u[2] * ( v[0]*w[1] - v[1]*w[0] )
		);
	}

	public static float[] linearCombination(float a, float[] u){
		return new float[]{ a*u[0], a*u[1], a*u[2] };
	}
	
	public static float[] linearCombination(float a, float[] u, float b, float[] v){
		return new float[]{
			a*u[0] + b*v[0],
			a*u[1] + b*v[1],
			a*u[2] + b*v[2]
		};
	}
	
	public static float[] linearCombination(float a, float[] u, float b, float[] v, float c, float[] w){
		return new float[]{
			a*u[0] + b*v[0] + c*w[0],
			a*u[1] + b*v[1] + c*w[1],
			a*u[2] + b*v[2] + c*w[2]
		};
	}
	
	public static float len(float[] u){
		return (float)Math.sqrt(u[0]*u[0] + u[1]*u[1] + u[2]*u[2]);
	}
	
	public static String toString(float[] v){
		return "[" + v[0] + ", " + v[1] + ", " + v[2] + "]";
	}
}
