package com.smartpdf.reader.activities;
import android.content.Intent; import android.content.SharedPreferences; import android.net.Uri;
import android.os.AsyncTask; import android.os.Build; import android.os.Bundle;
import android.text.Editable; import android.text.TextWatcher; import android.view.Menu; import android.view.MenuItem;
import android.view.View; import android.widget.EditText; import android.widget.FrameLayout;
import android.widget.LinearLayout; import android.widget.ProgressBar; import android.widget.TextView; import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher; import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog; import androidx.appcompat.app.AppCompatActivity; import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar; import androidx.recyclerview.widget.GridLayoutManager; import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView; import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton; import com.google.android.material.snackbar.Snackbar;
import com.smartpdf.reader.R; import com.smartpdf.reader.adapters.DashboardAdapter; import com.smartpdf.reader.adapters.PDFAdapter;
import com.smartpdf.reader.database.DBHelper; import com.smartpdf.reader.models.DashboardItem; import com.smartpdf.reader.models.PDFFile;
import com.smartpdf.reader.utils.AdManager; import com.smartpdf.reader.utils.FileUtils; import com.smartpdf.reader.utils.PermissionUtils;
import java.io.File; import java.util.ArrayList; import java.util.Collections; import java.util.Comparator; import java.util.List;

public class MainActivity extends AppCompatActivity implements PDFAdapter.Listener {
    public static final int ACTION_VIEW_PDF=1,ACTION_IMG2PDF=2,ACTION_PDF2IMG=3,ACTION_COMPRESS=4,ACTION_MERGE=5,
        ACTION_SPLIT=6,ACTION_LOCK=7,ACTION_UNLOCK=8,ACTION_OCR=9,ACTION_TXT2PDF=10,ACTION_TTS=11,
        ACTION_SIGN=12,ACTION_WATERMARK=13,ACTION_REORDER=14,ACTION_DELETE_PAGES=15,ACTION_ROTATE=16,
        ACTION_BOOKMARKS=17,ACTION_RECENT=18,ACTION_SEARCH=19,ACTION_METADATA=20,ACTION_DARK=21,ACTION_PRINT=22,ACTION_STATS=23;

    private static final String PREFS="leaf_prefs",KEY_DARK="dark_mode",KEY_VIEW="view_mode";
    private RecyclerView rvDash,rvFiles; private PDFAdapter adapter; private DashboardAdapter dashAdapter;
    private ProgressBar pb; private LinearLayout emptyLayout; private TextView tvEmpty; private EditText etSearch;
    private FrameLayout adContainer; private View tabHome,tabTools,tabHistory;
    private List<PDFFile> allPDFs=new ArrayList<>(); private DBHelper db; private SharedPreferences prefs;
    private String sortMode="date"; private boolean gridView=false;

    private final ActivityResultLauncher<Intent> picker=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),r->{
        if(r.getResultCode()==RESULT_OK&&r.getData()!=null)handleUri(r.getData().getData());});
    private final ActivityResultLauncher<String[]> permLauncher=registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),m->{
        boolean ok=false;for(Boolean v:m.values())if(v){ok=true;break;}if(ok)loadPDFs();else showDenied();});

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s); prefs=getSharedPreferences(PREFS,MODE_PRIVATE); applyDark();
        setContentView(R.layout.activity_main); db=DBHelper.getInstance(this);
        setupToolbar(); setupViews(); setupBottomNav(); setupDashboard();
        AdManager.loadBanner(this,adContainer); AdManager.loadInterstitial(this);
        checkPerm();}

    @Override protected void onResume(){super.onResume();filterAndDisplay(etSearch.getText().toString().trim());}

    private void setupToolbar(){Toolbar tb=findViewById(R.id.toolbar);setSupportActionBar(tb);if(getSupportActionBar()!=null)getSupportActionBar().setTitle("LeafPDF");}

    private void setupViews(){
        rvDash=findViewById(R.id.rv_dashboard); rvFiles=findViewById(R.id.rv_files);
        pb=findViewById(R.id.progress_bar); emptyLayout=findViewById(R.id.layout_empty);
        tvEmpty=findViewById(R.id.tv_empty); etSearch=findViewById(R.id.et_search);
        adContainer=findViewById(R.id.ad_container);
        adapter=new PDFAdapter(this,this,gridView); rvFiles.setAdapter(adapter);
        setListLayoutManager();
        etSearch.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){filterAndDisplay(s.toString().trim());}
            public void afterTextChanged(Editable s){}});
        ((FloatingActionButton)findViewById(R.id.fab_open)).setOnClickListener(v->openPicker());
        ((Chip)findViewById(R.id.chip_all)).setOnClickListener(v->filterAndDisplay(etSearch.getText().toString()));
        ((Chip)findViewById(R.id.chip_recent)).setOnClickListener(v->showRecent());
        ((Chip)findViewById(R.id.chip_favs)).setOnClickListener(v->showFavorites());}

    private void setupBottomNav(){
        BottomNavigationView bnv=findViewById(R.id.bottom_nav);
        bnv.setOnItemSelectedListener(item->{
            int id=item.getItemId();
            if(id==R.id.nav_home){showHomeTab();return true;}
            if(id==R.id.nav_tools){showToolsTab();return true;}
            if(id==R.id.nav_history){startActivity(new Intent(this,StatsActivity.class));return true;}
            return false;});}

    private void setupDashboard(){
        List<DashboardItem> items=new ArrayList<>();
        items.add(new DashboardItem(R.drawable.ic_view_pdf,"View PDF","Open & read",R.color.green_medium,ACTION_VIEW_PDF));
        items.add(new DashboardItem(R.drawable.ic_img2pdf,"Image→PDF","Convert images",R.color.green_dark,ACTION_IMG2PDF));
        items.add(new DashboardItem(R.drawable.ic_pdf2img,"PDF→Image","Export pages",R.color.teal_medium,ACTION_PDF2IMG));
        items.add(new DashboardItem(R.drawable.ic_compress,"Compress","Reduce size",R.color.green_medium,ACTION_COMPRESS));
        items.add(new DashboardItem(R.drawable.ic_merge,"Merge PDF","Combine files",R.color.green_dark,ACTION_MERGE));
        items.add(new DashboardItem(R.drawable.ic_split,"Split PDF","Divide pages",R.color.teal_medium,ACTION_SPLIT));
        items.add(new DashboardItem(R.drawable.ic_lock,"Lock PDF","Add password",R.color.green_medium,ACTION_LOCK));
        items.add(new DashboardItem(R.drawable.ic_unlock,"Unlock PDF","Remove password",R.color.green_dark,ACTION_UNLOCK));
        items.add(new DashboardItem(R.drawable.ic_ocr,"OCR","Extract text",R.color.teal_medium,ACTION_OCR));
        items.add(new DashboardItem(R.drawable.ic_txt2pdf,"Text→PDF","Create from text",R.color.green_medium,ACTION_TXT2PDF));
        items.add(new DashboardItem(R.drawable.ic_tts,"Read Aloud","Text to speech",R.color.green_dark,ACTION_TTS));
        items.add(new DashboardItem(R.drawable.ic_sign,"Signature","Digital sign",R.color.teal_medium,ACTION_SIGN));
        items.add(new DashboardItem(R.drawable.ic_watermark,"Watermark","Add watermark",R.color.green_medium,ACTION_WATERMARK));
        items.add(new DashboardItem(R.drawable.ic_reorder,"Reorder","Sort pages",R.color.green_dark,ACTION_REORDER));
        items.add(new DashboardItem(R.drawable.ic_delete_page,"Delete Pages","Remove pages",R.color.teal_medium,ACTION_DELETE_PAGES));
        items.add(new DashboardItem(R.drawable.ic_rotate,"Rotate","Rotate pages",R.color.green_medium,ACTION_ROTATE));
        items.add(new DashboardItem(R.drawable.ic_bookmark,"Bookmarks","Saved pages",R.color.green_dark,ACTION_BOOKMARKS));
        items.add(new DashboardItem(R.drawable.ic_recent,"Recent","Last opened",R.color.teal_medium,ACTION_RECENT));
        items.add(new DashboardItem(R.drawable.ic_search_pdf,"Search Text","Find in PDF",R.color.green_medium,ACTION_SEARCH));
        items.add(new DashboardItem(R.drawable.ic_metadata,"Metadata","Edit file info",R.color.green_dark,ACTION_METADATA));
        items.add(new DashboardItem(R.drawable.ic_dark_mode,"Dark Mode","Toggle theme",R.color.teal_medium,ACTION_DARK));
        items.add(new DashboardItem(R.drawable.ic_print,"Print","Print document",R.color.green_medium,ACTION_PRINT));
        items.add(new DashboardItem(R.drawable.ic_stats,"Statistics","Reading stats",R.color.green_dark,ACTION_STATS));
        dashAdapter=new DashboardAdapter(this,items,this::onDashAction);
        rvDash.setLayoutManager(new GridLayoutManager(this,3));
        rvDash.setAdapter(dashAdapter);}

    private void onDashAction(int action){
        switch(action){
            case ACTION_VIEW_PDF: openPicker(); break;
            case ACTION_IMG2PDF: startActivity(new Intent(this,ImageToPdfActivity.class)); break;
            case ACTION_PDF2IMG: pickAndRun(ACTION_PDF2IMG); break;
            case ACTION_COMPRESS: pickAndRun(ACTION_COMPRESS); break;
            case ACTION_MERGE: startActivity(new Intent(this,PdfToolsActivity.class).putExtra("tool","merge")); break;
            case ACTION_SPLIT: pickAndRun(ACTION_SPLIT); break;
            case ACTION_LOCK: pickAndRun(ACTION_LOCK); break;
            case ACTION_UNLOCK: pickAndRun(ACTION_UNLOCK); break;
            case ACTION_OCR: pickAndRun(ACTION_OCR); break;
            case ACTION_TXT2PDF: showTextToPdf(); break;
            case ACTION_TTS: pickAndRun(ACTION_TTS); break;
            case ACTION_SIGN: pickAndRun(ACTION_SIGN); break;
            case ACTION_WATERMARK: pickAndRun(ACTION_WATERMARK); break;
            case ACTION_REORDER: pickAndRun(ACTION_REORDER); break;
            case ACTION_DELETE_PAGES: pickAndRun(ACTION_DELETE_PAGES); break;
            case ACTION_ROTATE: pickAndRun(ACTION_ROTATE); break;
            case ACTION_BOOKMARKS: showRecent(); break;
            case ACTION_RECENT: showRecent(); break;
            case ACTION_SEARCH: etSearch.requestFocus(); break;
            case ACTION_METADATA: pickAndRun(ACTION_METADATA); break;
            case ACTION_DARK: toggleDark(); break;
            case ACTION_PRINT: pickAndRun(ACTION_PRINT); break;
            case ACTION_STATS: startActivity(new Intent(this,StatsActivity.class)); break;}}

    private void pickAndRun(int action){
        Toast.makeText(this,"Pick a PDF first",Toast.LENGTH_SHORT).show();
        // Store pending action, open picker
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().putInt("pending_action",action).apply();
        openPicker();}

    private void showTextToPdf(){
        android.widget.EditText et=new android.widget.EditText(this);
        et.setHint("Enter text to convert to PDF..."); et.setMinLines(5); et.setMaxLines(12);
        new AlertDialog.Builder(this).setTitle("Text to PDF").setView(et)
            .setPositiveButton("Convert",(d,w)->{
                String txt=et.getText().toString().trim();
                if(txt.isEmpty()){Toast.makeText(this,"Enter some text",Toast.LENGTH_SHORT).show();return;}
                startActivity(new Intent(this,PdfToolsActivity.class).putExtra("tool","txt2pdf").putExtra("text",txt));})
            .setNegativeButton("Cancel",null).show();}

    private void showHomeTab(){rvDash.setVisibility(View.GONE);rvFiles.setVisibility(View.VISIBLE);emptyLayout.setVisibility(adapter.getItemCount()==0?View.VISIBLE:View.GONE);}
    private void showToolsTab(){rvDash.setVisibility(View.VISIBLE);rvFiles.setVisibility(View.GONE);emptyLayout.setVisibility(View.GONE);}

    private void setListLayoutManager(){
        if(gridView)rvFiles.setLayoutManager(new GridLayoutManager(this,2));
        else rvFiles.setLayoutManager(new LinearLayoutManager(this));}

    private void checkPerm(){if(PermissionUtils.hasStoragePermission(this)){loadPDFs();return;}
        if(PermissionUtils.shouldShowRationale(this))
            new AlertDialog.Builder(this).setTitle("Permission Required").setMessage("LeafPDF needs storage permission to find PDF files.")
                .setPositiveButton("Grant",(d,w)->reqPerm()).setNegativeButton("Cancel",(d,w)->showDenied()).show();
        else reqPerm();}
    private void reqPerm(){permLauncher.launch(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU?new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}:new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE});}
    private void showDenied(){tvEmpty.setText("Storage permission denied.\nTap + to open a PDF manually.");setEmpty(true);}

    private void loadPDFs(){pb.setVisibility(View.VISIBLE);rvFiles.setVisibility(View.GONE);emptyLayout.setVisibility(View.GONE);
        AsyncTask.execute(()->{List<PDFFile> sc=FileUtils.scanPDFs(this);for(PDFFile p:sc){p.setFavorite(db.isFavorite(p.getPath()));int pct=db.getProgressPercent(p.getPath());p.setLastPage(pct);}
            runOnUiThread(()->{allPDFs=sc;pb.setVisibility(View.GONE);filterAndDisplay(etSearch.getText().toString().trim());});}); }

    private void filterAndDisplay(String q){
        List<PDFFile> out=new ArrayList<>();
        for(PDFFile f:allPDFs)if(q.isEmpty()||f.getName().toLowerCase().contains(q.toLowerCase()))out.add(f);
        sortList(out); adapter.updateList(out); setEmpty(adapter.getItemCount()==0);
        if(adapter.getItemCount()==0)tvEmpty.setText(q.isEmpty()?"No PDF files found.\nTap + to open.":"No results for: "+q);}
    private void showRecent(){List<PDFFile> r=db.getRecentFiles();for(PDFFile f:r)f.setFavorite(db.isFavorite(f.getPath()));adapter.updateList(r);setEmpty(r.isEmpty());if(r.isEmpty())tvEmpty.setText("No recent files yet.");}
    private void showFavorites(){List<PDFFile> r=db.getFavoriteFiles();adapter.updateList(r);setEmpty(r.isEmpty());if(r.isEmpty())tvEmpty.setText("No favorites yet.");}
    private void sortList(List<PDFFile> l){if("name".equals(sortMode))Collections.sort(l,(a,b)->a.getName().compareToIgnoreCase(b.getName()));else if("size".equals(sortMode))Collections.sort(l,(a,b)->Long.compare(b.getSize(),a.getSize()));else Collections.sort(l,(a,b)->b.getLastOpened()!=0?Long.compare(b.getLastOpened(),a.getLastOpened()):0);}
    private void setEmpty(boolean e){emptyLayout.setVisibility(e?View.VISIBLE:View.GONE);rvFiles.setVisibility(e?View.GONE:View.VISIBLE);}

    private void openPicker(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");picker.launch(Intent.createChooser(i,"Select PDF"));}
    private void handleUri(Uri uri){if(uri==null)return;AsyncTask.execute(()->{String n=FileUtils.getFileName(this,uri),p=FileUtils.getPathFromUri(this,uri);if(p==null)p=FileUtils.copyToCache(this,uri);String fp=p,fn=n!=null?n:(p!=null?new File(p).getName():"file.pdf");
        // Check pending action
        int action=getSharedPreferences(PREFS,MODE_PRIVATE).getInt("pending_action",-1);
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().remove("pending_action").apply();
        runOnUiThread(()->{if(fp==null){Toast.makeText(this,"Could not open file",Toast.LENGTH_SHORT).show();return;}
            if(action>1){startActivity(new Intent(this,PdfToolsActivity.class).putExtra("tool",""+action).putExtra("pdf_path",fp).putExtra("pdf_name",fn));}
            else openViewer(fn,fp);});});}
    private void openViewer(String n,String p){if(!FileUtils.isValidPDF(p)){Toast.makeText(this,"Invalid PDF",Toast.LENGTH_SHORT).show();return;}startActivity(new Intent(this,PDFViewerActivity.class).putExtra("pdf_path",p).putExtra("pdf_name",n));}

    @Override public void onClick(PDFFile f,int pos){openViewer(f.getName(),f.getPath());}
    @Override public void onLongClick(PDFFile f,int pos){showFileOptions(f,pos);}
    @Override public void onFavToggle(PDFFile f,int pos){boolean nf=!f.isFavorite();if(nf)db.addToFavorites(f);else db.removeFromFavorites(f.getPath());f.setFavorite(nf);for(PDFFile p:allPDFs)if(p.getPath().equals(f.getPath())){p.setFavorite(nf);break;}adapter.notifyItemChanged(pos);Snackbar.make(rvFiles,nf?"Added to favorites":"Removed from favorites",Snackbar.LENGTH_SHORT).show();}

    private void showFileOptions(PDFFile f,int pos){
        String[]opts={"Open","Share","Add to Favorites","Remove from Recent","Compress","Delete from List"};
        new AlertDialog.Builder(this).setTitle(f.getName()).setItems(opts,(d,w)->{
            switch(w){case 0:openViewer(f.getName(),f.getPath());break;case 1:share(f);break;case 2:onFavToggle(f,pos);break;case 3:db.removeFromRecent(f.getPath());showRecent();break;case 4:startActivity(new Intent(this,PdfToolsActivity.class).putExtra("tool","compress").putExtra("pdf_path",f.getPath()).putExtra("pdf_name",f.getName()));break;case 5:allPDFs.remove(f);filterAndDisplay(etSearch.getText().toString());break;}}).show();}

    private void share(PDFFile f){try{File file=new File(f.getPath());if(!file.exists()){Toast.makeText(this,"File not found",Toast.LENGTH_SHORT).show();return;}Uri u=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);Intent si=new Intent(Intent.ACTION_SEND);si.setType("application/pdf");si.putExtra(Intent.EXTRA_STREAM,u);si.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(si,"Share PDF"));}catch(Exception e){Toast.makeText(this,"Cannot share",Toast.LENGTH_SHORT).show();}}

    private void applyDark(){AppCompatDelegate.setDefaultNightMode(prefs.getBoolean(KEY_DARK,false)?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO);}
    private void toggleDark(){boolean d=prefs.getBoolean(KEY_DARK,false);prefs.edit().putBoolean(KEY_DARK,!d).apply();AppCompatDelegate.setDefaultNightMode(!d?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO);}

    @Override public boolean onCreateOptionsMenu(Menu m){getMenuInflater().inflate(R.menu.main_menu,m);return true;}
    @Override public boolean onOptionsItemSelected(MenuItem item){int id=item.getItemId();
        if(id==R.id.action_dark_mode){toggleDark();return true;}
        if(id==R.id.action_grid_toggle){gridView=!gridView;adapter.setGridMode(gridView);setListLayoutManager();return true;}
        if(id==R.id.action_sort_name){sortMode="name";filterAndDisplay(etSearch.getText().toString());return true;}
        if(id==R.id.action_sort_date){sortMode="date";filterAndDisplay(etSearch.getText().toString());return true;}
        if(id==R.id.action_sort_size){sortMode="size";filterAndDisplay(etSearch.getText().toString());return true;}
        if(id==R.id.action_refresh){loadPDFs();return true;}
        if(id==R.id.action_stats){startActivity(new Intent(this,StatsActivity.class));return true;}
        return super.onOptionsItemSelected(item);}
}
