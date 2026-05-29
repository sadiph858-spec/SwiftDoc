package com.swiftdoc.pdf22449.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import com.swiftdoc.pdf22449.R;
import com.swiftdoc.pdf22449.adapters.DashboardAdapter;
import com.swiftdoc.pdf22449.adapters.PDFAdapter;
import com.swiftdoc.pdf22449.database.DBHelper;
import com.swiftdoc.pdf22449.models.DashboardItem;
import com.swiftdoc.pdf22449.models.PDFFile;
import com.swiftdoc.pdf22449.utils.AdManager;
import com.swiftdoc.pdf22449.utils.FileUtils;
import com.swiftdoc.pdf22449.utils.PermissionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PDFAdapter.Listener {

    // Action codes
    public static final int ACTION_VIEW_PDF=1,ACTION_IMG2PDF=2,ACTION_PDF2IMG=3,
        ACTION_COMPRESS=4,ACTION_MERGE=5,ACTION_SPLIT=6,ACTION_LOCK=7,ACTION_UNLOCK=8,
        ACTION_OCR=9,ACTION_TXT2PDF=10,ACTION_TTS=11,ACTION_SIGN=12,ACTION_WATERMARK=13,
        ACTION_REORDER=14,ACTION_DELETE_PAGES=15,ACTION_ROTATE=16,ACTION_BOOKMARKS=17,
        ACTION_RECENT=18,ACTION_SEARCH=19,ACTION_METADATA=20,ACTION_DARK=21,ACTION_PRINT=22,
        ACTION_STATS=23,ACTION_SCAN=24,ACTION_CROP=25;

    private static final String PREFS="swift_prefs", KEY_DARK="dark_mode";

    private RecyclerView rvDash, rvFiles;
    private PDFAdapter adapter;
    private DashboardAdapter dashAdapter;
    private ProgressBar pb;
    private LinearLayout emptyLayout;
    private TextView tvEmpty;
    private EditText etSearch;
    private FrameLayout adContainer;

    private List<PDFFile> allPDFs = new ArrayList<>();
    private DBHelper db;
    private SharedPreferences prefs;
    private String sortMode = "date";
    private boolean gridView = false;
    private enum Tab { ALL, RECENT, FAVORITES }
    private Tab currentTab = Tab.ALL;

    private final ActivityResultLauncher<Intent> picker = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        r -> { if (r.getResultCode()==RESULT_OK && r.getData()!=null) handleUri(r.getData().getData()); });

    private final ActivityResultLauncher<String[]> permLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(),
        m -> { boolean ok=false; for(Boolean v:m.values()) if(v){ok=true;break;} if(ok) loadPDFs(); else showDenied(); });

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        applyDark();
        setContentView(R.layout.activity_main);
        db = DBHelper.getInstance(this);
        setupToolbar(); setupViews(); setupBottomNav(); setupDashboard();
        AdManager.loadBanner(this, adContainer);
        AdManager.loadInterstitial(this);
        checkPerm();
    }

    @Override protected void onResume() {
        super.onResume();
        filterAndDisplay(etSearch.getText().toString().trim());
    }

    private void setupToolbar() {
        Toolbar tb = findViewById(R.id.toolbar); setSupportActionBar(tb);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("SwiftDoc");
    }

    private void setupViews() {
        rvDash      = findViewById(R.id.rv_dashboard);
        rvFiles     = findViewById(R.id.rv_files);
        pb          = findViewById(R.id.progress_bar);
        emptyLayout = findViewById(R.id.layout_empty);
        tvEmpty     = findViewById(R.id.tv_empty);
        etSearch    = findViewById(R.id.et_search);
        adContainer = findViewById(R.id.ad_container);

        adapter = new PDFAdapter(this, this, gridView);
        rvFiles.setAdapter(adapter);
        rvFiles.setLayoutManager(new LinearLayoutManager(this));

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){ filterAndDisplay(s.toString().trim()); }
            public void afterTextChanged(Editable s){}
        });

        ((Chip)findViewById(R.id.chip_all)).setOnClickListener(v      -> { currentTab=Tab.ALL;       filterAndDisplay(etSearch.getText().toString()); });
        ((Chip)findViewById(R.id.chip_recent)).setOnClickListener(v   -> { currentTab=Tab.RECENT;    filterAndDisplay(etSearch.getText().toString()); });
        ((Chip)findViewById(R.id.chip_favs)).setOnClickListener(v     -> { currentTab=Tab.FAVORITES; filterAndDisplay(etSearch.getText().toString()); });
        ((FloatingActionButton)findViewById(R.id.fab_open)).setOnClickListener(v -> openPicker());
    }

    private void setupBottomNav() {
        ((BottomNavigationView)findViewById(R.id.bottom_nav)).setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id==R.id.nav_home)    { showHomeTab();   return true; }
            if (id==R.id.nav_tools)   { showToolsTab();  return true; }
            if (id==R.id.nav_history) { startActivity(new Intent(this,StatsActivity.class)); return true; }
            return false;
        });
    }

    private void setupDashboard() {
        List<DashboardItem> items = new ArrayList<>();
        items.add(new DashboardItem(R.drawable.ic_view_pdf,  "View PDF",      "Open & read",          R.color.green_medium,   ACTION_VIEW_PDF));
        items.add(new DashboardItem(R.drawable.ic_scan,      "Scan to PDF",   "Camera scanner",       R.color.primary,        ACTION_SCAN));
        items.add(new DashboardItem(R.drawable.ic_img2pdf,   "Image→PDF",     "Convert images",       R.color.green_dark,     ACTION_IMG2PDF));
        items.add(new DashboardItem(R.drawable.ic_pdf2img,   "PDF→Image",     "Export pages",         R.color.teal_medium,    ACTION_PDF2IMG));
        items.add(new DashboardItem(R.drawable.ic_crop,      "Crop PDF",      "Trim pages",           R.color.green_medium,   ACTION_CROP));
        items.add(new DashboardItem(R.drawable.ic_compress,  "Compress",      "Reduce size",          R.color.green_dark,     ACTION_COMPRESS));
        items.add(new DashboardItem(R.drawable.ic_merge,     "Merge PDF",     "Combine files",        R.color.teal_medium,    ACTION_MERGE));
        items.add(new DashboardItem(R.drawable.ic_split,     "Split PDF",     "Divide pages",         R.color.green_medium,   ACTION_SPLIT));
        items.add(new DashboardItem(R.drawable.ic_lock,      "Lock PDF",      "Add password",         R.color.green_dark,     ACTION_LOCK));
        items.add(new DashboardItem(R.drawable.ic_unlock,    "Unlock PDF",    "Remove password",      R.color.teal_medium,    ACTION_UNLOCK));
        items.add(new DashboardItem(R.drawable.ic_watermark, "Watermark",     "Add watermark",        R.color.green_medium,   ACTION_WATERMARK));
        items.add(new DashboardItem(R.drawable.ic_rotate,    "Rotate",        "Rotate pages",         R.color.green_dark,     ACTION_ROTATE));
        items.add(new DashboardItem(R.drawable.ic_txt2pdf,   "Text→PDF",      "Create from text",     R.color.teal_medium,    ACTION_TXT2PDF));
        items.add(new DashboardItem(R.drawable.ic_ocr,       "OCR",           "Extract text",         R.color.green_medium,   ACTION_OCR));
        items.add(new DashboardItem(R.drawable.ic_sign,      "Signature",     "Digital sign",         R.color.green_dark,     ACTION_SIGN));
        items.add(new DashboardItem(R.drawable.ic_tts,       "Read Aloud",    "Text to speech",       R.color.teal_medium,    ACTION_TTS));
        items.add(new DashboardItem(R.drawable.ic_reorder,   "Reorder",       "Sort pages",           R.color.green_medium,   ACTION_REORDER));
        items.add(new DashboardItem(R.drawable.ic_delete_page,"Del Pages",    "Remove pages",         R.color.green_dark,     ACTION_DELETE_PAGES));
        items.add(new DashboardItem(R.drawable.ic_bookmark,  "Bookmarks",     "Saved pages",          R.color.teal_medium,    ACTION_BOOKMARKS));
        items.add(new DashboardItem(R.drawable.ic_search_pdf,"Search Text",   "Find in PDF",          R.color.green_medium,   ACTION_SEARCH));
        items.add(new DashboardItem(R.drawable.ic_metadata,  "Metadata",      "Edit file info",       R.color.green_dark,     ACTION_METADATA));
        items.add(new DashboardItem(R.drawable.ic_dark_mode, "Dark Mode",     "Toggle theme",         R.color.teal_medium,    ACTION_DARK));
        items.add(new DashboardItem(R.drawable.ic_print,     "Print",         "Print document",       R.color.green_medium,   ACTION_PRINT));
        items.add(new DashboardItem(R.drawable.ic_stats,     "Statistics",    "Reading stats",        R.color.green_dark,     ACTION_STATS));

        dashAdapter = new DashboardAdapter(this, items, this::onDashAction);
        rvDash.setLayoutManager(new GridLayoutManager(this, 3));
        rvDash.setAdapter(dashAdapter);
    }

    private void onDashAction(int action) {
        switch (action) {
            case ACTION_VIEW_PDF:    openPicker(); break;
            case ACTION_SCAN:        startActivity(new Intent(this, ScanActivity.class)); break;
            case ACTION_IMG2PDF:     startActivity(new Intent(this, ImageToPdfActivity.class)); break;
            case ACTION_CROP:        pickForTool("crop"); break;
            case ACTION_PDF2IMG:     pickForTool("pdf2img"); break;
            case ACTION_COMPRESS:    pickForTool("compress"); break;
            case ACTION_MERGE:       startActivity(new Intent(this,PdfToolsActivity.class).putExtra("tool","merge")); break;
            case ACTION_SPLIT:       pickForTool("split"); break;
            case ACTION_LOCK:        pickForTool("lock"); break;
            case ACTION_UNLOCK:      pickForTool("unlock"); break;
            case ACTION_WATERMARK:   pickForTool("watermark"); break;
            case ACTION_ROTATE:      pickForTool("rotate"); break;
            case ACTION_TXT2PDF:     showTextToPdf(); break;
            case ACTION_OCR:         pickForTool("ocr"); break;
            case ACTION_SIGN:        pickForTool("sign"); break;
            case ACTION_TTS:         pickForTool("tts"); break;
            case ACTION_REORDER:     pickForTool("reorder"); break;
            case ACTION_DELETE_PAGES:pickForTool("delete_pages"); break;
            case ACTION_BOOKMARKS:   currentTab=Tab.RECENT; filterAndDisplay(""); showHomeTab(); break;
            case ACTION_SEARCH:      etSearch.requestFocus(); showHomeTab(); break;
            case ACTION_METADATA:    pickForTool("metadata"); break;
            case ACTION_DARK:        toggleDark(); break;
            case ACTION_PRINT:       pickForTool("print"); break;
            case ACTION_STATS:       startActivity(new Intent(this, StatsActivity.class)); break;
        }
    }

    private void pickForTool(String tool) {
        prefs.edit().putString("pending_tool", tool).apply();
        Toast.makeText(this, "Select a PDF for: " + tool, Toast.LENGTH_SHORT).show();
        openPicker();
    }

    private void showTextToPdf() {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Enter text to convert to PDF…"); et.setMinLines(5); et.setMaxLines(12);
        new AlertDialog.Builder(this).setTitle("Text to PDF").setView(et)
            .setPositiveButton("Convert",(d,w)->{ String txt=et.getText().toString().trim(); if(txt.isEmpty()){Toast.makeText(this,"Enter text",Toast.LENGTH_SHORT).show();return;} startActivity(new Intent(this,PdfToolsActivity.class).putExtra("tool","txt2pdf").putExtra("text",txt));})
            .setNegativeButton("Cancel",null).show();
    }

    private void showHomeTab()  { rvDash.setVisibility(View.GONE);    rvFiles.setVisibility(View.VISIBLE); setEmpty(adapter.getItemCount()==0); }
    private void showToolsTab() { rvDash.setVisibility(View.VISIBLE); rvFiles.setVisibility(View.GONE);   emptyLayout.setVisibility(View.GONE); }

    private void checkPerm() {
        if (PermissionUtils.hasStoragePermission(this)) { loadPDFs(); return; }
        if (PermissionUtils.shouldShowRationale(this))
            new AlertDialog.Builder(this).setTitle("Permission Required")
                .setMessage("SwiftDoc needs storage permission to find PDF files.")
                .setPositiveButton("Grant",(d,w)->reqPerm())
                .setNegativeButton("Cancel",(d,w)->showDenied()).show();
        else reqPerm();
    }
    private void reqPerm() {
        permLauncher.launch(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}
            : new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE});
    }
    private void showDenied() { tvEmpty.setText(getString(R.string.permission_denied_msg)); setEmpty(true); }

    private void loadPDFs() {
        pb.setVisibility(View.VISIBLE); rvFiles.setVisibility(View.GONE); emptyLayout.setVisibility(View.GONE);
        AsyncTask.execute(() -> {
            List<PDFFile> sc = FileUtils.scanPDFs(this);
            for (PDFFile p : sc) { p.setFavorite(db.isFavorite(p.getPath())); p.setLastPage(db.getProgressPercent(p.getPath())); }
            runOnUiThread(() -> { allPDFs=sc; pb.setVisibility(View.GONE); filterAndDisplay(etSearch.getText().toString().trim()); });
        });
    }

    private void filterAndDisplay(String q) {
        List<PDFFile> src;
        switch (currentTab) {
            case RECENT:    src = getRecent();          break;
            case FAVORITES: src = db.getFavoriteFiles(); break;
            default:        src = allPDFs;              break;
        }
        List<PDFFile> out = new ArrayList<>();
        for (PDFFile f : src) if (q.isEmpty() || f.getName().toLowerCase().contains(q.toLowerCase())) out.add(f);
        sortList(out);
        adapter.updateList(out);
        setEmpty(adapter.getItemCount() == 0);
        if (adapter.getItemCount()==0) tvEmpty.setText(currentTab==Tab.FAVORITES ? getString(R.string.no_favorites) : q.isEmpty() ? getString(R.string.no_pdfs) : getString(R.string.no_results));
    }

    private List<PDFFile> getRecent() { List<PDFFile> r=db.getRecentFiles(); for(PDFFile f:r) f.setFavorite(db.isFavorite(f.getPath())); return r; }
    private void sortList(List<PDFFile> l) {
        if ("name".equals(sortMode)) Collections.sort(l,(a,b)->a.getName().compareToIgnoreCase(b.getName()));
        else if ("size".equals(sortMode)) Collections.sort(l,(a,b)->Long.compare(b.getSize(),a.getSize()));
    }
    private void setEmpty(boolean e) { emptyLayout.setVisibility(e?View.VISIBLE:View.GONE); rvFiles.setVisibility(e?View.GONE:View.VISIBLE); }

    private void openPicker() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/pdf");
        picker.launch(Intent.createChooser(i,"Select PDF"));
    }

    private void handleUri(Uri uri) {
        if (uri==null) return;
        pb.setVisibility(View.VISIBLE);
        AsyncTask.execute(() -> {
            String n=FileUtils.getFileName(this,uri), p=FileUtils.getPathFromUri(this,uri);
            if (p==null) p=FileUtils.copyToCache(this,uri);
            String fp=p, fn=n!=null?n:(p!=null?new File(p).getName():"file.pdf");
            String tool = prefs.getString("pending_tool", null);
            prefs.edit().remove("pending_tool").apply();
            runOnUiThread(() -> {
                pb.setVisibility(View.GONE);
                if (fp==null) { Toast.makeText(this,getString(R.string.error_opening_file),Toast.LENGTH_SHORT).show(); return; }
                if (tool != null) {
                    if ("crop".equals(tool)) startActivity(new Intent(this,CropActivity.class).putExtra("pdf_path",fp).putExtra("pdf_name",fn));
                    else startActivity(new Intent(this,PdfToolsActivity.class).putExtra("tool",tool).putExtra("pdf_path",fp).putExtra("pdf_name",fn));
                } else openViewer(fn, fp);
            });
        });
    }

    private void openViewer(String n, String p) {
        if (!FileUtils.isValidPDF(p)) { Toast.makeText(this,getString(R.string.error_invalid_pdf),Toast.LENGTH_SHORT).show(); return; }
        startActivity(new Intent(this,PDFViewerActivity.class).putExtra("pdf_path",p).putExtra("pdf_name",n));
    }

    @Override public void onClick(PDFFile f,int pos)       { openViewer(f.getName(),f.getPath()); }
    @Override public void onLongClick(PDFFile f,int pos)   { showFileOptions(f,pos); }
    @Override public void onFavToggle(PDFFile f,int pos)   {
        boolean nf=!f.isFavorite(); if(nf) db.addToFavorites(f); else db.removeFromFavorites(f.getPath());
        f.setFavorite(nf); for(PDFFile p:allPDFs) if(p.getPath().equals(f.getPath())){p.setFavorite(nf);break;}
        adapter.notifyItemChanged(pos);
        Snackbar.make(rvFiles,nf?getString(R.string.added_to_favorites):getString(R.string.removed_from_favorites),Snackbar.LENGTH_SHORT).show();
    }

    private void showFileOptions(PDFFile f,int pos) {
        String[]opts={"Open","Crop","Compress","Share","Favorite","Remove from Recent"};
        new AlertDialog.Builder(this).setTitle(f.getName()).setItems(opts,(d,w)->{
            switch(w){
                case 0: openViewer(f.getName(),f.getPath()); break;
                case 1: startActivity(new Intent(this,CropActivity.class).putExtra("pdf_path",f.getPath()).putExtra("pdf_name",f.getName())); break;
                case 2: startActivity(new Intent(this,PdfToolsActivity.class).putExtra("tool","compress").putExtra("pdf_path",f.getPath()).putExtra("pdf_name",f.getName())); break;
                case 3: share(f); break;
                case 4: onFavToggle(f,pos); break;
                case 5: db.removeFromRecent(f.getPath()); if(currentTab==Tab.RECENT) filterAndDisplay(""); break;
            }
        }).show();
    }

    private void share(PDFFile f) {
        try { File file=new File(f.getPath()); if(!file.exists()){Toast.makeText(this,getString(R.string.file_not_found),Toast.LENGTH_SHORT).show();return;}
            Uri u=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            Intent si=new Intent(Intent.ACTION_SEND); si.setType("application/pdf"); si.putExtra(Intent.EXTRA_STREAM,u); si.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(si,getString(R.string.share_pdf)));
        } catch(Exception e) { Toast.makeText(this,getString(R.string.error_sharing),Toast.LENGTH_SHORT).show(); }
    }

    private void applyDark() { AppCompatDelegate.setDefaultNightMode(prefs.getBoolean(KEY_DARK,false)?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO); }
    private void toggleDark() { boolean d=prefs.getBoolean(KEY_DARK,false); prefs.edit().putBoolean(KEY_DARK,!d).apply(); AppCompatDelegate.setDefaultNightMode(!d?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO); }

    @Override public boolean onCreateOptionsMenu(Menu m) { getMenuInflater().inflate(R.menu.main_menu,m); return true; }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.action_dark_mode){toggleDark();return true;}
        if(id==R.id.action_grid_toggle){gridView=!gridView;adapter.setGridMode(gridView);rvFiles.setLayoutManager(gridView?new GridLayoutManager(this,2):new LinearLayoutManager(this));return true;}
        if(id==R.id.action_sort_name){sortMode="name";filterAndDisplay(etSearch.getText().toString());return true;}
        if(id==R.id.action_sort_date){sortMode="date";filterAndDisplay(etSearch.getText().toString());return true;}
        if(id==R.id.action_sort_size){sortMode="size";filterAndDisplay(etSearch.getText().toString());return true;}
        if(id==R.id.action_refresh){loadPDFs();return true;}
        if(id==R.id.action_stats){startActivity(new Intent(this,StatsActivity.class));return true;}
        return super.onOptionsItemSelected(item);
    }
}
