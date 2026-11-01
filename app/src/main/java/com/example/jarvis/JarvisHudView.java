package com.example.jarvis;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

public class JarvisHudView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int red = Color.rgb(255, 55, 42);
    private final int gold = Color.rgb(255, 190, 75);
    private final int cyan = Color.rgb(70, 210, 235);
    private final int white = Color.rgb(235, 240, 250);

    private final long startTime = SystemClock.uptimeMillis();

    public JarvisHudView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private void line(Canvas c, float x1, float y1,
                      float x2, float y2, int color, float width) {
        paint.setColor(color);
        paint.setStrokeWidth(width);
        paint.setStyle(Paint.Style.STROKE);
        c.drawLine(x1, y1, x2, y2, paint);
    }

    private void circle(Canvas c, float x, float y,
                        float radius, int color, float width) {
        paint.setColor(color);
        paint.setStrokeWidth(width);
        paint.setStyle(Paint.Style.STROKE);
        c.drawCircle(x, y, radius, paint);
    }

    private void label(Canvas c, String value,
                       float x, float y, float size, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(android.graphics.Typeface.create(
                "sans-serif", android.graphics.Typeface.BOLD));
        c.drawText(value, x, y, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        float time =
                (SystemClock.uptimeMillis() - startTime) / 1000f;

        float rotation = time * 24f;

        canvas.drawColor(Color.rgb(5, 8, 15));

        // Background grid
        paint.setColor(Color.rgb(24, 31, 43));
        paint.setStrokeWidth(1);

        for (int i = 0; i < w; i += 28) {
            canvas.drawLine(i, 0, i, h, paint);
        }

        for (int i = 0; i < h; i += 28) {
            canvas.drawLine(0, i, w, i, paint);
        }

        // Outer frame
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(red);

        canvas.drawRoundRect(
                new RectF(10, 10, w - 10, h - 10),
                18, 18, paint);

        // Rotating reactor rings
        canvas.save();
        canvas.rotate(rotation, cx, cy);

        circle(canvas, cx, cy, 118, red, 3);
        circle(canvas, cx, cy, 108, gold, 1);
        circle(canvas, cx, cy, 94, cyan, 2);
        circle(canvas, cx, cy, 80, gold, 1);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(gold);

        RectF arc = new RectF(
                cx - 126, cy - 126,
                cx + 126, cy + 126);

        canvas.drawArc(arc, 15, 65, false, paint);
        canvas.drawArc(arc, 145, 75, false, paint);
        canvas.drawArc(arc, 270, 50, false, paint);

        for (int i = 0; i < 12; i++) {
            canvas.save();
            canvas.rotate(i * 30f, cx, cy);

            line(canvas, cx, cy - 135,
                    cx, cy - 123, gold, 3);

            canvas.restore();
        }

        canvas.restore();

        // Reactor glow
        float pulse = 1f + 0.06f *
                (float) Math.sin(time * 2.5f);

        glow.setColor(Color.argb(75, 255, 130, 35));
        glow.setStyle(Paint.Style.FILL);

        canvas.drawCircle(cx, cy, 63 * pulse, glow);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 170, 65));
        canvas.drawCircle(cx, cy, 35 * pulse, paint);

        paint.setColor(Color.rgb(255, 235, 190));
        canvas.drawCircle(cx, cy, 22 * pulse, paint);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, 9, paint);

        // HUD labels
        label(canvas, "J.A.R.V.I.S.", 24, 34, 13, gold);
        label(canvas, "CORE ONLINE", 24, 53, 9, cyan);

        label(canvas, "SYSTEM", w - 75, 34, 10, white);
        label(canvas, "ACTIVE", w - 75, 50, 10, gold);

        label(canvas, "ARC REACTOR",
                cx - 47, cy + 158, 11, gold);

        label(canvas, "ENERGY  •  STABLE",
                cx - 57, cy + 176, 9, cyan);

        // Moving scan line
        float scanY = (time * 55f) % Math.max(h, 1);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(35, 255, 80, 50));

        canvas.drawRect(
                12, scanY, w - 12, scanY + 3, paint);

        // Bottom status line
        line(canvas, 25, h - 35,
                w - 25, h - 35, gold, 1);

        label(canvas, "STARK SYSTEMS  //  PERSONAL AI",
                24, h - 19, 9, white);

        postInvalidateDelayed(33);
    }
}
