package com.yashoid.openpainting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class WorldView extends View implements StrongGestureDetector.OnGestureListener {

    public static final int MODE_PAINT = 0;
    public static final int MODE_PAN = 1;

    public interface OnPointListener {

        void onPoint(int x, int y);

        void onTouchFinished();

    }

    private OnPointListener mOnPointListener = null;

    private final Matrix mMatrix = new Matrix();
    private final Matrix mReverseMatrix = new Matrix();

    private StrongGestureDetector mGestureDetector;

    private Bitmap mBitmap = null;

    private int mMode = MODE_PAINT;

    private final Paint mPaint = new Paint();
    private final float[] mHelperPoint = new float[2];

    public WorldView(Context context) {
        super(context);
        initialize(context, null, 0);
    }

    public WorldView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public WorldView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        mPaint.setAntiAlias(false);
        mPaint.setFilterBitmap(false);

        mGestureDetector = new StrongGestureDetector(this);
    }

    public void setOnPointListener(OnPointListener listener) {
        mOnPointListener = listener;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;

        resetMatrix();
    }

    public int getMode() {
        return mMode;
    }

    public void setMode(int mode) {
        mMode = mode;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        resetMatrix();
    }

    private void resetMatrix() {
        if (mBitmap == null || mBitmap.getWidth() == 0 || mBitmap.getHeight() == 0 || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        mMatrix.reset();
        mMatrix.postTranslate((getWidth() - mBitmap.getWidth()) / 2f, (getHeight() - mBitmap.getHeight()) / 2f);

        float bitmapRatio = (float) mBitmap.getWidth() / mBitmap.getHeight();
        float viewRatio = (float) getWidth() / getHeight();

        float scale;

        if (bitmapRatio < viewRatio) {
            scale = (float) getWidth() / mBitmap.getWidth();
        }
        else {
            scale = (float) getHeight() / mBitmap.getHeight();
        }

        mMatrix.postScale(scale, scale, getWidth() / 2f, getHeight() / 2f);

        mMatrix.invert(mReverseMatrix);

        invalidate();
    }

    public void setColor(int x, int y, int color) {
        if (mBitmap != null) {
            mBitmap.setPixel(x, y, color);
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBitmap != null) {
            canvas.save();
            canvas.setMatrix(mMatrix);
            canvas.drawBitmap(mBitmap, 0, 0, mPaint);
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mBitmap == null) {
            return false;
        }

        if (mMode == MODE_PAN) {
            return mGestureDetector.onTouchEvent(event);
        }

        mHelperPoint[0] = event.getX();
        mHelperPoint[1] = event.getY();

        mReverseMatrix.mapPoints(mHelperPoint);

        int x = (int) mHelperPoint[0];
        int y = (int) mHelperPoint[1];

        if (x < 0 || y < 0 || x >= mBitmap.getWidth() || y >= mBitmap.getHeight()) {
            if (mOnPointListener != null) {
                mOnPointListener.onTouchFinished();
            }
            return false;
        }

        if (mOnPointListener != null) {
            mOnPointListener.onPoint(x, y);
        }

        if (event.getActionMasked() != MotionEvent.ACTION_DOWN && event.getActionMasked() != MotionEvent.ACTION_MOVE) {
            if (mOnPointListener != null) {
                mOnPointListener.onTouchFinished();
            }
        }

        return true;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        return true;
    }

    @Override
    public void onUp() {

    }

    @Override
    public void onTranslate(float dx, float dy) {
        mMatrix.postTranslate(dx, dy);
        mMatrix.invert(mReverseMatrix);
        invalidate();
    }

    @Override
    public void onScale(float ds) {
        mMatrix.postScale(ds, ds, getWidth() / 2f, getHeight() / 2f);
        mMatrix.invert(mReverseMatrix);
        invalidate();
    }

    @Override
    public void onRotate(float dd) {

    }

}
