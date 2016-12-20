package com.agilx.companycam.util.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by mattboyd on 11/11/16.
 */

public class FocusIndicatorView extends View {

    public float radius = 0.5f;

    public FocusIndicatorView(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public FocusIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    public FocusIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
    }

    @Override
    public void onDraw(Canvas canvas) {

        // Get the width and height if this view
        int width = this.getWidth();
        int height = this.getHeight();

        // Define a stroke width in pixels
        float strokeWidth = 5.0f;

        RectF indicatorRect = new RectF();
        indicatorRect.set(strokeWidth/2.0f, strokeWidth/2.0f, width - strokeWidth/2.0f, height - strokeWidth/2.0f);

        // Draw the main circle
        Paint p = new Paint();
        p.setFlags(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokeWidth);
        p.setColor(0x88ffffff);
        canvas.drawArc(indicatorRect, 0, 360, false, p);

        RectF interiorRect = new RectF();
        interiorRect.set(((1.0f - radius)*width)/2.0f, ((1.0f - radius)*height)/2.0f, ((1.0f + radius)*width)/2.0f, ((1.0f + radius)*height)/2.0f);

        // Draw the interior circle
        if (radius > 0) {
            p = new Paint();
            p.setFlags(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.FILL);
            p.setShader(new RadialGradient(width/2.0f, height/2.0f, width*(radius/2.0f), Color.TRANSPARENT, 0x88ffffff, Shader.TileMode.MIRROR));
            canvas.drawArc(interiorRect, 0, 360, true, p);
        }
    }
}
