package com.zmy.next.beautyface.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * Created by zmy on 2017/8/4.
 */

public class SurfaceViewRenderer extends SurfaceView {
    public SurfaceViewRenderer(Context context) {
        this(context, null);
    }

    public SurfaceViewRenderer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SurfaceViewRenderer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
