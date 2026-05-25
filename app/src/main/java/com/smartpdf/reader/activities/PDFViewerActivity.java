package com.smartpdf.reader.activities;
import android.content.Intent; import android.graphics.Color; import android.net.Uri; import android.os.Bundle;
import android.view.Menu; import android.view.MenuItem; import android.view.View; import android.view.WindowManager;
import android.widget.FrameLayout; import android.widget.ProgressBar; import android.widget.SeekBar;
import android.widget.TextView; import android.widget.Toast;
import androidx.appcompat.app.AlertDialog; import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; import androidx.core.content.FileProvider;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnRenderListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.smartpdf.reader.R;
import com.smartpdf.reader.database.DBHelper;
import com.smartpdf.reader.models.Bookmark;
import com.smartpdf.reader.models.PDFFile;
import com.smartpdf.reader.utils.AdManager;
import java.io.File; import java.text.SimpleDateFormat; import java.util.Date; import java.util.List;
import java.util.Locale; import java.util.Timer; import java.util.TimerTask;

public class PDFViewerActivity extends AppCompatActivity {
    public static final int MODE_DAY=0, MODE_NIGHT=1, MODE_SEPIA=2;

    private PDFView pdfView;
    private ProgressBar pb;
    private TextView tvPage, tvError;
    private View layoutLoading;
    private FloatingActionButton fabFav, fabBookmark;
    private SeekBar sbBrightness;
    private FrameLayout adContainer;

    private String path, name;
    private int total=0, currentPage=0, readMode=MODE_DAY;
    private boolean fav=false, isBookmarked=false, fullScreen=false, autoScroll=false;
    private boolean swipeHorizontal=false;
    private DBHelper db;
    private long sessionStart=0;
    private Timer autoScrollTimer;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_pdf_viewer);
        db = DBHelper.getInstance(this);
        path = getIntent().getStringExtra("pdf_path");
        name = getIntent().getStringExtra("pdf_name");
        if (path == null) { Toast.makeText(this, "No file", Toast.LENGTH_SHORT).show(); finish(); return; }
        setupToolbar();
        setupViews();
        fav = db.isFavorite(path);
        updateFavIcon();
        sessionStart = System.currentTimeMillis();
        saveRecent();
        int lastPage = db.getLastPage(path);
        loadPDF(lastPage);
        AdManager.loadBanner(this, adContainer);
    }

    private void setupToolbar() {
        Toolbar tb = findViewById(R.id.toolbar_viewer);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(name != null ? name : "LeafPDF");
        }
    }

    private void setupViews() {
        pdfView       = findViewById(R.id.pdf_view);
        pb            = findViewById(R.id.progress_bar_viewer);
        tvPage        = findViewById(R.id.tv_page_indicator);
        tvError       = findViewById(R.id.tv_error);
        layoutLoading = findViewById(R.id.layout_loading);
        fabFav        = findViewById(R.id.fab_favorite);
        fabBookmark   = findViewById(R.id.fab_bookmark);
        adContainer   = findViewById(R.id.ad_container);
        sbBrightness  = findViewById(R.id.sb_brightness);
        fabFav.setOnClickListener(v -> toggleFav());
        fabBookmark.setOnClickListener(v -> toggleBookmark());
        sbBrightness.setProgress(80);
        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int prog, boolean user) {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = Math.max(0.01f, prog / 100f);
                getWindow().setAttributes(lp);
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void loadPDF(int startPage) {
        File f = new File(path);
        if (!f.exists()) { showErr("File not found"); return; }
        layoutLoading.setVisibility(View.VISIBLE);
        pdfView.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
        try {
            pdfView.fromFile(f)
                .defaultPage(startPage)
                // ── Performance settings ──────────────────────────────
                .enableSwipe(true)
                .swipeHorizontal(swipeHorizontal)
                .enableDoubletap(true)
                .enableAntialiasing(false)        // OFF for speed; flip ON for quality
                .spacing(2)
                .pageFitPolicy(FitPolicy.WIDTH)
                .enableAnnotationRendering(false) // OFF = faster first render
                .pageSnap(false)                  // smooth scroll, no snap delay
                .pageFling(true)                  // momentum fling
                .autoSpacing(false)
                .nightMode(readMode == MODE_NIGHT)
                // ── Callbacks ─────────────────────────────────────────
                .onRender((OnRenderListener) nbPages -> {
                    // First page visible — hide spinner immediately
                    runOnUiThread(() -> {
                        layoutLoading.setVisibility(View.GONE);
                        pdfView.setVisibility(View.VISIBLE);
                    });
                })
                .onLoad((OnLoadCompleteListener) nbPages -> runOnUiThread(() -> {
                    total = nbPages;
                    tvPage.setText((startPage + 1) + " / " + nbPages);
                    updateBookmarkIcon();
                }))
                .onPageChange((OnPageChangeListener) (page, count) -> runOnUiThread(() -> {
                    currentPage = page;
                    tvPage.setText((page + 1) + " / " + count);
                    updateBookmarkIcon();
                }))
                .onError((OnErrorListener) e -> runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    showErr("Failed to load PDF");
                }))
                .load();
        } catch (Exception e) {
            layoutLoading.setVisibility(View.GONE);
            showErr("Failed to load PDF");
        }
    }

    private void showErr(String m) {
        tvError.setText(m);
        tvError.setVisibility(View.VISIBLE);
        pdfView.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
    }

    private void saveRecent() {
        File f = new File(path);
        String d = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(f.lastModified()));
        db.addToRecent(new PDFFile(name != null ? name : f.getName(), path, d, f.length()));
    }

    private void toggleFav() {
        File f = new File(path);
        String d = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(f.lastModified()));
        PDFFile p = new PDFFile(name != null ? name : f.getName(), path, d, f.length());
        if (fav) {
            db.removeFromFavorites(path); fav = false;
            Snackbar.make(pdfView, "Removed from favorites", Snackbar.LENGTH_SHORT).show();
        } else {
            db.addToFavorites(p); fav = true;
            Snackbar.make(pdfView, "Added to favorites \u2605", Snackbar.LENGTH_SHORT).show();
        }
        updateFavIcon();
    }
    private void updateFavIcon() {
        fabFav.setImageResource(fav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
    }

    private void toggleBookmark() {
        if (db.isBookmarked(path, currentPage)) {
            db.removeBookmarkByPage(path, currentPage); isBookmarked = false;
            Snackbar.make(pdfView, "Bookmark removed", Snackbar.LENGTH_SHORT).show();
        } else {
            db.addBookmark(path, currentPage, "Page " + (currentPage + 1)); isBookmarked = true;
            Snackbar.make(pdfView, "Bookmarked page " + (currentPage + 1), Snackbar.LENGTH_SHORT).show();
        }
        updateBookmarkIcon();
    }
    private void updateBookmarkIcon() {
        isBookmarked = db.isBookmarked(path, currentPage);
        fabBookmark.setImageResource(isBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
    }

    private void showJumpToPage() {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setHint("1 - " + total);
        new AlertDialog.Builder(this).setTitle("Jump to Page").setView(et)
            .setPositiveButton("Go", (d, w) -> {
                try {
                    int pg = Integer.parseInt(et.getText().toString()) - 1;
                    if (pg >= 0 && pg < total) pdfView.jumpTo(pg, true);
                    else Toast.makeText(this, "Invalid page", Toast.LENGTH_SHORT).show();
                } catch (Exception ex) { Toast.makeText(this, "Enter a number", Toast.LENGTH_SHORT).show(); }
            }).setNegativeButton("Cancel", null).show();
    }

    private void showBookmarkList() {
        List<Bookmark> bms = db.getBookmarks(path);
        if (bms.isEmpty()) { Toast.makeText(this, "No bookmarks yet", Toast.LENGTH_SHORT).show(); return; }
        String[] titles = new String[bms.size()];
        for (int i = 0; i < bms.size(); i++) titles[i] = "Page " + (bms.get(i).getPage() + 1) + " — " + bms.get(i).getTitle();
        new AlertDialog.Builder(this).setTitle("Bookmarks")
            .setItems(titles, (d, w) -> pdfView.jumpTo(bms.get(w).getPage(), true))
            .setNegativeButton("Close", null).show();
    }

    private void changeReadMode() {
        String[] modes = {"Day Mode \u2600", "Night Mode \uD83C\uDF19", "Sepia Mode \uD83D\uDCDC"};
        new AlertDialog.Builder(this).setTitle("Reading Mode")
            .setItems(modes, (d, w) -> { readMode = w; applyReadMode(); }).show();
    }
    private void applyReadMode() {
        switch (readMode) {
            case MODE_NIGHT: pdfView.setBackgroundColor(Color.BLACK); break;
            case MODE_SEPIA: pdfView.setBackgroundColor(Color.parseColor("#F5DEB3")); break;
            default:         pdfView.setBackgroundColor(Color.WHITE); break;
        }
        int lastPage = currentPage;
        pdfView.recycle();
        loadPDF(lastPage);
    }

    private void toggleFullScreen() {
        fullScreen = !fullScreen;
        View dv = getWindow().getDecorView();
        if (fullScreen) {
            dv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            if (getSupportActionBar() != null) getSupportActionBar().hide();
        } else {
            dv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            if (getSupportActionBar() != null) getSupportActionBar().show();
        }
    }

    private void toggleScrollDirection() {
        swipeHorizontal = !swipeHorizontal;
        int p = currentPage; pdfView.recycle(); loadPDF(p);
        Toast.makeText(this, swipeHorizontal ? "Horizontal scroll" : "Vertical scroll", Toast.LENGTH_SHORT).show();
    }

    private void toggleAutoScroll() {
        autoScroll = !autoScroll;
        if (autoScroll) {
            autoScrollTimer = new Timer();
            autoScrollTimer.scheduleAtFixedRate(new TimerTask() {
                @Override public void run() {
                    runOnUiThread(() -> {
                        if (currentPage < total - 1) pdfView.jumpTo(currentPage + 1, true);
                        else { autoScroll = false; autoScrollTimer.cancel(); }
                    });
                }
            }, 3000, 3000);
        } else { if (autoScrollTimer != null) autoScrollTimer.cancel(); }
        Toast.makeText(this, autoScroll ? "Auto-scroll ON" : "Auto-scroll OFF", Toast.LENGTH_SHORT).show();
    }

    private void sharePDF() {
        try {
            File file = new File(path);
            if (!file.exists()) { Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show(); return; }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent si = new Intent(Intent.ACTION_SEND);
            si.setType("application/pdf"); si.putExtra(Intent.EXTRA_STREAM, uri);
            si.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(si, "Share PDF"));
        } catch (Exception e) { Toast.makeText(this, "Cannot share", Toast.LENGTH_SHORT).show(); }
    }

    private void printPDF() {
        try {
            android.print.PrintManager pm = (android.print.PrintManager) getSystemService(PRINT_SERVICE);
            android.webkit.WebView wv = new android.webkit.WebView(this);
            pm.print(name != null ? name : "Document",
                wv.createPrintDocumentAdapter(name != null ? name : "Document"),
                new android.print.PrintAttributes.Builder().build());
        } catch (Exception e) { Toast.makeText(this, "Print not available", Toast.LENGTH_SHORT).show(); }
    }

    @Override public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.viewer_menu, m); return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home)        { onBackPressed(); return true; }
        if (id == R.id.action_share)        { sharePDF(); return true; }
        if (id == R.id.action_jump)         { showJumpToPage(); return true; }
        if (id == R.id.action_bookmarks)    { showBookmarkList(); return true; }
        if (id == R.id.action_read_mode)    { changeReadMode(); return true; }
        if (id == R.id.action_fullscreen)   { toggleFullScreen(); return true; }
        if (id == R.id.action_scroll_dir)   { toggleScrollDirection(); return true; }
        if (id == R.id.action_auto_scroll)  { toggleAutoScroll(); return true; }
        if (id == R.id.action_first)        { pdfView.jumpTo(0, true); return true; }
        if (id == R.id.action_last && total > 0) { pdfView.jumpTo(total - 1, true); return true; }
        if (id == R.id.action_print)        { printPDF(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override protected void onPause() {
        super.onPause();
        if (path != null && total > 0) {
            long elapsed = System.currentTimeMillis() - sessionStart;
            db.updateProgress(path, currentPage, total, elapsed);
        }
        stopAutoScroll();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        stopAutoScroll();
        if (pdfView != null) pdfView.recycle();
    }
    private void stopAutoScroll() {
        if (autoScrollTimer != null) { autoScrollTimer.cancel(); autoScrollTimer = null; }
    }
}
