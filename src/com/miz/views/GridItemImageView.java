package com.miz.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * A custom ImageView class that shows all images in a 2:3 format.
 * @author Michell
 *
 */
public class GridItemImageView extends ImageView {

	private int mWidth, mHeight;
	
    public GridItemImageView(Context context) {
        super(context);
    }

    public GridItemImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridItemImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	mWidth = MeasureSpec.getSize(widthMeasureSpec);
    	mHeight = (int) (mWidth * 1.5);
        setMeasuredDimension(mWidth, mHeight);
    }
}