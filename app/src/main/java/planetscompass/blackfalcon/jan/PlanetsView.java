package planetscompass.blackfalcon.jan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

/**
 * Compass-shaped sky map. Centre = straight overhead, rim = horizon.
 * The dial turns with the phone so the direction you face is at the top.
 */
public class PlanetsView extends View {

    private static final int GOLD = 0xFFC9A227;
    private static final String[] DIRS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final String[] SHORT = {"Sun", "Moon", "Mer", "Ven", "Mar", "Jup", "Sat", "Ura", "Nep"};
    private static final int[] COLORS = {
            0xFFFFD54F, 0xFFE0E0E0, 0xFFB0B0B0, 0xFFFFF3C4, 0xFFFF5722,
            0xFFE8B070, 0xFFEAD27A, 0xFF7FE5E5, 0xFF5C7CFA};
    private static final float[] SIZE_DP = {11f, 9f, 5f, 7f, 6f, 8f, 7f, 5f, 5f};

    private final float dp;
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dirPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint creditPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path marker = new Path();

    private Astronomy.Body[] bodies;
    private boolean hasLocation;
    private float heading;
    private String locText = "";
    private String message = "";

    public PlanetsView(Context context) {
        super(context);
        dp = getResources().getDisplayMetrics().density;
        float sp = getResources().getDisplayMetrics().scaledDensity;

        ringPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setColor(0xFFB0B6BD);
        tickPaint.setStrokeWidth(1.5f * dp);

        dirPaint.setTextAlign(Paint.Align.CENTER);
        dirPaint.setTypeface(Typeface.DEFAULT_BOLD);

        labelPaint.setTextSize(11 * sp);

        smallPaint.setColor(0xFF9AA0A6);
        smallPaint.setTextSize(11 * sp);

        creditPaint.setColor(GOLD);
        creditPaint.setTextAlign(Paint.Align.RIGHT);
        creditPaint.setTypeface(Typeface.DEFAULT_BOLD);
        creditPaint.setTextSize(15 * sp);

        messagePaint.setColor(Color.WHITE);
        messagePaint.setTextAlign(Paint.Align.CENTER);
        messagePaint.setTextSize(16 * sp);

        markerPaint.setColor(GOLD);
        markerPaint.setStyle(Paint.Style.FILL);
    }

    public void setState(Astronomy.Body[] bodies, boolean hasLocation, float heading,
                         String locText, String message) {
        this.bodies = bodies;
        this.hasLocation = hasLocation;
        this.heading = heading;
        this.locText = locText == null ? "" : locText;
        this.message = message == null ? "" : message;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        canvas.drawColor(Color.BLACK);

        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) * 0.33f;

        // mandatory credit, top-right corner
        canvas.drawText("By: Black Falcon", w - 16 * dp, 16 * dp + creditPaint.getTextSize(), creditPaint);

        // position, top-left corner
        float ty = 16 * dp + smallPaint.getTextSize();
        for (String line : locText.split("\n")) {
            canvas.drawText(line, 16 * dp, ty, smallPaint);
            ty += smallPaint.getTextSize() * 1.25f;
        }

        // rings: horizon, 30 and 60 degrees of altitude
        ringPaint.setColor(0xCCFFFFFF);
        ringPaint.setStrokeWidth(2 * dp);
        canvas.drawCircle(cx, cy, radius, ringPaint);
        ringPaint.setColor(0x44FFFFFF);
        ringPaint.setStrokeWidth(1 * dp);
        canvas.drawCircle(cx, cy, radius * 2f / 3f, ringPaint);
        canvas.drawCircle(cx, cy, radius / 3f, ringPaint);

        // centre cross
        canvas.drawLine(cx - 6 * dp, cy, cx + 6 * dp, cy, ringPaint);
        canvas.drawLine(cx, cy - 6 * dp, cx, cy + 6 * dp, ringPaint);

        // ticks every 10 degrees and the direction letters (they turn with the dial)
        for (int az = 0; az < 360; az += 10) {
            double th = Math.toRadians(az - heading);
            float s = (float) Math.sin(th);
            float c = (float) Math.cos(th);
            float len = (az % 30 == 0) ? 10 * dp : 5 * dp;
            canvas.drawLine(cx + radius * s, cy - radius * c,
                    cx + (radius + len) * s, cy - (radius + len) * c, tickPaint);
        }
        for (int i = 0; i < 8; i++) {
            double th = Math.toRadians(i * 45 - heading);
            float s = (float) Math.sin(th);
            float c = (float) Math.cos(th);
            boolean cardinal = i % 2 == 0;
            dirPaint.setTextSize((cardinal ? 20 : 12) * getResources().getDisplayMetrics().scaledDensity);
            dirPaint.setColor(i == 0 ? 0xFFFF5252 : (cardinal ? Color.WHITE : 0xFF9AA0A6));
            float rr = radius + 26 * dp;
            canvas.drawText(DIRS[i], cx + rr * s, cy - rr * c + dirPaint.getTextSize() * 0.35f, dirPaint);
        }

        // marker: the way you are facing
        float yTop = cy - radius - 52 * dp;
        marker.reset();
        marker.moveTo(cx, yTop + 12 * dp);
        marker.lineTo(cx - 7 * dp, yTop);
        marker.lineTo(cx + 7 * dp, yTop);
        marker.close();
        canvas.drawPath(marker, markerPaint);

        if (hasLocation && bodies != null) {
            for (int i = bodies.length - 1; i >= 0; i--) { // Sun and Moon drawn last, on top
                Astronomy.Body b = bodies[i];
                double th = Math.toRadians(b.azimuth - heading);
                boolean up = b.altitude >= 0;
                float r = up
                        ? (float) (radius * (90.0 - b.altitude) / 90.0)
                        : radius + dp * (5f + 10f * (float) Math.min(1.0, -b.altitude / 45.0));
                float x = cx + r * (float) Math.sin(th);
                float y = cy - r * (float) Math.cos(th);
                float rad = SIZE_DP[i] * dp;
                int color = COLORS[i];

                if (up) {
                    dotPaint.setStyle(Paint.Style.FILL);
                    dotPaint.setColor(color);
                    labelPaint.setColor(color);
                } else { // below the horizon: hollow and dim
                    int dim = (color & 0x00FFFFFF) | 0x99000000;
                    dotPaint.setStyle(Paint.Style.STROKE);
                    dotPaint.setStrokeWidth(2 * dp);
                    dotPaint.setColor(dim);
                    labelPaint.setColor(dim);
                }
                canvas.drawCircle(x, y, rad, dotPaint);
                canvas.drawText(SHORT[i], x + rad + 3 * dp, y + labelPaint.getTextSize() * 0.35f, labelPaint);
            }
        } else {
            float y = cy - 10 * dp;
            for (String line : message.split("\n")) {
                canvas.drawText(line, cx, y, messagePaint);
                y += messagePaint.getTextSize() * 1.5f;
            }
        }

        // legend
        smallPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("centre = overhead  \u2022  rim = horizon  \u2022  hollow = below horizon",
                cx, h - 8 * dp, smallPaint);
        smallPaint.setTextAlign(Paint.Align.LEFT);
    }
}
