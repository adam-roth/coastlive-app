package au.com.suncoastpc.coastlive.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DrawableView extends View {
    protected Paint paint = new Paint();
    protected List<PaintedLine> lines = new ArrayList<>();
    protected Bitmap backgroundImage = null;

    private void setup() {
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5.0f);

        this.setDrawingCacheEnabled(true);
        this.setDrawingCacheBackgroundColor(Color.TRANSPARENT);
    }

    public DrawableView(Context context) {
        super(context);
        setup();
    }

    public DrawableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public DrawableView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    public void setDrawingColor(int color) {
        Paint newPaint = new Paint();
        newPaint.setColor(color);
        newPaint.setStrokeWidth(paint.getStrokeWidth());

        paint = newPaint;
        //paint.setColor(color);
    }

    public void setStrokeWidth(float width) {
        Paint newPaint = new Paint();
        newPaint.setColor(paint.getColor());
        newPaint.setStrokeWidth(width);

        paint = newPaint;
        //paint.setStrokeWidth(width);
    }

    public boolean isBlank() {
        return lines.isEmpty();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (backgroundImage != null) {
            canvas.drawBitmap(backgroundImage, 0, 0, paint);
        }
        for (PaintedLine line : lines) {
            float[] coords = line.getCoords();
            canvas.drawLine(coords[0], coords[1], coords[2], coords[3], line.getPaint());
        }
    }

    public void addLine(float x1, float y1, float x2, float y2) {
        lines.add(new PaintedLine(new float[]{x1, y1, x2, y2}, paint));
        this.invalidate();
    }

    public void clear() {
        lines.clear();
        backgroundImage = null;
        this.invalidate();
    }

    public byte[] toPng() {
        Bitmap bitmap = this.getDrawingCache();
        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngBytes);

        return pngBytes.toByteArray();
    }

    public void setBackgroundPng(File pngFile) {
        backgroundImage = BitmapFactory.decodeFile(pngFile.getAbsolutePath());
        this.invalidate();
    }

    private static class PaintedLine {
        private float[] coords;
        private Paint paint;

        public PaintedLine(float[] coords, Paint paint) {
            this.coords = coords;
            this.paint = paint;
        }

        public float[] getCoords() {
            return coords;
        }

        public Paint getPaint() {
            return paint;
        }
    }
}
