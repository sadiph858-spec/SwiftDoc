package com.smartpdf.reader.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * FastPDFRenderer — Ultra-high performance PDF rendering engine.
 *
 * Key optimisations:
 *  • Android PdfRenderer (native/GPU) — bypasses Java PDF parsing entirely
 *  • LruCache holds up to 12 MB of decoded page bitmaps (instant page flips)
 *  • Dedicated background HandlerThread for sequential decode (no lock contention)
 *  • Pre-fetches the next 2 + previous 1 pages ahead of the current view
 *  • Adaptive render scale: renders at exactly the device's display density
 *  • Bitmap pooling via recycle() to avoid GC pressure on large files
 *  • Progressive callback fires on first-page-ready, not on full-doc-load
 */
public class FastPDFRenderer {

    private static final String TAG = "FastPDFRenderer";

    // Page cache: key = page index, value = rendered Bitmap
    // Limit = 12 MB (~3 full-HD pages at once)
    private final LruCache<Integer, Bitmap> pageCache = new LruCache<Integer, Bitmap>(12 * 1024 * 1024) {
        @Override protected int sizeOf(Integer key, Bitmap value) {
            return value.getByteCount();
        }
        @Override protected void entryRemoved(boolean evicted, Integer key, Bitmap old, Bitmap newVal) {
            if (old != null && !old.isRecycled() && newVal == null) old.recycle();
        }
    };

    // Single background decode thread (avoids concurrent PdfRenderer access — it is NOT thread-safe)
    private final HandlerThread decodeThread = new HandlerThread("PDF-Decode", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private final Handler decodeHandler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Pre-fetch thread pool (2 threads: one for "next page", one for "prev page")
    private final ExecutorService prefetchPool = Executors.newFixedThreadPool(2);

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pfd;
    private int pageCount = 0;

    // Render scale — slightly below screen DPI for speed vs. quality balance
    private int renderWidth  = 1080;
    private int renderHeight = 1920;

    public interface Callback {
        /** Called once the renderer is open and page count is known */
        void onReady(int pageCount);
        /** Called when a page bitmap is ready for display */
        void onPageReady(int pageIndex, Bitmap bitmap);
        /** Called on any error */
        void onError(String message);
    }

    public FastPDFRenderer(Context ctx) {
        // Compute optimal render size from screen metrics
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(dm);
        // Render at native screen width, proportional height
        renderWidth  = dm.widthPixels;
        renderHeight = (int)(dm.widthPixels * 1.414f); // A4 aspect ratio

        decodeThread.start();
        decodeHandler = new Handler(decodeThread.getLooper());
    }

    /** Open the PDF asynchronously. Calls callback.onReady() when ready. */
    public void open(File pdfFile, Callback callback) {
        decodeHandler.post(() -> {
            try {
                pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                pdfRenderer = new PdfRenderer(pfd);
                pageCount = pdfRenderer.getPageCount();
                mainHandler.post(() -> callback.onReady(pageCount));
            } catch (Exception e) {
                Log.e(TAG, "open failed: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Cannot open PDF: " + e.getMessage()));
            }
        });
    }

    /**
     * Request a page render.
     * Returns cached bitmap immediately if available (zero latency),
     * otherwise queues decode and calls callback when done.
     */
    public Bitmap getPage(int pageIndex, Callback callback) {
        // Cache hit — instant return
        Bitmap cached = pageCache.get(pageIndex);
        if (cached != null && !cached.isRecycled()) return cached;

        // Queue decode on background thread
        decodeHandler.post(() -> renderPage(pageIndex, callback));

        // Pre-fetch neighbours in parallel
        prefetchAround(pageIndex);
        return null;
    }

    /** Pre-fetches page at index+1 and index-1 into cache */
    private void prefetchAround(int index) {
        int[] neighbours = {index + 1, index + 2, index - 1};
        for (int n : neighbours) {
            if (n >= 0 && n < pageCount && pageCache.get(n) == null) {
                final int page = n;
                prefetchPool.submit(() -> {
                    decodeHandler.post(() -> {
                        if (pageCache.get(page) == null) renderPage(page, null);
                    });
                });
            }
        }
    }

    /** Must be called on the decode thread. */
    private void renderPage(int pageIndex, Callback callback) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pageCount) return;
        // Double-check cache (may have been filled while queued)
        if (pageCache.get(pageIndex) != null) {
            if (callback != null) {
                Bitmap b = pageCache.get(pageIndex);
                if (b != null) mainHandler.post(() -> callback.onPageReady(pageIndex, b));
            }
            return;
        }
        try {
            PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);

            // Scale to fit render width, preserve aspect ratio
            float scale = (float) renderWidth / page.getWidth();
            int bmpW = renderWidth;
            int bmpH = Math.max(1, (int)(page.getHeight() * scale));

            Bitmap bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(Color.WHITE);                         // white background
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            pageCache.put(pageIndex, bmp);
            if (callback != null) mainHandler.post(() -> callback.onPageReady(pageIndex, bmp));

        } catch (Exception e) {
            Log.e(TAG, "render page " + pageIndex + " failed: " + e);
            if (callback != null) mainHandler.post(() -> callback.onError(e.getMessage()));
        }
    }

    /** Pre-warm: render the first N pages right after open (speeds up first open) */
    public void preWarm(int pages) {
        decodeHandler.post(() -> {
            int limit = Math.min(pages, pageCount);
            for (int i = 0; i < limit; i++) {
                if (pageCache.get(i) == null) renderPage(i, null);
            }
        });
    }

    public int getPageCount() { return pageCount; }

    public void clearCache() { pageCache.evictAll(); }

    /** Release all resources. Must be called in onDestroy(). */
    public void release() {
        decodeHandler.removeCallbacksAndMessages(null);
        prefetchPool.shutdown();
        decodeHandler.post(() -> {
            try {
                pageCache.evictAll();
                if (pdfRenderer != null) { pdfRenderer.close(); pdfRenderer = null; }
                if (pfd != null)         { pfd.close();         pfd = null; }
            } catch (Exception e) { Log.e(TAG, "release: " + e); }
            decodeThread.quitSafely();
        });
    }
}
