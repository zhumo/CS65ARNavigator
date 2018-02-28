package edu.dartmouth.com.arnavigation.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;

public class NonSwipingViewPager extends ViewPager {

    public NonSwipingViewPager(Context context) { super(context); }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
