package com.newcam.imageprocessing;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;

import com.newcam.imageprocessing.utils.DrawingUtil;

import java.util.List;

import boofcv.alg.feature.detect.edge.EdgeSegment;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;

/**
 * Created by dan on 5/10/17.
 */

public class DrawingUtilAndroid implements DrawingUtil {

    private Canvas canvas;

    public DrawingUtilAndroid(Canvas canvas){
        this.canvas = canvas;
    }

    private void _drawContour(List<Point2D_I32> points, Paint paint){
        int i=0;
        Point2D_I32 p = null;
        for(Point2D_I32 q : points) {
            if(i > 0){
                canvas.drawLine(p.getX(), p.getY(), q.getX(), q.getY(), paint);
            }
            p = q;
            i++;
        }
    }

    @Override
    public void drawContourLight(List<Point2D_I32> points){
        Paint paint = new Paint();
        paint.setColor(Color.argb(255, 255,255,255));
        _drawContour(points, paint);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void drawContourHeavy(List<Point2D_I32> points){
        float pr = 5.0f;

        Paint paint = new Paint();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(255, 0,0,255));
        for(Point2D_I32 q : points) {
            canvas.drawArc(q.getX()-pr, q.getY()-pr, q.getX()+pr, q.getY()+pr, 0, 360, false, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(255, 255,0,0));
        paint.setStrokeWidth(2.0f);
        _drawContour(points, paint);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void drawPoints(List<Point2D_F32> points){
        float pr = 5.0f;
        int[] colors = new int[]{
            Color.argb(255, 255,0,0),
            Color.argb(255, 0,255,0),
            Color.argb(255, 0,0,255),
            Color.argb(255, 255,255,255)
        };

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        int i=0;
        for(Point2D_F32 p : points){
            int ci = (i < colors.length) ? i : (colors.length - 1);
            paint.setColor(Color.argb(255, 0,255,0));
            canvas.drawArc(p.getX()-pr, p.getY()-pr, p.getX()+pr, p.getY()+pr, 0, 360, false, paint);

            i++;
        }
    }

    @Override
    public void drawLine(Point2D_F32 p, Point2D_F32 q){
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(255, 255,255,255));
        canvas.drawLine(p.getX(), p.getY(), q.getX(), q.getY(), paint);
    }

    @Override
    public void drawLines(List<LineParametric2D_F32> lines){
        for(LineParametric2D_F32 line : lines){
            drawLine(line.getPointOnLine(-500), line.getPointOnLine(500));
        }
    }

    @Override
    public void drawEdgesLight(List<EdgeSegment> segments){
        for(EdgeSegment segment : segments){
            drawContourLight(segment.points);
        }
    }
}
