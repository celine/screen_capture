package com.screencapture;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by wenchihhsieh on 2017/4/4.
 */

public class ViewTouchListener implements View.OnTouchListener {
    PointF initial;
    PointF initialTouch;
    WindowManager.LayoutParams layoutParams;
    WindowManager windowManager;
    GestureDetector gestureDetector;
    private static final String LOG_TAG = ViewTouchListener.class.getSimpleName();

    public ViewTouchListener(Context context, WindowManager windowManager, WindowManager.LayoutParams layoutParams) {
        this.windowManager = windowManager;
        this.layoutParams = layoutParams;
        gestureDetector = new GestureDetector(context, new SingleTapConfirm());
    }

    public static class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(LOG_TAG, "detect " + event.getRawY() + " " + event.getRawY());

        /*switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initial = new PointF(layoutParams.x, layoutParams.y);
                initialTouch = new PointF(event.getRawX(), event.getRawY());
                return false;
            case MotionEvent.ACTION_UP:
                return false;
            case MotionEvent.ACTION_MOVE:

                layoutParams.x = (int) (initial.x + (event.getRawX() - initialTouch.x));
                layoutParams.y = (int) (initial.y + (event.getRawY() - initialTouch.y));
                windowManager.updateViewLayout(v, layoutParams);
                return false;
        }*/
        return false;
    }
}
