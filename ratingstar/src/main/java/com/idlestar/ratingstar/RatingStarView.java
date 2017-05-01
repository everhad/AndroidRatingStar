package com.idlestar.ratingstar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import com.idlestar.ratingstar.R;

import static android.graphics.Canvas.CLIP_SAVE_FLAG;

/**
 * RatingStar is specific RatingBar use star drawable as the progress mark.
 *
 * NOTE:
 * Padding will be larger if is {@link #cornerRadius} is set (No exact calc to handle this issue).
 */
public class RatingStarView extends View implements View.OnClickListener {
    private static final String TAG = "RatingStarView";
    private static final int DEFAULT_STAR_HEIGHT = 32;
    private float cornerRadius = 4f;
    private int starForegroundColor = 0xffED4A4B;
    private int strokeColor = 0xffED4A4B;
    private int starBackgroundColor = Color.WHITE;
    /** used to make round smooth star horn */
    private CornerPathEffect pathEffect;
    private ArrayList<StarModel> starList;
    private float rating;
    /**
     * expected star number.
     */
    private int starNum = 5;
    /**
     * real drawn star number.
     */
    private int starCount;
    /** calculated value */
    private float starWidth;
    /** calculated value */
    private float starHeight;
    private float starMargin = 8;
    private float strokeWidth = 2f;
    private boolean drawStrokeForFullStar;
    private boolean drawStrokeForHalfStar = true;
    private boolean drawStrokeForEmptyStar = true;
    private boolean enableSelectRating = false;
    private float starThicknessFactor = StarModel.DEFAULT_THICKNESS;
    private float dividerX;
    private float clickedX, clickedY;
    private Paint paint;
    private OnClickListener mOuterOnClickListener;

    // region constructors
    public RatingStarView(Context context) {
        super(context);
        init(null, 0);
    }

    public RatingStarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public RatingStarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }
    // endregion

    private void init(AttributeSet attrs, int defStyle) {
        loadAttributes(attrs, defStyle);

        // init paint
        paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(strokeWidth);

        // properties
        pathEffect = new CornerPathEffect(cornerRadius);

        // click to rate
        super.setOnClickListener(this);
    }

    private void loadAttributes(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.RatingStarView, defStyle, 0);
        strokeColor = a.getColor(R.styleable.RatingStarView_strokeColor, strokeColor);
        starForegroundColor = a.getColor(R.styleable.RatingStarView_starForegroundColor, starForegroundColor);
        starBackgroundColor = a.getColor(R.styleable.RatingStarView_starBackgroundColor, starBackgroundColor);
        cornerRadius = a.getDimension(R.styleable.RatingStarView_cornerRadius, cornerRadius);
        starMargin = a.getDimension(R.styleable.RatingStarView_starMargin, starMargin);
        strokeWidth = a.getDimension(R.styleable.RatingStarView_strokeWidth, strokeWidth);
        starThicknessFactor = a.getFloat(R.styleable.RatingStarView_starThickness, starThicknessFactor);
        rating = a.getFloat(R.styleable.RatingStarView_rating, rating);
        starNum = a.getInteger(R.styleable.RatingStarView_starNum, starNum);
        drawStrokeForEmptyStar = a.getBoolean(R.styleable.RatingStarView_drawStrokeForEmptyStar, true);
        drawStrokeForFullStar = a.getBoolean(R.styleable.RatingStarView_drawStrokeForFullStar, false);
        drawStrokeForHalfStar = a.getBoolean(R.styleable.RatingStarView_drawStrokeForHalfStar, true);
        enableSelectRating = a.getBoolean(R.styleable.RatingStarView_enableSelectRating, false);
        a.recycle();
    }

    private void setStarBackgroundColor(int color) {
        starBackgroundColor = color;
        invalidate();
    }

    /**
     * @see StarModel#setThickness(float)
     */
    public void setStarThickness(float thicknessFactor) {
        for (StarModel star : starList) {
            star.setThickness(thicknessFactor);
        }
        invalidate();
    }

    public void setStrokeWidth(float width) {
        this.strokeWidth = width;
        invalidate();
    }

    /**
     * Finally progress is: progress = rating / starNum
     * @param rating should be [0, starNum]
     */
    public void setRating(float rating) {
        if (rating != this.rating) {
            this.rating = rating;
            invalidate();
        }
    }

    /**
     * Set the smooth of the star's horn.
     * @param cornerRadius corner circle radius
     */
    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        invalidate();
    }

    /**
     * The horizontal margin between two stars. The {@link #setCornerRadius} would make extra space
     * as it make the star smaller.
     * @param margin horizontal space
     */
    public void setStarMargin(int margin) {
        this.starMargin = margin;
        calcStars();
        invalidate();
    }

    /**
     * How many stars to show, one star means one score = 1f. See {@link #setRating(float)}<br />
     * NOTE: The star's height is made by contentHeight by default.So, be sure to has defined the
     * correct StarView's height.
     * @param count star count.
     */
    public void setStarNum(int count) {
        if (starNum != count) {
            starNum = count;
            calcStars();
            invalidate();
        }
    }

    private void onPaddingChanged() {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        for (StarModel star : starList) {
            star.moveStarTo(left, top);
        }
    }

    public void setDrawStrokeForFullStar(boolean draw) {
        drawStrokeForFullStar = draw;
    }

    public void setDrawStrokeForEmptyStar(boolean draw) {
        drawStrokeForEmptyStar = draw;
    }

    /**
     * Create all stars data, according to the contentWidth/contentHeight.
     */
    private void calcStars() {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        int left = paddingLeft;
        int top = paddingTop;

        // according to the View's height , make star height.
        int starHeight = contentHeight;
        if (contentHeight > contentWidth) {
            starHeight = contentWidth;
        }

        if (starHeight <= 0) return;
        float startWidth = StarModel.getStarWidth(starHeight);

        // starCount * startWidth + (starCount - 1) * starMargin = contentWidth
        int starCount = (int) ((contentWidth + starMargin) / (startWidth + starMargin));
        if (starCount > starNum) {
            starCount = starNum;
        }

        this.starHeight = starHeight;
        this.starWidth = startWidth;
        Log.d(TAG, "drawing starCount = " + starCount  + ", contentWidth = " + contentWidth
                + ", startWidth = " + startWidth + ", starHeight = " + starHeight);

        starList = new ArrayList<>(starCount);

        for (int i = 0; i < starCount; i++) {
            StarModel star = new StarModel(starThicknessFactor);
            starList.add(star);
            star.setDrawingOuterRect(left, top, starHeight);
            left += startWidth + 0.5f + starMargin;
        }

        this.starCount = starCount;
        this.starWidth = startWidth;
        this.starHeight = starHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float width;
        int height; // must have height

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = DEFAULT_STAR_HEIGHT;
            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(height, heightSize);
            }
        }

        float starHeight = height - getPaddingBottom() - getPaddingTop();

        if (widthMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            width = widthSize;
        } else {
            // get the perfect width
            width = getPaddingLeft() + getPaddingRight();
            if (starNum > 0) {
                if (starHeight > 0) {
                    width += starMargin * (starNum - 1);
                    width += StarModel.getStarWidth(starHeight) * starNum;
                }
            }

            if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(widthSize, width);
            }
        }

        Log.d(TAG, "[onMeasure] width = " + width + ", pLeft = " + getPaddingLeft()
            + ", pRight = " + getPaddingRight() + ", starMargin = " + starMargin
            + ", starHeight = " + starHeight + ", starWidth = " + StarModel.getStarWidth(starHeight));

        int widthInt = (int) (width);
        if (widthInt < width) {
            widthInt++;
        }

        setMeasuredDimension(widthInt, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (starList == null) {
            calcStars();
        }

        if (starList == null || starList.size() == 0) {
            return;
        }

        for (int i = 0; i < starList.size(); i++) {
            if (rating >= i + 1) {
                drawFullStar(starList.get(i), canvas);
            } else {
                float decimal = rating - i;
                if (decimal > 0) {
                    drawPartialStar(starList.get(i), canvas, decimal);
                } else {
                    drawEmptyStar(starList.get(i), canvas);
                }
            }
        }
    }

    private void drawFullStar(StarModel star, Canvas canvas) {
        drawSolidStar(star, canvas, starForegroundColor);
        if (drawStrokeForFullStar) {
            drawStarStroke(star, canvas);
        }
    }

    private void drawEmptyStar(StarModel star, Canvas canvas) {
        drawSolidStar(star, canvas, starBackgroundColor);
        if (drawStrokeForEmptyStar) {
            drawStarStroke(star, canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h != oldh) {
            calcStars();
        }
    }

    private void drawPartialStar(StarModel star, Canvas canvas, float percent) {
        Log.d(TAG, "drawPartialStar percent = " + percent);
        if (percent <= 0) {
            drawEmptyStar(star, canvas);
            return;
        } else if (percent >= 1) {
            drawFullStar(star, canvas);
            return;
        }

        // layer 1
        drawSolidStar(star, canvas, starBackgroundColor);

        float dividerX = star.getOuterRect().left + star.getOuterRect().width() * percent;
        this.dividerX = dividerX;

        // layer 2
        RectF r = star.getOuterRect();
        canvas.saveLayerAlpha(r.left, r.top, r.right, r.bottom, 0xff, CLIP_SAVE_FLAG);
        RectF clip = new RectF(star.getOuterRect());
        clip.right = dividerX;
        canvas.clipRect(clip);
        drawSolidStar(star, canvas, starForegroundColor);
        canvas.restore();

        // layer 1
        if (drawStrokeForHalfStar) {
            drawStarStroke(star, canvas);
        }
    }

    private void drawSolidStar(StarModel star, Canvas canvas, int fillColor) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        paint.setPathEffect(pathEffect);

        VertexF prev = star.getVertex(1);
        Path path = new Path();

        for (int i = 0; i < 5; i++) {
            path.rewind();
            path.moveTo(prev.x, prev.y);

            VertexF next = prev.next;

            path.lineTo(next.x, next.y);
            path.lineTo(next.next.x, next.next.y);
            path.lineTo(next.next.x, next.next.y);
            canvas.drawPath(path, paint);

            prev = next.next;
        }

        // fill the middle hole. use +1.0 +1.5 because the path-API will leave 1px gap.
        path.rewind();
        prev = star.getVertex(1);
        path.moveTo(prev.x - 1f, prev.y - 1f);
        prev = prev.next.next;
        path.lineTo(prev.x + 1.5f, prev.y - 0.5f);
        prev = prev.next.next;
        path.lineTo(prev.x + 1.5f, prev.y + 1f);
        prev = prev.next.next;
        path.lineTo(prev.x, prev.y + 1f);
        prev = prev.next.next;
        path.lineTo(prev.x - 1f, prev.y + 1f);

        paint.setPathEffect(null);
        canvas.drawPath(path, paint);
    }

    private void drawStarStroke(StarModel star, Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(strokeColor);
        paint.setPathEffect(pathEffect);
        VertexF prev = star.getVertex(1);
        Path path = new Path();

        for (int i = 0; i < 5; i++) {
            path.rewind();
            path.moveTo(prev.x, prev.y);

            VertexF next = prev.next;

            path.lineTo(next.x, next.y);
            path.lineTo(next.next.x, next.next.y);
            path.lineTo(next.next.x, next.next.y);

            canvas.drawPath(path, paint);
            prev = next.next;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            clickedX = event.getX();
            clickedY = event.getY();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOuterOnClickListener = l;
    }

    @Override
    public void onClick(View v) {
        if (mOuterOnClickListener != null) {
            mOuterOnClickListener.onClick(v);
        }

        if (enableSelectRating) {
            changeRatingByClick();
        }
    }

    private void changeRatingByClick() {
        int paddingTop = getPaddingTop();
        if (clickedY < paddingTop || clickedY > paddingTop + starHeight) {
            return;
        }

        int paddingLeft = getPaddingLeft();
        float starWidth = this.starWidth;
        float starMargin = this.starMargin;

        float left = paddingLeft;
        for (int i = 1; i <= starCount; i++) {
            float right = left + starWidth;
            if (clickedX >= left && clickedX <= right) {
                if (this.rating == i) {
                    setRating(0);
                } else {
                    setRating(i);
                }
                break;
            }

            left += (starWidth + starMargin);
        }
    }
}
