package com.miz.functions;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AspectRatioImageViewCover extends ImageView {

    public AspectRatioImageViewCover(Context context) {
        super(context);
    }

    public AspectRatioImageViewCover(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageViewCover(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int height = MeasureSpec.getSize(heightMeasureSpec);
    	int width = (int) (height * 0.667);
        setMeasuredDimension(width, height);
    }
}