package com.notagilx.companycam.util.views;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

public class VerticalTextView extends TextView {
    final boolean topDown;

    public VerticalTextView(Context context, AttributeSet attrs){
        super(context, attrs);
        final int gravity = getGravity();
        if(Gravity.isVertical(gravity) && (gravity&Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
            setGravity((gravity&Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
            topDown = true;
        }else
            topDown = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    protected void onDraw(Canvas canvas){

        TextPaint textPaint = getPaint();
        textPaint.setColor(getCurrentTextColor());
        textPaint.drawableState = getDrawableState();

        canvas.save();

        if(topDown){
            canvas.translate(getWidth(), 0);
            canvas.rotate(90);
        }else {
            canvas.translate(0, getHeight());
            canvas.rotate(-90);
        }

        // getCompoundPaddingLeft() and getExtendedPaddingTop() can sometimes throw null pointer exceptions depending on when they get
        // called in the layout process.
        try {
            canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());
        }
        catch (NullPointerException npe) {
        }

        // getLayout() can return null depending on when this method is called, so check to make sure it returns a valid object.
        if (getLayout() != null) {
            getLayout().draw(canvas);
        }

        canvas.restore();
    }
}
