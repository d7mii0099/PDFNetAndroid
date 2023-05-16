//------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Canvas;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;

/**
 * This class is a TextView that draw text vertically.
 * If gravity is Gravity.BOTTOM, it draws text from bottom to top,
 * otherwise draw text from top to bottom
 */
public class VerticalTextView extends AppCompatTextView {

    private boolean topDown = false;

    /**
     * Class constructor
     * @param context Context
     */
    public VerticalTextView(Context context) {
        super(context);
        init();
    }
    /**
     * Class constructor
     * @param context Context
     */
    public VerticalTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    /**
     * Class constructor
     * @param context Context
     */
    public VerticalTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int gravity = getGravity();
        if (Gravity.isVertical(gravity) && (gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM){
            setGravity((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) | Gravity.TOP);
            topDown = false;
        }
        else{
            topDown = true;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        TextPaint textPaint = getPaint();
        textPaint.setColor(getCurrentTextColor());
        textPaint.drawableState = getDrawableState();

        canvas.save();

        if (topDown){
            canvas.translate(getWidth(), 0);
            canvas.rotate(90);
        }
        else{
            canvas.translate(0, getHeight());
            canvas.rotate(-90);
        }

        canvas.translate(getCompoundPaddingLeft(),
            getExtendedPaddingTop());

        getLayout().draw(canvas);
        canvas.restore();
    }
}
