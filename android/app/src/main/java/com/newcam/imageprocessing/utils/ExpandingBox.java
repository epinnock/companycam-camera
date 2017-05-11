package com.newcam.imageprocessing.utils;

public class ExpandingBox {

	protected boolean havePoint;
	protected int minX;
	protected int maxX;
	protected int minY;
	protected int maxY;
	
	public ExpandingBox(){
		reset();
	}
	
	public void reset(){
		havePoint = false;
		minX = 0;
		maxX = 0;
		minY = 0;
		maxY = 0;
	}
	
	private void putFirst(int x, int y){
		havePoint = true;
		minX = x;
		maxX = x;
		minY = y;
		maxY = y;
	}
	
	public void put(int x, int y){
		if(!havePoint){ 
			putFirst(x,y); 
			return; 
		}
		
		if(x < minX){ minX = x; }
		if(x > maxX){ maxX = x; }
		if(y < minY){ minY = y; }
		if(y > maxY){ maxY = y; }
	}
	
	public int area(){
		return havePoint ? (maxX - minX)*(maxY - minY) : 0;
	}
}
