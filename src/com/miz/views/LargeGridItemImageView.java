package com.miz.views;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class LargeGridItemImageView extends ImageView {

    public LargeGridItemImageView(Context context) {
        super(context);
    }

    public LargeGridItemImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LargeGridItemImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int width = MeasureSpec.getSize(widthMeasureSpec);
    	int height = (int) (width * 0.45);
        setMeasuredDimension(width, height);
    }
}