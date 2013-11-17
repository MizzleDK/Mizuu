package com.miz.functions;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AspectRatioImageViewBackdropLarge extends ImageView {

    public AspectRatioImageViewBackdropLarge(Context context) {
        super(context);
    }

    public AspectRatioImageViewBackdropLarge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageViewBackdropLarge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	int width = MeasureSpec.getSize(widthMeasureSpec);
    	int height = (int) (width / 1.4);
        setMeasuredDimension(width, height);
    }
}