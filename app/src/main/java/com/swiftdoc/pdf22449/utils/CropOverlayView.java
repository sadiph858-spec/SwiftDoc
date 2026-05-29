package com.swiftdoc.pdf22449.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Transparent overlay view drawn on top of the PDF page image.
 * Draws a draggable crop rectangle with corner handles.
 */
public class CropOverlayView extends View {

    private final Paint borderPaint = new Paint();
    private final Paint dimPaint    = new Paint();
    private final Paint handlePaint = new Paint();

    private RectF cropRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f); // relative 0-1
    private static final int HANDLE_NONE=0,TOP_LEFT=1,TOP_RIGHT=2,BOT_LEFT=3,BOT_RIGHT=4,MOVE=5;
    private int activeHandle = HANDLE_NONE;
    private float touchStartX, touchStartY;
    private float startL, startT, startR, startB;
    private static final float HANDLE_RADIUS_DP = 16f;

    public CropOverlayView(Context ctx) { super(ctx); init(); }
    public CropOverlayView(Context ctx, AttributeSet a) { super(ctx,a); init(); }
    public CropOverlayView(Context ctx, AttributeSet a, int s) { super(ctx,a,s); init(); }

    private void init() {
        borderPaint.setColor(Color.WHITE); borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f); borderPaint.setAntiAlias(true);
        dimPaint.setColor(0x88000000); dimPaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(0xFF4CAF50); handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setAntiAlias(true);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void reset() { cropRect.set(0.08f,0.08f,0.92f,0.92f); invalidate(); }
    public void setCropRect(float l,float t,float r,float b) { cropRect.set(l,t,r,b); invalidate(); }
    public RectF getCropRect() { return new RectF(cropRect); }

    @Override protected void onDraw(Canvas canvas) {
        int w=getWidth(), h=getHeight();
        float l=cropRect.left*w, t=cropRect.top*h, r=cropRect.right*w, b=cropRect.bottom*h;
        // Dim outside crop
        canvas.drawRect(0,0,w,t,dimPaint); canvas.drawRect(0,b,w,h,dimPaint);
        canvas.drawRect(0,t,l,b,dimPaint); canvas.drawRect(r,t,w,b,dimPaint);
        // Border
        canvas.drawRect(l,t,r,b,borderPaint);
        // Grid lines
        Paint grid=new Paint(borderPaint); grid.setAlpha(80);
        float tw=(r-l)/3f,th=(b-t)/3f;
        canvas.drawLine(l+tw,t,l+tw,b,grid); canvas.drawLine(l+2*tw,t,l+2*tw,b,grid);
        canvas.drawLine(l,t+th,r,t+th,grid); canvas.drawLine(l,t+2*th,r,t+2*th,grid);
        // Handles
        float hr=HANDLE_RADIUS_DP*getResources().getDisplayMetrics().density;
        canvas.drawCircle(l,t,hr,handlePaint); canvas.drawCircle(r,t,hr,handlePaint);
        canvas.drawCircle(l,b,hr,handlePaint); canvas.drawCircle(r,b,hr,handlePaint);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int w=getWidth(), h=getHeight();
        float x=e.getX(), y=e.getY();
        float l=cropRect.left*w, t=cropRect.top*h, r=cropRect.right*w, b=cropRect.bottom*h;
        float hr=HANDLE_RADIUS_DP*getResources().getDisplayMetrics().density*1.5f;

        if (e.getAction()==MotionEvent.ACTION_DOWN) {
            if (dist(x,y,l,t)<hr) activeHandle=TOP_LEFT;
            else if (dist(x,y,r,t)<hr) activeHandle=TOP_RIGHT;
            else if (dist(x,y,l,b)<hr) activeHandle=BOT_LEFT;
            else if (dist(x,y,r,b)<hr) activeHandle=BOT_RIGHT;
            else if (x>l&&x<r&&y>t&&y<b) activeHandle=MOVE;
            else activeHandle=HANDLE_NONE;
            touchStartX=x; touchStartY=y;
            startL=cropRect.left; startT=cropRect.top; startR=cropRect.right; startB=cropRect.bottom;
        } else if (e.getAction()==MotionEvent.ACTION_MOVE && activeHandle!=HANDLE_NONE) {
            float dx=(x-touchStartX)/w, dy=(y-touchStartY)/h;
            float minSize=0.05f;
            switch(activeHandle){
                case TOP_LEFT: cropRect.left=clamp(startL+dx,0,cropRect.right-minSize); cropRect.top=clamp(startT+dy,0,cropRect.bottom-minSize); break;
                case TOP_RIGHT: cropRect.right=clamp(startR+dx,cropRect.left+minSize,1); cropRect.top=clamp(startT+dy,0,cropRect.bottom-minSize); break;
                case BOT_LEFT: cropRect.left=clamp(startL+dx,0,cropRect.right-minSize); cropRect.bottom=clamp(startB+dy,cropRect.top+minSize,1); break;
                case BOT_RIGHT: cropRect.right=clamp(startR+dx,cropRect.left+minSize,1); cropRect.bottom=clamp(startB+dy,cropRect.top+minSize,1); break;
                case MOVE: float nl=clamp(startL+dx,0,1-(startR-startL)), nt=clamp(startT+dy,0,1-(startB-startT)); cropRect.offsetTo(nl,nt); break;
            }
            invalidate();
        } else if (e.getAction()==MotionEvent.ACTION_UP) activeHandle=HANDLE_NONE;
        return true;
    }
    private float dist(float x1,float y1,float x2,float y2){return (float)Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));}
    private float clamp(float v,float mn,float mx){return Math.max(mn,Math.min(mx,v));}
}
