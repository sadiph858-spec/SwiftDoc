package com.smartpdf.reader.activities;
import android.content.Intent; import android.net.Uri; import android.os.Bundle;
import android.view.Menu; import android.view.MenuItem; import android.view.View;
import android.widget.ProgressBar; import android.widget.TextView; import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity; import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import com.github.barteksc.pdfviewer.PDFView; import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.smartpdf.reader.R; import com.smartpdf.reader.database.DBHelper; import com.smartpdf.reader.models.PDFFile;
import java.io.File; import java.text.SimpleDateFormat; import java.util.Date; import java.util.Locale;
public class PDFViewerActivity extends AppCompatActivity {
    private PDFView pdfView; private ProgressBar pb; private TextView tvPage,tvError; private View loadLayout;
    private FloatingActionButton fabFav;
    private String path,name; private int total=0; private DBHelper db; private boolean fav=false;
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_pdf_viewer);
        db=DBHelper.getInstance(this);path=getIntent().getStringExtra("pdf_path");name=getIntent().getStringExtra("pdf_name");
        if(path==null){Toast.makeText(this,R.string.error_no_file,Toast.LENGTH_SHORT).show();finish();return;}
        Toolbar tb=findViewById(R.id.toolbar_viewer);setSupportActionBar(tb);
        if(getSupportActionBar()!=null){getSupportActionBar().setDisplayHomeAsUpEnabled(true);getSupportActionBar().setTitle(name!=null?name:getString(R.string.app_name));}
        pdfView=findViewById(R.id.pdf_view);pb=findViewById(R.id.progress_bar_viewer);
        tvPage=findViewById(R.id.tv_page_indicator);tvError=findViewById(R.id.tv_error);
        loadLayout=findViewById(R.id.layout_loading);fabFav=findViewById(R.id.fab_favorite);
        fabFav.setOnClickListener(v->toggleFav());
        fav=db.isFavorite(path);updateFavIcon();saveRecent();loadPDF();}
    private void loadPDF(){
        File f=new File(path);if(!f.exists()){showErr(getString(R.string.error_file_not_found));return;}
        loadLayout.setVisibility(View.VISIBLE);pdfView.setVisibility(View.GONE);tvError.setVisibility(View.GONE);
        try{pdfView.fromFile(f).defaultPage(0).enableSwipe(true).swipeHorizontal(false)
            .enableDoubletap(true).enableAntialiasing(true).spacing(4).pageFitPolicy(FitPolicy.WIDTH)
            .scrollHandle(new DefaultScrollHandle(this))
            .onLoad(n->runOnUiThread(()->{total=n;loadLayout.setVisibility(View.GONE);pdfView.setVisibility(View.VISIBLE);tvPage.setText(getString(R.string.page_indicator,1,n));}))
            .onPageChange((p,c)->runOnUiThread(()->tvPage.setText(getString(R.string.page_indicator,p+1,c))))
            .onError(e->runOnUiThread(()->{loadLayout.setVisibility(View.GONE);showErr(getString(R.string.error_loading_pdf));}))
            .load();}
        catch(Exception e){loadLayout.setVisibility(View.GONE);showErr(getString(R.string.error_loading_pdf));}}
    private void showErr(String m){tvError.setText(m);tvError.setVisibility(View.VISIBLE);pdfView.setVisibility(View.GONE);loadLayout.setVisibility(View.GONE);}
    private void saveRecent(){File f=new File(path);String d=new SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(new Date(f.lastModified()));db.addToRecent(new PDFFile(name!=null?name:f.getName(),path,d,f.length()));}
    private void toggleFav(){
        File f=new File(path);String d=new SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(new Date(f.lastModified()));
        PDFFile p=new PDFFile(name!=null?name:f.getName(),path,d,f.length());
        if(fav){db.removeFromFavorites(path);fav=false;Snackbar.make(pdfView,R.string.removed_from_favorites,Snackbar.LENGTH_SHORT).show();}
        else{db.addToFavorites(p);fav=true;Snackbar.make(pdfView,R.string.added_to_favorites,Snackbar.LENGTH_SHORT).show();}
        updateFavIcon();}
    private void updateFavIcon(){fabFav.setImageResource(fav?R.drawable.ic_star_filled:R.drawable.ic_star_outline);}
    private void sharePDF(){
        try{File f=new File(path);if(!f.exists()){Toast.makeText(this,R.string.file_not_found,Toast.LENGTH_SHORT).show();return;}
            Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);
            Intent si=new Intent(Intent.ACTION_SEND);si.setType("application/pdf");si.putExtra(Intent.EXTRA_STREAM,u);si.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(si,getString(R.string.share_pdf)));}
        catch(Exception e){Toast.makeText(this,R.string.error_sharing,Toast.LENGTH_SHORT).show();}}
    @Override public boolean onCreateOptionsMenu(Menu m){getMenuInflater().inflate(R.menu.viewer_menu,m);return true;}
    @Override public boolean onOptionsItemSelected(MenuItem item){
        int id=item.getItemId();
        if(id==android.R.id.home){onBackPressed();return true;}
        if(id==R.id.action_share){sharePDF();return true;}
        if(id==R.id.action_go_to_first){pdfView.jumpTo(0,true);return true;}
        if(id==R.id.action_go_to_last&&total>0){pdfView.jumpTo(total-1,true);return true;}
        return super.onOptionsItemSelected(item);}
    @Override protected void onDestroy(){super.onDestroy();if(pdfView!=null)pdfView.recycle();}
}
