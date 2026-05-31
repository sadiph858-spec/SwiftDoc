package com.swiftdoc.pdf22449.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.swiftdoc.pdf22449.R;
import com.swiftdoc.pdf22449.utils.CropOverlayView;
import com.swiftdoc.pdf22449.utils.ImageProcessorUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CropActivity extends AppCompatActivity {

    private android.widget.ImageView ivPage;
    private CropOverlayView cropOverlay;
    private Button btnAutoCrop, btnSave;
    private ProgressBar pb;
    private TextView tvInfo;
    private SeekBar sbPage;

    private String pdfPath, pdfName;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pfd;
    private int currentPage = 0, pageCount = 0;
    private Bitmap currentBitmap;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_crop);
        pdfPath = getIntent().getStringExtra("pdf_path");
        pdfName = getIntent().getStringExtra("pdf_name");
        Toolbar tb = findViewById(R.id.toolbar_crop);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Crop PDF");
        }
        ivPage      = findViewById(R.id.iv_crop_page);
        cropOverlay = findViewById(R.id.crop_overlay);
        btnAutoCrop = findViewById(R.id.btn_auto_crop);
        btnSave     = findViewById(R.id.btn_save_cropped);
        pb          = findViewById(R.id.pb_crop);
        tvInfo      = findViewById(R.id.tv_crop_info);
        sbPage      = findViewById(R.id.sb_page_select);
        btnAutoCrop.setOnClickListener(v -> autoCrop());
        btnSave.setOnClickListener(v -> saveCroppedPDF());
        openPDF();
    }

    private void openPDF() {
        if (pdfPath == null) { Toast.makeText(this,"No PDF",Toast.LENGTH_SHORT).show(); return; }
        AsyncTask.execute(() -> {
            try {
                pfd = ParcelFileDescriptor.open(new File(pdfPath), ParcelFileDescriptor.MODE_READ_ONLY);
                pdfRenderer = new PdfRenderer(pfd);
                pageCount = pdfRenderer.getPageCount();
                runOnUiThread(() -> {
                    tvInfo.setText("Page 1 / " + pageCount + " — drag handles to crop");
                    sbPage.setMax(Math.max(0, pageCount - 1));
                    sbPage.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        public void onProgressChanged(SeekBar sb, int p, boolean u) { currentPage = p; renderPage(p); }
                        public void onStartTrackingTouch(SeekBar sb) {}
                        public void onStopTrackingTouch(SeekBar sb) {}
                    });
                    renderPage(0);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Cannot open: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void renderPage(int idx) {
        if (pdfRenderer == null || idx >= pageCount) return;
        pb.setVisibility(View.VISIBLE);
        AsyncTask.execute(() -> {
            try {
                PdfRenderer.Page page = pdfRenderer.openPage(idx);
                int sw = getResources().getDisplayMetrics().widthPixels;
                float scale = (float) sw / page.getWidth();
                int bw = sw, bh = (int)(page.getHeight() * scale);
                Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
                android.graphics.Canvas c = new android.graphics.Canvas(bmp);
                c.drawColor(Color.WHITE);
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                if (currentBitmap != null && !currentBitmap.isRecycled()) currentBitmap.recycle();
                currentBitmap = bmp;
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    ivPage.setImageBitmap(bmp);
                    cropOverlay.reset();
                    tvInfo.setText("Page " + (idx + 1) + " / " + pageCount);
                });
            } catch (Exception e) {
                runOnUiThread(() -> pb.setVisibility(View.GONE));
            }
        });
    }

    private void autoCrop() {
        if (currentBitmap == null) return;
        pb.setVisibility(View.VISIBLE);
        AsyncTask.execute(() -> {
            int w = currentBitmap.getWidth(), h = currentBitmap.getHeight();
            int top = 0, bottom = h-1, left = 0, right = w-1;
            final int T = 240;
            outer1: for (top=0; top<h; top++)
                for (int x=0; x<w; x++) { int c=currentBitmap.getPixel(x,top); if(Color.red(c)<T||Color.green(c)<T||Color.blue(c)<T) break outer1; }
            outer2: for (bottom=h-1; bottom>top; bottom--)
                for (int x=0; x<w; x++) { int c=currentBitmap.getPixel(x,bottom); if(Color.red(c)<T||Color.green(c)<T||Color.blue(c)<T) break outer2; }
            outer3: for (left=0; left<w; left++)
                for (int y=top; y<bottom; y++) { int c=currentBitmap.getPixel(left,y); if(Color.red(c)<T||Color.green(c)<T||Color.blue(c)<T) break outer3; }
            outer4: for (right=w-1; right>left; right--)
                for (int y=top; y<bottom; y++) { int c=currentBitmap.getPixel(right,y); if(Color.red(c)<T||Color.green(c)<T||Color.blue(c)<T) break outer4; }
            int pad = 20;
            top    = Math.max(0, top-pad);
            bottom = Math.min(h-1, bottom+pad);
            left   = Math.max(0, left-pad);
            right  = Math.min(w-1, right+pad);
            final float fl=(float)left/w, ft=(float)top/h, fr=(float)right/w, fb=(float)bottom/h;
            runOnUiThread(() -> { pb.setVisibility(View.GONE); cropOverlay.setCropRect(fl,ft,fr,fb); });
        });
    }

    private void saveCroppedPDF() {
        RectF cr = cropOverlay.getCropRect();
        if (cr == null) { Toast.makeText(this,"Set a crop area first",Toast.LENGTH_SHORT).show(); return; }
        pb.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);
        AsyncTask.execute(() -> {
            try {
                String fname = "SwiftDoc_Cropped_" +
                    new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
                File out = new File(android.os.Environment
                    .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fname);
                Document doc = new Document(PageSize.A4, 5, 5, 5, 5);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = pdfRenderer.openPage(i);
                    int sw = getResources().getDisplayMetrics().widthPixels;
                    float scale = (float) sw / page.getWidth();
                    Bitmap bmp = Bitmap.createBitmap(sw, (int)(page.getHeight()*scale), Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas cv = new android.graphics.Canvas(bmp);
                    cv.drawColor(Color.WHITE);
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
                    Bitmap cropped = ImageProcessorUtils.applyRelativeCrop(bmp, cr);
                    if (cropped != bmp) bmp.recycle();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    cropped.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                    cropped.recycle();
                    Image img = Image.getInstance(baos.toByteArray());
                    img.scaleToFit(PageSize.A4.getWidth()-10, PageSize.A4.getHeight()-10);
                    img.setAlignment(Image.ALIGN_CENTER);
                    doc.add(img);
                }
                doc.close();
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    new AlertDialog.Builder(this)
                        .setTitle("✅ Cropped!")
                        .setMessage("Saved to Downloads:\n" + fname)
                        .setPositiveButton("Open PDF", (d, w) ->
                            startActivity(new Intent(this, PDFViewerActivity.class)
                                .putExtra("pdf_path", out.getAbsolutePath())
                                .putExtra("pdf_name", fname)))
                        .setNegativeButton("OK", null).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { if (pdfRenderer != null) pdfRenderer.close(); if (pfd != null) pfd.close(); } catch (Exception e) { }
    }

    @Override public boolean onOptionsItemSelected(MenuItem i) {
        if (i.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(i);
    }
}
