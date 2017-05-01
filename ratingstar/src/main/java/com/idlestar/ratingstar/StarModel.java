package com.idlestar.ratingstar;

import android.graphics.RectF;

/**
 * Holds all vertexes (x,y) and bounds (outerRect) info about a drawing Star.
 * used by {@link RatingStarView}, Most calculations (about x,y、size etc.) is done here.
 *
 * <h1>[Based Idea or Concept] </h1>
 *
 * ## Standard Coordinate:<br />
 *  The coordinate is —— toward right for x+ ,toward up for y+ .
 * <br /><br />
 * ## 5 outer vertexes<br />
 * The outer circle's (means "circumcircle") radius is 1f, original point O is the star's center,
 * so, the 5 vertexes at 5 outer corner is (from top A, at clockwise order):
 *
 *  <li>A（0,1）</li>
 *  <li>B(cos18°,sin18°)</li>
 *  <li>C(cos54°,-sin54°)</li>
 *  <li>D(-cos54°,-sin54°)</li>
 *  <li>E(-cos18°,sin18°)</li>
 * </p>
 * Created by hxw on 2017-04-23.
 */
class StarModel {

    private static final String TAG = "StarModel";
    public static final float DEFAULT_THICKNESS = 0.5f;
    public static final float MIN_THICKNESS = 0.3f;
    public static final float MAX_THICKNESS = 0.9f;
    public static final float DEFAULT_SCALE_FACTOR = 0.9511f;
    private float currentScaleFactor = DEFAULT_SCALE_FACTOR;
    private RectF outerRect = new RectF();
    private float currentThicknessFactor = DEFAULT_THICKNESS;

    /**
     * @param thicknessFactor see {@link #setThickness(float)}
     */
    public StarModel(float thicknessFactor) {
        reset(thicknessFactor);
    }

    public StarModel() {
        this(DEFAULT_THICKNESS);
    }

    /**
     * Reset all vertexes values to based on radius-1f, will call adjustCoordinate() automatically,
     * So after reset() the Coordinate is match with Android.
     *
     * @param thickness {@link #setThicknessOnStandardCoordinate }
     */
    private void reset(float thickness) {
        currentScaleFactor = DEFAULT_SCALE_FACTOR;
        initAllVertexesToStandard();
        updateOuterRect();
        setThicknessOnStandardCoordinate(thickness);
        adjustCoordinate();
    }

    public void setDrawingOuterRect(int left, int top, int height) {
        // ScaleFactor=1f means width is 1f
        float resizeFactor = height / aspectRatio;
        offsetStar(-outerRect.left, -outerRect.top);
        changeScaleFactor(resizeFactor);
        offsetStar(left, top);
        updateOuterRect();
    }

    public void moveStarTo(float left, float top) {
        float offsetX = left - outerRect.left;
        float offsetY = left - outerRect.top;
        offsetStar(offsetX, offsetY);
        updateOuterRect();
    }

    // region vertexes fields

    /**
     * 10 float values for star's 5 vertex's (x,y) —— outer circle's radius is 1f (
     * NOTE: In the "Standard Coordinate".) , first vertex is for top corner, in clockwise order.
     */
    private static final float[] starVertexes = new float[]{
            -0.9511f, 0.3090f,         // E (left)
            0.0000f, 1.0000f,         // A (top vertex)
            0.9511f, 0.3090f,         // B (right)
            0.5878f, -0.8090f,        // C (bottom right)
            -0.5878f, -0.8090f,        // D (bottom left)
    };

    /**
     * ratio = height / width.
     * width is think as 1f, because the star's width is lager.
     * NOTE: In the　"Standard Coordinate"
     */
    private static final float aspectRatio
            = (starVertexes[3] - starVertexes[7]) / (starVertexes[4] - starVertexes[0]);

    /**
     * firstVertex is vertex: E (very left one)
     *
     * @see  StarModel
     */
    private VertexF firstVertex;

    /**
     * All star vertexes, from the most left one. then clockwise.
     *
     * NOTE: init or update by {@link #initAllVertexesToStandard() }
     *
     * @see #firstVertex
     * @see #starVertexes
     */
    private VertexF[] vertexes;

    // endregion

    private void initAllVertexesToStandard() {
        if (firstVertex == null) {
            firstVertex = new VertexF(starVertexes[0], starVertexes[1]);
        } else {
            firstVertex.x = starVertexes[0];
            firstVertex.y = starVertexes[1];
        }

        // create all 10 vertexes into #vertexes
        if (vertexes == null) {
            vertexes = new VertexF[10];
            vertexes[0] = firstVertex;

            for (int i = 1; i < 10; i++) {
                vertexes[i] = new VertexF();
                vertexes[i - 1].next = vertexes[i];
            }

            // link tail and head
            vertexes[9].next = vertexes[0];
        }

        // update all 5 outer vertexes.
        VertexF current = firstVertex;
        for (int i = 0; i < 5; i++) {
            current.x = starVertexes[i * 2];
            current.y = starVertexes[i * 2 + 1];

            current = current.next.next;
        }

        // update all 5 inner vertexes.
        VertexF prevOuter = firstVertex;
        for (int i = 0; i < 5; i++) {
            VertexF innerV = prevOuter.next;

            innerV.x = (prevOuter.x + innerV.next.x) / 2f;
            innerV.y = (prevOuter.y + innerV.next.y) / 2f;

            prevOuter = innerV.next;
        }
    }

    /**
     * Get vertex at index in {@link #vertexes}
     * @param index see {@link #vertexes}
     */
    public VertexF getVertex(int index) {
        return vertexes[index];
    }

    public RectF getOuterRect() {
        return new RectF(outerRect);
    }

    /**
     * Keep the star's outer bounds exactly.
     * NOTE: call this after any vertex value changed.
     */
    private void updateOuterRect() {
        outerRect.top = vertexes[2].y;
        outerRect.right = vertexes[4].x;
        outerRect.bottom = vertexes[8].y;
        outerRect.left = vertexes[0].x;
    }

    private void offsetStar(float left, float top) {
        for (int i = 0; i < vertexes.length; i++) {
            vertexes[i].x += left;
            vertexes[i].y += top;
        }
    }

    private void changeScaleFactor(float newFactor) {
        float scale = newFactor / currentScaleFactor;
        if (scale == 1f) return;
        for (int i = 0; i < vertexes.length; i++) {
            vertexes[i].x *= scale;
            vertexes[i].y *= scale;
        }
        currentScaleFactor = newFactor;
    }

    /**
     * change the thickness of star.
     * value {@link #DEFAULT_THICKNESS}is about to make a standard star.
     *
     * @param factor between {@link #MIN_THICKNESS} and {@link #MAX_THICKNESS}.
     */
    public void setThickness(float factor) {
        if (currentThicknessFactor == factor) return;
        float oldScale = currentScaleFactor;
        float left = outerRect.left;
        float top = outerRect.top;

        reset(factor);

        changeScaleFactor(oldScale);
        moveStarTo(left, top);
    }

    private void setThicknessOnStandardCoordinate(float thicknessFactor) {
        if (thicknessFactor < MIN_THICKNESS) {
            thicknessFactor = MIN_THICKNESS;
        } else if (thicknessFactor > MAX_THICKNESS) {
            thicknessFactor = MAX_THICKNESS;
        }

        for (int i = 1; i < vertexes.length; i += 2) {
            vertexes[i].x *= thicknessFactor;
            vertexes[i].y *= thicknessFactor;
        }

        currentThicknessFactor = thicknessFactor;
    }

    /**
     * reverse Y, and move to y=0
     */
    private void adjustCoordinate() {
        float offsetX = -outerRect.left;
        float offsetY = outerRect.top;

        for (int i = 0; i < vertexes.length; i++) {
            vertexes[i].y = -vertexes[i].y + offsetY;
            vertexes[i].x += offsetX;

            // standard value is in radius = 1f, so..
            vertexes[i].x /= 2f;
            vertexes[i].y /= 2f;
        }

        updateOuterRect();
    }

    /**
     * ratio = height / width. width is think as 1f, because the star's width is lager.
     * NOTE: In the　"Standard Coordinate"
     *
     * @return ratio = height / width.
     */
    public static float getOuterRectAspectRatio() {
        return aspectRatio;
    }

    public static float getStarWidth(float starHeight) {
        return  starHeight / getOuterRectAspectRatio();
    }
}
