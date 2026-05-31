package com.swiftdoc.pdf22449.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.swiftdoc.pdf22449.R;
import com.swiftdoc.pdf22449.utils.ImageProcessorUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageView   ivPreview;
    private Button      btnCapture, btnAddMore, btnConvert, btnRetake;
    private ProgressBar pb;

    private ImageCapture imageCapture;
    private final List<Bitmap> capturedPages = new ArrayList<>();
    private static final int REQ_CAMERA = 200;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_scan);
        Toolbar tb = findViewById(R.id.toolbar_scan);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Scan to PDF");
        }
        previewView = findViewById(R.id.camera_preview);
        ivPreview   = findViewById(R.id.iv_scan_preview);
        btnCapture  = findViewById(R.id.btn_capture);
        btnAddMore  = findViewById(R.id.btn_add_more);
        btnConvert  = findViewById(R.id.btn_scan_convert);
        btnRetake   = findViewById(R.id.btn_retake);
        pb          = findViewById(R.id.pb_scan);

        btnCapture.setOnClickListener(v -> capturePhoto());
        btnAddMore.setOnClickListener(v -> showCamera());
        btnRetake.setOnClickListener(v  -> { if (!capturedPages.isEmpty()) capturedPages.remove(capturedPages.size()-1); showCamera(); });
        btnConvert.setOnClickListener(v -> buildPDF());

        if (hasCameraPermission()) startCamera();
        else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(req, perms, grants);
        if (req == REQ_CAMERA && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED)
            startCamera();
        else Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show();
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cp = ProcessCameraProvider.getInstance(this).get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setJpegQuality(95)
                    .build();
                cp.unbindAll();
                cp.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
                showCamera();
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void showCamera() {
        previewView.setVisibility(View.VISIBLE);
        ivPreview.setVisibility(View.GONE);
        btnCapture.setVisibility(View.VISIBLE);
        btnAddMore.setVisibility(View.GONE);
        btnRetake.setVisibility(View.GONE);
        btnConvert.setVisibility(View.GONE);
    }

    private void capturePhoto() {
        if (imageCapture == null) return;
        pb.setVisibility(View.VISIBLE);
        File photoFile = new File(getCacheDir(), "scan_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions opts = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(this),
            new ImageCapture.OnImageSavedCallback() {
                @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults r) {
                    pb.setVisibility(View.GONE);
                    AsyncTask.execute(() -> {
                        Bitmap raw      = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        Bitmap deskewed = ImageProcessorUtils.autoDeskew(raw);
                        Bitmap enhanced = ImageProcessorUtils.enhanceForScan(deskewed);
                        Bitmap cropped  = ImageProcessorUtils.cropWhitespace(enhanced, 20);
                        if (deskewed != raw)     raw.recycle();
                        if (enhanced != deskewed) deskewed.recycle();
                        capturedPages.add(cropped);
                        runOnUiThread(() -> showPreview(cropped));
                    });
                }
                @Override public void onError(@NonNull ImageCaptureException e) {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(ScanActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showPreview(Bitmap bmp) {
        previewView.setVisibility(View.GONE);
        ivPreview.setVisibility(View.VISIBLE);
        ivPreview.setImageBitmap(bmp);
        btnCapture.setVisibility(View.GONE);
        btnAddMore.setVisibility(View.VISIBLE);
        btnRetake.setVisibility(View.VISIBLE);
        btnConvert.setVisibility(View.VISIBLE);
        btnConvert.setText("Convert " + capturedPages.size() + " page(s) to PDF");
    }

    private void buildPDF() {
        if (capturedPages.isEmpty()) return;
        pb.setVisibility(View.VISIBLE);
        btnConvert.setEnabled(false);
        AsyncTask.execute(() -> {
            try {
                String fname = "SwiftDoc_Scan_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
                File out = new File(android.os.Environment
                    .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fname);
                Document doc = new Document(PageSize.A4, 10, 10, 10, 10);
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();
                for (Bitmap bmp : capturedPages) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                    Image img = Image.getInstance(baos.toByteArray());
                    img.scaleToFit(PageSize.A4.getWidth() - 20, PageSize.A4.getHeight() - 20);
                    img.setAlignment(Image.ALIGN_CENTER);
                    doc.add(img);
                }
                doc.close();
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    btnConvert.setEnabled(true);
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("✅ Scan Complete!")
                        .setMessage(capturedPages.size() + " pages saved to Downloads:\n" + fname)
                        .setPositiveButton("Open PDF", (d, w) ->
                            startActivity(new Intent(this, PDFViewerActivity.class)
                                .putExtra("pdf_path", out.getAbsolutePath())
                                .putExtra("pdf_name", fname)))
                        .setNegativeButton("OK", null).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    btnConvert.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override public boolean onOptionsItemSelected(MenuItem i) {
        if (i.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(i);
    }
}
