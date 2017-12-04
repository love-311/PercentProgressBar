package www.lee311.percentprogressbar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Lxy on 2017/10/24.
 * PercentProgressBar
 */

public class PercentProgressBar extends View {

    private static int TRANSPARENT_WIDTH = 80;
    private final int VERTICAL = 1;
    private final int HORIZONTAL = 2;
    private final int defaultBgColor = 0xFf00ff00;
    private final int defaultProgressColor = 0xff3300;
    private final int defaultTextColor = 0xFFffff00;
    private int bgColor = defaultBgColor;
    private int progressColor = defaultProgressColor;
    private int textColor = defaultTextColor;
    /*圆角弧度*/
    private float rectRadius = 10f;
    /*文字与图表超出矩形框之外的透明矩形框*/
    private RectF mRectF = new RectF();
    /*画背景使用的Rect*/
    private RectF bgRect = new RectF();
    /*画进度使用的Rect*/
    private RectF progressRect = new RectF();
    /*背景画笔*/
    private Paint bgPaint;
    /* 超出范围的背景画笔*/
    private Paint mPaint;
    /*进度画笔*/
    private Paint progressPaint;
    /*写字画笔*/
    private Paint writePaint;
    /*进度方向*/
    private int orientation = VERTICAL;
    private int max = 100;
    private int progress = 0;
    private Bitmap bitmap;
    /*icon显示区域Rect*/
    private Rect srcRect;
    /*icon显示位置Rect*/
    private Rect dstRect;
    private float iconPadding;
    /*进度百分比*/
    private int percent = 0;
    /*文字大小*/
    private int textSize = 64;
    /*滑块图案*/
    private Bitmap iconBitmap;
    /*根据上下距离确定文字基线位置*/
    private float top = 0;//为基线到字体上边框的距离,即上图中的top
    private float bottom = 0;//为基线到字体下边框的距离,即上图中的bottom
    private int baseLineY = 0;
    private OnProgressChangedListener changedListener;
    private ValueAnimator valueAnimator;

    public PercentProgressBar(Context context) {
        super(context);
        init(context, null);
    }

    public PercentProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PercentProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        //关闭硬件加速，不然setXfermode()可能会不生效
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PercentProgressBar);
            bgColor = typedArray.getColor(R.styleable.PercentProgressBar_bgColor, defaultBgColor);
            textColor = typedArray.getColor(R.styleable.PercentProgressBar_textColor, defaultTextColor);
            progressColor = typedArray.getColor(R.styleable.PercentProgressBar_progressColor, defaultProgressColor);
            progress = typedArray.getInteger(R.styleable.PercentProgressBar_progressValue, progress);
            max = typedArray.getInteger(R.styleable.PercentProgressBar_progressMax, max);
            if (max <= 0)
                throw new RuntimeException("Max 必须大于 0");
            orientation = typedArray.getInteger(R.styleable.PercentProgressBar_progressOrientation, VERTICAL);
            int imgSrc = typedArray.getResourceId(R.styleable.PercentProgressBar_iconSrc, 0);
            int bitmapSrc = typedArray.getResourceId(R.styleable.PercentProgressBar_iconBitmap, 0);
            iconPadding = typedArray.getDimensionPixelSize(R.styleable.PercentProgressBar_iconPadding, 10);
            rectRadius = typedArray.getDimensionPixelSize(R.styleable.PercentProgressBar_rectRadius, 10);
            if (max < progress) {
                progress = max;
            }
            typedArray.recycle();

            if (imgSrc != 0) {
                bitmap = ((BitmapDrawable) getResources().getDrawable(imgSrc)).getBitmap();
            }
            if (bitmapSrc != 0) {
                iconBitmap = ((BitmapDrawable) getResources().getDrawable(bitmapSrc)).getBitmap();
            }

        }

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(bgColor);

        //超出范围的大透明背景
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.TRANSPARENT);

        writePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        writePaint.setTextSize(textSize);
        writePaint.setColor(Color.WHITE);
        writePaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fontMetrics = writePaint.getFontMetrics();
        top = fontMetrics.top;//为基线到字体上边框的距离,即上图中的top
        bottom = fontMetrics.bottom;//为基线到字体下边框的距离,即上图中的bottom

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        progressPaint.setColor(progressColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bgRect.set(getPaddingLeft() + TRANSPARENT_WIDTH
                , getPaddingTop() + TRANSPARENT_WIDTH
                , getWidth() - getPaddingRight() - TRANSPARENT_WIDTH
                , getHeight() - getPaddingBottom() - TRANSPARENT_WIDTH);
        //透明背景
        mRectF.set(getPaddingLeft()
                , getPaddingTop()
                , getWidth() - getPaddingRight()
                , getHeight() - getPaddingBottom());
        computeProgressRect();

        if (bitmap != null) {
            srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            int iconSideLength;
            if (orientation == VERTICAL) {
                iconSideLength = (int) (bgRect.width() - iconPadding * 2);
                dstRect = new Rect((int) bgRect.left + (int) iconPadding
                        , (int) (bgRect.bottom - iconSideLength - iconPadding)
                        , (int) bgRect.right - (int) iconPadding
                        , (int) bgRect.bottom - (int) iconPadding);
            } else {
                iconSideLength = (int) (bgRect.height() - iconPadding * 2);
                dstRect = new Rect((int) bgRect.left + (int) iconPadding
                        , (int) (bgRect.bottom - iconPadding - iconSideLength)
                        , (int) (bgRect.left + iconPadding + iconSideLength)
                        , (int) (bgRect.bottom - iconPadding));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int layerId = canvas.saveLayer(0, 0, canvasWidth, canvasHeight, null, Canvas.ALL_SAVE_FLAG);
        {
            bgPaint.setColor(bgColor);
            //writePaint.setColor(textColor);
            //透明背景框
            canvas.drawRoundRect(mRectF, rectRadius, rectRadius, mPaint);
            // draw the background of progress
            canvas.drawRoundRect(bgRect, rectRadius, rectRadius, bgPaint);
            //draw text in rect center
            /*Log.d("RectProgress", "progressRect.width():" + progressRect.width());
            Log.d("RectProgress", "progressRect.height():" + progressRect.height());*/
            // draw progress
            canvas.drawRect(progressRect, progressPaint);
            bgPaint.setXfermode(null);
            if (orientation == VERTICAL) {
                canvas.drawText("测试", progressRect.width() / 2 - textSize, (bgRect.height() - progressRect.height()) / 2, writePaint);
            } else if (orientation == HORIZONTAL) {
                baseLineY = (int) (progressRect.centerY() - top / 2 - bottom / 2);//基线中间点的y轴计算公式
                //当大于0时绘制
                if (progress > 0) {
                    canvas.drawText("支付宝", progressRect.centerX(), baseLineY - textSize - 24, writePaint);
                    canvas.drawText("余额", progressRect.centerX(), baseLineY, writePaint);
                    canvas.drawText(progress * 1000 + "", progressRect.centerX(), baseLineY + textSize + 24, writePaint);
                }
                if (progress < max) {
                    canvas.drawText("余额宝", progressRect.width() + (bgRect.width() - progressRect.width()) / 2 + TRANSPARENT_WIDTH, baseLineY - textSize - 24, writePaint);
                    canvas.drawText("余额", progressRect.width() + (bgRect.width() - progressRect.width()) / 2 + TRANSPARENT_WIDTH, baseLineY, writePaint);
                    canvas.drawText((max - progress) * 1000 + "", progressRect.width() + (bgRect.width() - progressRect.width()) / 2 + TRANSPARENT_WIDTH, baseLineY + textSize + 24, writePaint);
                }
            }
            //draw icon bitmap
            canvas.drawBitmap(iconBitmap, progressRect.width() - iconBitmap.getWidth() / 2 + TRANSPARENT_WIDTH,
                    progressRect.height() - iconBitmap.getHeight() + TRANSPARENT_WIDTH - 20, null);

            if (bitmap != null) {
                //draw icon
                //canvas.drawBitmap(bitmap, srcRect, dstRect, bgPaint);
            }
        }
        canvas.restoreToCount(layerId);
        // TODO: 弄明白为什么在xml预览中,canvas.restoreToCount
        // TODO: 会导致后续的canvas对象为空 但canvas.restore方法则不会导致这个问题
//        canvas.restore();
//        canvas.save();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //在加进度条内才执行操作
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mRectF.contains(event.getX(), event.getY())) {
                    //按下时,在进度内才执行操作
                    //handleTouch(event);
                    //invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouch(event);
                invalidate();
                break;
        }
        //invalidate();
        return super.onTouchEvent(event);
    }

    private void handleTouch(MotionEvent event) {
        if (orientation == VERTICAL) {
            if (event.getY() < bgRect.top) {
                //触点超出Progress顶部
                progressRect.top = bgRect.top;
            } else if (event.getY() > bgRect.bottom) {
                //触点超过Progress底部
                progressRect.top = bgRect.bottom;
            } else {
                progressRect.top = event.getY();
            }
            int tmp = (int) ((progressRect.height() / bgRect.height()) * 100);
            if (percent != tmp) {
                percent = tmp;
                progress = percent * max / 100;
                if (changedListener != null)
                    changedListener.onProgressChanged(progress, percent);
            }
        } else {
            if (event.getX() > bgRect.right) {
                //触点超出Progress右端
                progressRect.right = bgRect.right;
            } else if (event.getX() < bgRect.left) {
                //触点超出Progress左端
                progressRect.right = bgRect.left;
            } else {
                progressRect.right = event.getX();
            }
            int tmp = (int) ((progressRect.width() / bgRect.width()) * 100);
            if (percent != tmp) {
                percent = tmp;
                progress = percent * max / 100;
                if (changedListener != null)
                    changedListener.onProgressChanged(progress, percent);
            }
        }
    }

    //设置滚动变化的监听
    public void setChangedListener(OnProgressChangedListener changedListener) {
        this.changedListener = changedListener;
    }

    //设置最大值
    public void setMax(int m) {
        if (max <= 0)
            throw new RuntimeException("Max 必须大于 0");
        max = m;
    }

    //设置当前的进度
    public void setProgress(int p) {
        int oldProgress = progress;
        progress = p;
        if (max < progress) {
            progress = max;
        } else if (progress < 0)
            progress = 0;

        startProgressAnim(oldProgress);
    }

    //设置文字颜色
    public void setTextColor(int color) {
        writePaint.setColor(color);
    }

    //设置文字大小
    public void setTextSize(int size) {
        writePaint.setTextSize(size);
    }


    /**/
    private void startProgressAnim(int oldProgress) {
        if (valueAnimator != null && valueAnimator.isRunning()) {
            valueAnimator.cancel();
        }
        valueAnimator = ValueAnimator.ofInt(oldProgress, progress);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (int) animation.getAnimatedValue();
                computeProgressRect();
                invalidate();
            }
        });
        valueAnimator.setDuration(1000);
        valueAnimator.start();
    }

    /**
     * 计算进度Progress
     */
    private void computeProgressRect() {
        if (orientation == VERTICAL) {
            progressRect.set(bgRect.left
                    , bgRect.bottom - progress * bgRect.height() / max
                    , bgRect.right
                    , bgRect.bottom);
        } else {
            progressRect.set(bgRect.left
                    , bgRect.top
                    , bgRect.left + progress * bgRect.width() / max
                    , bgRect.bottom);
        }
    }

    public interface OnProgressChangedListener {
        void onProgressChanged(int currentValue, int percent);
    }
}
