package com.yashoid.openpainting;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public class TouchGatherer {

    private static final int TOUCH_COUNT_LIMIT = 20;
    private static final long TOUCH_TIME_LIMIT = 1000 / 60;

    public interface OnBatchListener {

        void sendBatchTouches(List<Touch> touches);

    }

    private final Handler mHandler;

    private OnBatchListener mListener;

    private final List<Touch> mTouches = new ArrayList<>();

    private Touch mLastTouch = null;

    public TouchGatherer(OnBatchListener listener) {
        mHandler = new Handler();

        mListener = listener;
    }

    public void addTouch(Touch touch) {
        mLastTouch = touch;

        if (mTouches.isEmpty()) {
            mHandler.postDelayed(mTouchSender, TOUCH_TIME_LIMIT);
        }

        mTouches.add(touch);

        if (mTouches.size() >= TOUCH_COUNT_LIMIT) {
            mListener.sendBatchTouches(mTouches);

            mTouches.clear();

            mHandler.removeCallbacks(mTouchSender);
        }
    }

    public void addTouchConnecting(Touch touch) {
        if (mLastTouch == null) {
            addTouch(touch);
            return;
        }

        Touch lastTouch = mLastTouch;

        int xDistance = Math.abs(touch.x - lastTouch.x);
        int yDistance = Math.abs(touch.y - lastTouch.y);

        if (xDistance > yDistance) {
            int step = touch.x > lastTouch.x ? 1 : -1;

            int x = lastTouch.x + step;

            while (step > 0 ? x < touch.x : x > touch.x) {
                int y = Math.round(lastTouch.y + (float) Math.abs(x - lastTouch.x) / xDistance * (touch.y - lastTouch.y));
                addTouch(new Touch(x, y, touch.color));

                x += step;
            }
        }
        else {
            int step = touch.y > lastTouch.y ? 1 : -1;

            int y = lastTouch.y + step;

            while (step > 0 ? y < touch.y : y > touch.y) {
                int x = Math.round(lastTouch.x + (float) Math.abs(y - lastTouch.y) / yDistance * (touch.x - lastTouch.x));
                addTouch(new Touch(x, y, touch.color));

                y += step;
            }
        }

        addTouch(touch);
    }

    public void disconnectTouches() {
        mLastTouch = null;
    }

    private final Runnable mTouchSender = () -> {
        if (!mTouches.isEmpty()) {
            mListener.sendBatchTouches(mTouches);

            mTouches.clear();
        }
    };

}
