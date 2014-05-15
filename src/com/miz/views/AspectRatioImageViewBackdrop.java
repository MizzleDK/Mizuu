package com.miz.views;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AspectRatioImageViewBackdrop extends ImageView {

    public AspectRatioImageViewBackdrop(Context context) {
        super(context);
    }

    public AspectRatioImageViewBackdrop(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageViewBackdrop(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int width = MeasureSpec.getSize(widthMeasureSpec);
    	int height = (int) (width / 1.778);
        setMeasuredDimension(width, height);
    }
}