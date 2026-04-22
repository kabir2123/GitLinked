package com.example.gitlinked.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Grid-based proximity radar that shows nearby developers as dots.
 * Center = YOU, other dots = discovered developers.
 * Distance estimated from BLE RSSI signal strength.
 *
 * Visual features:
 * - Grid with subtle cell borders
 * - Pulsing center dot (you)
 * - Animated developer dots that drift slightly (real-time feel)
 * - Density-highlighted cells (amber glow where developers are)
 * - Sweep line animation
 * - Legend bar at bottom
 */
public class RadarView extends View {

    // Grid configuration
    private static final int GRID_COLS = 7;
    private static final int GRID_ROWS = 7;
    private static final int CENTER_COL = 3;
    private static final int CENTER_ROW = 3;

    // Colors
    private static final int COLOR_BG = Color.parseColor("#1A1D23");
    private static final int COLOR_GRID_LINE = Color.parseColor("#2A2D35");
    private static final int COLOR_GRID_CELL = Color.parseColor("#22252D");
    private static final int COLOR_DENSITY = Color.parseColor("#B8860B"); // Amber/gold
    private static final int COLOR_YOU = Color.parseColor("#FF4D6A");     // Pink-red
    private static final int COLOR_DEV = Color.parseColor("#5B9CE6");     // Blue
    private static final int COLOR_DEV_RING = Color.parseColor("#3A6CA8");
    private static final int COLOR_TEXT = Color.parseColor("#E0E0E0");
    private static final int COLOR_TEXT_DIM = Color.parseColor("#707580");
    private static final int COLOR_SWEEP = Color.parseColor("#2AEFC0");  // Cyan-green
    private static final int COLOR_ACTIVE = Color.parseColor("#2ECC71"); // Green

    // Paints
    private Paint paintBg, paintGridLine, paintGridCell;
    private Paint paintDensity, paintYou, paintYouGlow;
    private Paint paintDev, paintDevRing, paintDevLabel;
    private Paint paintText, paintTextDim, paintSweep;
    private Paint paintActive, paintActiveBg;
    private Paint paintTitle, paintSubtitle, paintLegend;

    // Developer data
    private List<RadarDot> developers = new ArrayList<>();
    private boolean isActive = false;

    // Animation
    private float sweepAngle = 0f;
    private float pulseScale = 1f;
    private ValueAnimator sweepAnimator;
    private ValueAnimator pulseAnimator;
    private Random random = new Random();

    // Cached dimensions
    private float cellWidth, cellHeight;
    private float gridLeft, gridTop, gridRight, gridBottom;
    private float radarPadding = 0;

    /**
     * Represents a developer dot on the radar.
     */
    public static class RadarDot {
        public String username;
        public float gridX; // 0-6 grid position (fractional)
        public float gridY;
        public float targetX; // Target position for animation
        public float targetY;
        public int rssi;      // BLE signal strength
        public boolean isOnline;

        // Drift animation
        float driftX = 0, driftY = 0;
        float driftSpeedX, driftSpeedY;

        public RadarDot(String username, int rssi) {
            this.username = username;
            this.rssi = rssi;
            this.isOnline = true;

            // Estimate distance from RSSI and map to grid position
            float distance = estimateDistance(rssi);
            float angle = (float) (Math.random() * 2 * Math.PI);

            // Map distance to grid cells from center (max ~3 cells out)
            float gridDist = Math.min(distance / 10f, 2.8f); // ~10m per cell
            this.gridX = CENTER_COL + (float) (gridDist * Math.cos(angle));
            this.gridY = CENTER_ROW + (float) (gridDist * Math.sin(angle));
            this.targetX = gridX;
            this.targetY = gridY;

            // Random drift speed for subtle movement
            this.driftSpeedX = (float) (Math.random() * 0.01 - 0.005);
            this.driftSpeedY = (float) (Math.random() * 0.01 - 0.005);
        }

        /**
         * Estimate distance in meters from RSSI using log-distance path loss model.
         * txPower = -59 dBm (typical BLE at 1m)
         * n = 2.0 (path loss exponent for open air)
         */
        private static float estimateDistance(int rssi) {
            int txPower = -59;
            if (rssi == 0) return -1;
            double ratio = (double) rssi / txPower;
            if (ratio < 1.0) {
                return (float) Math.pow(ratio, 10);
            } else {
                return (float) (0.89976 * Math.pow(ratio, 7.7095) + 0.111);
            }
        }
    }

    public RadarView(Context context) {
        super(context);
        init();
    }

    public RadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RadarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        // Background
        paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBg.setColor(COLOR_BG);

        // Grid lines
        paintGridLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGridLine.setColor(COLOR_GRID_LINE);
        paintGridLine.setStrokeWidth(1f);
        paintGridLine.setStyle(Paint.Style.STROKE);

        // Grid cells
        paintGridCell = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGridCell.setColor(COLOR_GRID_CELL);
        paintGridCell.setStyle(Paint.Style.FILL);

        // Density highlight
        paintDensity = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDensity.setColor(Color.argb(80, 184, 134, 11));
        paintDensity.setStyle(Paint.Style.FILL);

        // You dot
        paintYou = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintYou.setColor(COLOR_YOU);
        paintYou.setStyle(Paint.Style.FILL);

        paintYouGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintYouGlow.setColor(Color.argb(60, 255, 77, 106));
        paintYouGlow.setStyle(Paint.Style.FILL);

        // Developer dots
        paintDev = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDev.setColor(COLOR_DEV);
        paintDev.setStyle(Paint.Style.FILL);

        paintDevRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDevRing.setColor(COLOR_DEV_RING);
        paintDevRing.setStrokeWidth(2f);
        paintDevRing.setStyle(Paint.Style.STROKE);

        paintDevLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDevLabel.setColor(COLOR_TEXT);
        paintDevLabel.setTextSize(24f);
        paintDevLabel.setTextAlign(Paint.Align.CENTER);

        // Text paints
        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(COLOR_TEXT);
        paintText.setTextSize(28f);

        paintTextDim = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTextDim.setColor(COLOR_TEXT_DIM);
        paintTextDim.setTextSize(22f);

        // Sweep line
        paintSweep = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSweep.setColor(Color.argb(40, 42, 239, 192));
        paintSweep.setStrokeWidth(2f);

        // Active badge
        paintActive = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintActive.setColor(COLOR_ACTIVE);
        paintActive.setTextSize(22f);
        paintActive.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        paintActiveBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintActiveBg.setColor(Color.argb(40, 46, 204, 113));
        paintActiveBg.setStyle(Paint.Style.FILL);

        // Title & subtitle
        paintTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTitle.setColor(COLOR_TEXT);
        paintTitle.setTextSize(32f);
        paintTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintTitle.setLetterSpacing(0.15f);

        paintSubtitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSubtitle.setColor(COLOR_TEXT_DIM);
        paintSubtitle.setTextSize(22f);
        paintSubtitle.setLetterSpacing(0.05f);

        // Legend
        paintLegend = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLegend.setColor(COLOR_TEXT_DIM);
        paintLegend.setTextSize(20f);

        startAnimations();
    }

    private void startAnimations() {
        // Sweep animation (rotating line)
        sweepAnimator = ValueAnimator.ofFloat(0f, 360f);
        sweepAnimator.setDuration(4000);
        sweepAnimator.setRepeatCount(ValueAnimator.INFINITE);
        sweepAnimator.setInterpolator(new LinearInterpolator());
        sweepAnimator.addUpdateListener(anim -> {
            sweepAngle = (float) anim.getAnimatedValue();

            // Update developer dot drift
            for (RadarDot dot : developers) {
                dot.driftX += dot.driftSpeedX;
                dot.driftY += dot.driftSpeedY;

                // Bounce drift
                if (Math.abs(dot.driftX) > 0.3f) dot.driftSpeedX = -dot.driftSpeedX;
                if (Math.abs(dot.driftY) > 0.3f) dot.driftSpeedY = -dot.driftSpeedY;
            }

            invalidate();
        });
        sweepAnimator.start();

        // Pulse animation for center dot
        pulseAnimator = ValueAnimator.ofFloat(0.8f, 1.2f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.addUpdateListener(anim -> pulseScale = (float) anim.getAnimatedValue());
        pulseAnimator.start();
    }

    // ==================== Public API ====================

    /**
     * Set the list of discovered developers on the radar.
     */
    public void setDevelopers(List<RadarDot> devs) {
        this.developers.clear();
        this.developers.addAll(devs);
        invalidate();
    }

    /**
     * Add a single developer dot with live RSSI.
     */
    public void addDeveloper(String username, int rssi) {
        // Check if already exists
        for (RadarDot dot : developers) {
            if (dot.username.equals(username)) {
                // Update position based on new RSSI
                updateDotRssi(dot, rssi);
                return;
            }
        }
        developers.add(new RadarDot(username, rssi));
        invalidate();
    }

    /**
     * Update a dot's position based on new RSSI reading.
     */
    private void updateDotRssi(RadarDot dot, int newRssi) {
        dot.rssi = newRssi;
        float distance = RadarDot.estimateDistance(newRssi);
        float angle = (float) Math.atan2(dot.gridY - CENTER_ROW, dot.gridX - CENTER_COL);

        float gridDist = Math.min(distance / 10f, 2.8f);
        dot.targetX = CENTER_COL + (float) (gridDist * Math.cos(angle));
        dot.targetY = CENTER_ROW + (float) (gridDist * Math.sin(angle));

        // Smooth lerp towards target
        dot.gridX += (dot.targetX - dot.gridX) * 0.1f;
        dot.gridY += (dot.targetY - dot.gridY) * 0.1f;
    }

    /**
     * Remove all developers from the radar.
     */
    public void clearDevelopers() {
        developers.clear();
        invalidate();
    }

    public void setActive(boolean active) {
        this.isActive = active;
        invalidate();
    }

    // ==================== Drawing ====================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // Background
        canvas.drawRect(0, 0, w, h, paintBg);

        // Calculate grid dimensions
        float headerHeight = 120f;
        float footerHeight = 60f;
        float padding = 24f;
        radarPadding = padding;

        float gridAreaWidth = w - padding * 2;
        float gridAreaHeight = h - headerHeight - footerHeight - padding;
        float gridSize = Math.min(gridAreaWidth, gridAreaHeight);

        cellWidth = gridSize / GRID_COLS;
        cellHeight = gridSize / GRID_ROWS;

        gridLeft = (w - gridSize) / 2f;
        gridTop = headerHeight;
        gridRight = gridLeft + gridSize;
        gridBottom = gridTop + gridSize;

        // Draw header
        drawHeader(canvas, w);

        // Draw grid
        drawGrid(canvas);

        // Draw density highlights
        drawDensityHighlights(canvas);

        // Draw sweep line
        drawSweepLine(canvas);

        // Draw developer dots
        drawDeveloperDots(canvas);

        // Draw center dot (YOU)
        drawCenterDot(canvas);

        // Draw legend/footer
        drawFooter(canvas, w, h);
    }

    private void drawHeader(Canvas canvas, int width) {
        canvas.drawText("PROXIMITY RADAR", gridLeft, 45f, paintTitle);
        canvas.drawText("LIVE DEVELOPER DENSITY", gridLeft, 80f, paintSubtitle);

        // Active badge
        if (isActive) {
            float badgeRight = width - radarPadding;
            float badgeWidth = paintActive.measureText("● ACTIVE") + 32;
            RectF badgeBg = new RectF(badgeRight - badgeWidth, 25f, badgeRight, 60f);
            canvas.drawRoundRect(badgeBg, 16f, 16f, paintActiveBg);
            canvas.drawText("● ACTIVE", badgeRight - badgeWidth + 16, 50f, paintActive);
        }

        // Grid coverage label
        paintTextDim.setTextSize(18f);
        canvas.drawText("50m Coverage (Grid)", gridLeft + 4, gridTop + 20, paintTextDim);
        paintTextDim.setTextSize(22f);
    }

    private void drawGrid(Canvas canvas) {
        // Draw cells
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                float x = gridLeft + col * cellWidth;
                float y = gridTop + row * cellHeight;
                RectF cell = new RectF(x + 1, y + 1, x + cellWidth - 1, y + cellHeight - 1);
                canvas.drawRoundRect(cell, 4f, 4f, paintGridCell);
            }
        }

        // Draw grid lines
        for (int i = 0; i <= GRID_COLS; i++) {
            float x = gridLeft + i * cellWidth;
            canvas.drawLine(x, gridTop, x, gridBottom, paintGridLine);
        }
        for (int i = 0; i <= GRID_ROWS; i++) {
            float y = gridTop + i * cellHeight;
            canvas.drawLine(gridLeft, y, gridRight, y, paintGridLine);
        }
    }

    private void drawDensityHighlights(Canvas canvas) {
        // Highlight cells that contain developers
        for (RadarDot dot : developers) {
            int col = Math.round(dot.gridX + dot.driftX);
            int row = Math.round(dot.gridY + dot.driftY);

            if (col >= 0 && col < GRID_COLS && row >= 0 && row < GRID_ROWS) {
                float x = gridLeft + col * cellWidth;
                float y = gridTop + row * cellHeight;
                RectF cell = new RectF(x + 2, y + 2, x + cellWidth - 2, y + cellHeight - 2);
                canvas.drawRoundRect(cell, 4f, 4f, paintDensity);
            }
        }
    }

    private void drawSweepLine(Canvas canvas) {
        if (!isActive) return;

        float centerX = gridLeft + CENTER_COL * cellWidth + cellWidth / 2;
        float centerY = gridTop + CENTER_ROW * cellHeight + cellHeight / 2;
        float maxRadius = (gridRight - gridLeft) / 2f;

        // Draw sweep arc (fading trail)
        float radians = (float) Math.toRadians(sweepAngle);
        float endX = centerX + maxRadius * (float) Math.cos(radians);
        float endY = centerY + maxRadius * (float) Math.sin(radians);

        Paint sweepLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sweepLinePaint.setColor(Color.argb(60, 42, 239, 192));
        sweepLinePaint.setStrokeWidth(2f);
        canvas.drawLine(centerX, centerY, endX, endY, sweepLinePaint);

        // Draw fading sweep arc area
        Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.FILL);
        arcPaint.setShader(new LinearGradient(
                centerX, centerY, endX, endY,
                Color.argb(25, 42, 239, 192),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));

        // Draw a few trailing lines for sweep effect
        for (int i = 1; i <= 8; i++) {
            float trailAngle = (float) Math.toRadians(sweepAngle - i * 3);
            float trailEndX = centerX + maxRadius * (float) Math.cos(trailAngle);
            float trailEndY = centerY + maxRadius * (float) Math.sin(trailAngle);
            int alpha = Math.max(0, 40 - i * 5);
            sweepLinePaint.setColor(Color.argb(alpha, 42, 239, 192));
            canvas.drawLine(centerX, centerY, trailEndX, trailEndY, sweepLinePaint);
        }
    }

    private void drawCenterDot(Canvas canvas) {
        float cx = gridLeft + CENTER_COL * cellWidth + cellWidth / 2;
        float cy = gridTop + CENTER_ROW * cellHeight + cellHeight / 2;

        // Pulsing glow
        float glowRadius = 20f * pulseScale;
        canvas.drawCircle(cx, cy, glowRadius, paintYouGlow);

        // Outer ring
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(Color.argb(100, 255, 77, 106));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f);
        canvas.drawCircle(cx, cy, 14f * pulseScale, ringPaint);

        // Center dot
        canvas.drawCircle(cx, cy, 8f, paintYou);
    }

    private void drawDeveloperDots(Canvas canvas) {
        for (RadarDot dot : developers) {
            float dotX = gridLeft + (dot.gridX + dot.driftX) * cellWidth + cellWidth / 2;
            float dotY = gridTop + (dot.gridY + dot.driftY) * cellHeight + cellHeight / 2;

            // Clamp to grid bounds
            dotX = Math.max(gridLeft + 10, Math.min(gridRight - 10, dotX));
            dotY = Math.max(gridTop + 10, Math.min(gridBottom - 10, dotY));

            // Outer ring
            canvas.drawCircle(dotX, dotY, 14f, paintDevRing);

            // Inner dot
            canvas.drawCircle(dotX, dotY, 7f, paintDev);

            // Subtle glow
            Paint devGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
            devGlow.setColor(Color.argb(30, 91, 156, 230));
            canvas.drawCircle(dotX, dotY, 18f, devGlow);

            // Username label (truncated)
            String label = dot.username;
            if (label.length() > 8) label = label.substring(0, 7) + "…";
            paintDevLabel.setTextSize(20f);
            canvas.drawText(label, dotX, dotY - 22f, paintDevLabel);
        }
    }

    private void drawFooter(Canvas canvas, int width, int height) {
        float footerY = gridBottom + 40;

        // YOU legend
        canvas.drawCircle(gridLeft + 10, footerY - 5, 6f, paintYou);
        canvas.drawText("YOU", gridLeft + 26, footerY, paintLegend);

        // DEVS legend
        float devsX = gridLeft + 100;
        canvas.drawCircle(devsX + 10, footerY - 5, 6f, paintDev);
        canvas.drawText("DEVS", devsX + 26, footerY, paintLegend);

        // Sector label
        String sector = "SECTOR " + (CENTER_ROW + 1) + "-" + (char) ('A' + CENTER_COL);
        float sectorWidth = paintLegend.measureText(sector);
        canvas.drawText(sector, gridRight - sectorWidth, footerY, paintLegend);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (sweepAnimator != null) sweepAnimator.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (sweepAnimator != null && !sweepAnimator.isRunning()) sweepAnimator.start();
        if (pulseAnimator != null && !pulseAnimator.isRunning()) pulseAnimator.start();
    }
}
