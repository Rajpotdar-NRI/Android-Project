package com.campus.survival;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class TapPlusView extends View {

    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TapPlusView(Context context) {
        super(context);
        float d = context.getResources().getDisplayMetrics().density;

        outline.setColor(Color.WHITE);
        outline.setStrokeWidth(6 * d);
        outline.setStrokeCap(Paint.Cap.ROUND);

        fill.setColor(context.getResources().getColor(R.color.orange_deep, null));
        fill.setStrokeWidth(3 * d);
        fill.setStrokeCap(Paint.Cap.ROUND);

        dot.setColor(fill.getColor());

        setClickable(false);
        setFocusable(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float gap = getWidth() * 0.12f;   // empty centre
        float arm = getWidth() * 0.40f;   // arm end distance from centre

        drawPlus(canvas, cx, cy, gap, arm, outline);
        drawPlus(canvas, cx, cy, gap, arm, fill);
        canvas.drawCircle(cx, cy, fill.getStrokeWidth() * 0.6f, dot);
    }

    private void drawPlus(Canvas c, float cx, float cy, float gap, float arm, Paint p) {
        c.drawLine(cx, cy - arm, cx, cy - gap, p); // up
        c.drawLine(cx, cy + gap, cx, cy + arm, p); // down
        c.drawLine(cx - arm, cy, cx - gap, cy, p); // left
        c.drawLine(cx + gap, cy, cx + arm, cy, p); // right
    }
}
