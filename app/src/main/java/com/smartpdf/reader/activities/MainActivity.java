package com.smartpdf.reader.activities;
import android.app.Activity; import android.content.Intent; import android.content.SharedPreferences;
import android.net.Uri; import android.os.AsyncTask; import android.os.Build; import android.os.Bundle;
import android.text.Editable; import android.text.TextWatcher; import android.view.Menu; import android.view.MenuItem;
import android.view.View; import android.widget.EditText; import android.widget.LinearLayout;
import android.widget.ProgressBar; import android.widget.TextView; import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher; import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog; import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate; import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip; import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.smartpdf.reader.R; import com.smartpdf.reader.adapters.PDFAdapter;
import com.smartpdf.reader.database.DBHelper; import com.smartpdf.reader.models.PDFFile;
import com.smartpdf.reader.utils.FileUtils; import com.smartpdf.reader.utils.PermissionUtils;
import java.io.File; import java.util.ArrayList; import java.util.List;
public class MainActivity extends AppCompatActivity implements PDFAdapter.Listener {
    private static final String PREFS="leaf_prefs",KEY_DARK="dark_mode";
    private RecyclerView rv; private PDFAdapter adapter; private ProgressBar pb;
    private LinearLayout emptyLayout; private TextView tvEmpty; private EditText etSearch;
    private List<PDFFile> allPDFs=new ArrayList<>(); private DBHelper db; private SharedPreferences prefs;
    private enum Tab{ALL,RECENT,FAVORITES} private Tab tab=Tab.ALL;
    private final ActivityResultLauncher<Intent> picker=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),r->{
        if(r.getResultCode()==Activity.RESULT_OK&&r.getData()!=null)handleUri(r.getData().getData());});
    private final ActivityResultLauncher<String[]> permLauncher=registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),m->{
        boolean ok=false;for(Boolean v:m.values())if(v){ok=true;break;}
        if(ok)loadPDFs();else showDenied();});
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);prefs=getSharedPreferences(PREFS,MODE_PRIVATE);applyDark();
        setContentView(R.layout.activity_main);db=DBHelper.getInstance(this);
        Toolbar tb=findViewById(R.id.toolbar);setSupportActionBar(tb);
        if(getSupportActionBar()!=null)getSupportActionBar().setTitle(R.string.app_name);
        rv=findViewById(R.id.recycler_view);pb=findViewById(R.id.progress_bar);
        emptyLayout=findViewById(R.id.layout_empty);tvEmpty=findViewById(R.id.tv_empty_message);
        etSearch=findViewById(R.id.et_search);
        adapter=new PDFAdapter(this,this);rv.setLayoutManager(new LinearLayoutManager(this));rv.setAdapter(adapter);rv.setHasFixedSize(true);
        etSearch.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){filter(s.toString().trim());}
            public void afterTextChanged(Editable s){}});
        ((Chip)findViewById(R.id.chip_all)).setOnClickListener(v->{tab=Tab.ALL;filter(etSearch.getText().toString());});
        ((Chip)findViewById(R.id.chip_recent)).setOnClickListener(v->{tab=Tab.RECENT;filter(etSearch.getText().toString());});
        ((Chip)findViewById(R.id.chip_favorites)).setOnClickListener(v->{tab=Tab.FAVORITES;filter(etSearch.getText().toString());});
        ((FloatingActionButton)findViewById(R.id.fab_open)).setOnClickListener(v->openPicker());
        checkPerm();}
    @Override protected void onResume(){super.onResume();filter(etSearch.getText().toString().trim());}
    private void checkPerm(){
        if(PermissionUtils.hasStoragePermission(this)){loadPDFs();return;}
        if(PermissionUtils.shouldShowRationale(this))
            new AlertDialog.Builder(this).setTitle(R.string.permission_required).setMessage(R.string.permission_rationale)
                .setPositiveButton(R.string.grant,(d,w)->reqPerm()).setNegativeButton(R.string.cancel,(d,w)->showDenied()).show();
        else reqPerm();}
    private void reqPerm(){
        permLauncher.launch(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU?
            new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}:
            new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE});}
    private void showDenied(){tvEmpty.setText(R.string.permission_denied_msg);setEmpty(true);}
    private void loadPDFs(){
        pb.setVisibility(View.VISIBLE);rv.setVisibility(View.GONE);emptyLayout.setVisibility(View.GONE);
        AsyncTask.execute(()->{
            List<PDFFile> s=FileUtils.scanPDFs(this);
            for(PDFFile p:s)p.setFavorite(db.isFavorite(p.getPath()));
            runOnUiThread(()->{allPDFs=s;pb.setVisibility(View.GONE);filter(etSearch.getText().toString().trim());});});}
    private void filter(String q){
        List<PDFFile> src; switch(tab){case RECENT:src=getRecent();break;case FAVORITES:src=db.getFavoriteFiles();break;default:src=allPDFs;}
        List<PDFFile> out=new ArrayList<>();
        for(PDFFile f:src)if(q.isEmpty()||f.getName().toLowerCase().contains(q.toLowerCase()))out.add(f);
        adapter.updateList(out);setEmpty(adapter.getItemCount()==0);
        if(adapter.getItemCount()==0)tvEmpty.setText(tab==Tab.FAVORITES?R.string.no_favorites:q.isEmpty()?R.string.no_pdfs_found:R.string.no_results);}
    private List<PDFFile> getRecent(){List<PDFFile> r=db.getRecentFiles();for(PDFFile f:r)f.setFavorite(db.isFavorite(f.getPath()));return r;}
    private void setEmpty(boolean e){emptyLayout.setVisibility(e?View.VISIBLE:View.GONE);rv.setVisibility(e?View.GONE:View.VISIBLE);}
    private void openPicker(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");picker.launch(Intent.createChooser(i,getString(R.string.select_pdf)));}
    private void handleUri(Uri uri){
        if(uri==null)return; pb.setVisibility(View.VISIBLE);
        AsyncTask.execute(()->{
            String n=FileUtils.getFileName(this,uri),p=FileUtils.getPathFromUri(this,uri);
            if(p==null)p=FileUtils.copyToCache(this,uri);
            String fp=p,fn=n!=null?n:(p!=null?new File(p).getName():"file.pdf");
            runOnUiThread(()->{pb.setVisibility(View.GONE);if(fp==null){Toast.makeText(this,R.string.error_opening_file,Toast.LENGTH_SHORT).show();return;}openViewer(fn,fp);});});}
    private void openViewer(String n,String p){
        if(!FileUtils.isValidPDF(p)){Toast.makeText(this,R.string.error_invalid_pdf,Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(this,PDFViewerActivity.class);i.putExtra("pdf_path",p);i.putExtra("pdf_name",n);startActivity(i);}
    @Override public void onClick(PDFFile f,int pos){openViewer(f.getName(),f.getPath());}
    @Override public void onLongClick(PDFFile f,int pos){
        BottomSheetDialog sh=new BottomSheetDialog(this,R.style.BottomSheetStyle);
        View v=getLayoutInflater().inflate(R.layout.bottom_sheet_pdf_options,null);sh.setContentView(v);
        ((TextView)v.findViewById(R.id.tv_sheet_title)).setText(f.getName());
        ((TextView)v.findViewById(R.id.tv_sheet_size)).setText(f.getFormattedSize()+" · "+f.getDate());
        ((TextView)v.findViewById(R.id.tv_favorite_label)).setText(f.isFavorite()?R.string.remove_from_favorites:R.string.add_to_favorites);
        v.findViewById(R.id.option_favorite).setOnClickListener(x->{onFavToggle(f,pos);sh.dismiss();});
        v.findViewById(R.id.option_share).setOnClickListener(x->{share(f);sh.dismiss();});
        v.findViewById(R.id.option_remove_recent).setOnClickListener(x->{db.removeFromRecent(f.getPath());
            Snackbar.make(rv,R.string.removed_from_recent,Snackbar.LENGTH_SHORT).show();
            if(tab==Tab.RECENT)filter(etSearch.getText().toString());sh.dismiss();});
        sh.show();}
    @Override public void onFavToggle(PDFFile f,int pos){
        boolean nf=!f.isFavorite();if(nf)db.addToFavorites(f);else db.removeFromFavorites(f.getPath());
        f.setFavorite(nf);for(PDFFile p:allPDFs)if(p.getPath().equals(f.getPath())){p.setFavorite(nf);break;}
        adapter.notifyItemChanged(pos);Snackbar.make(rv,nf?R.string.added_to_favorites:R.string.removed_from_favorites,Snackbar.LENGTH_SHORT).show();}
    private void share(PDFFile f){
        try{File file=new File(f.getPath());if(!file.exists()){Toast.makeText(this,R.string.file_not_found,Toast.LENGTH_SHORT).show();return;}
            Uri u=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            Intent si=new Intent(Intent.ACTION_SEND);si.setType("application/pdf");si.putExtra(Intent.EXTRA_STREAM,u);si.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(si,getString(R.string.share_pdf)));}
        catch(Exception e){Toast.makeText(this,R.string.error_sharing,Toast.LENGTH_SHORT).show();}}
    private void applyDark(){AppCompatDelegate.setDefaultNightMode(prefs.getBoolean(KEY_DARK,false)?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO);}
    @Override public boolean onCreateOptionsMenu(Menu m){getMenuInflater().inflate(R.menu.main_menu,m);return true;}
    @Override public boolean onOptionsItemSelected(MenuItem item){
        int id=item.getItemId();
        if(id==R.id.action_dark_mode){boolean d=prefs.getBoolean(KEY_DARK,false);prefs.edit().putBoolean(KEY_DARK,!d).apply();AppCompatDelegate.setDefaultNightMode(!d?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO);}
        else if(id==R.id.action_refresh)checkPerm();
        else if(id==R.id.action_clear_recent){db.clearRecent();Snackbar.make(rv,R.string.recent_cleared,Snackbar.LENGTH_SHORT).show();if(tab==Tab.RECENT)filter("");}
        return super.onOptionsItemSelected(item);}
}
