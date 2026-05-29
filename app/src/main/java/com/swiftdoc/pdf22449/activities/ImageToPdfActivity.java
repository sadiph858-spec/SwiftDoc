package com.swiftdoc.pdf22449.activities;
import android.content.Intent; import android.graphics.Bitmap; import android.graphics.BitmapFactory;
import android.graphics.Canvas; import android.graphics.ColorMatrix; import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint; import android.net.Uri; import android.os.AsyncTask; import android.os.Bundle;
import android.view.MenuItem; import android.view.View; import android.widget.Button; import android.widget.FrameLayout;
import android.widget.ImageView; import android.widget.ProgressBar; import android.widget.RadioGroup; import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher; import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity; import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager; import androidx.recyclerview.widget.RecyclerView;
import com.itextpdf.text.Document; import com.itextpdf.text.Image; import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.swiftdoc.pdf22449.R; import com.swiftdoc.pdf22449.adapters.ImagePreviewAdapter;
import com.swiftdoc.pdf22449.utils.AdManager;
import java.io.ByteArrayOutputStream; import java.io.File; import java.io.FileOutputStream;
import java.io.InputStream; import java.text.SimpleDateFormat; import java.util.ArrayList;
import java.util.Date; import java.util.List; import java.util.Locale;

public class ImageToPdfActivity extends AppCompatActivity {
    public static final int FILTER_ORIGINAL=0,FILTER_DOCUMENT=1,FILTER_GRAYSCALE=2,FILTER_BW=3,FILTER_BRIGHT=4;
    private RecyclerView rvImages; private ImagePreviewAdapter imageAdapter;
    private RadioGroup rgFilter; private Button btnConvert,btnAddMore; private ProgressBar pb;
    private ImageView ivPreview; private FrameLayout adContainer;
    private List<Uri> selectedUris=new ArrayList<>(); private int currentFilter=FILTER_ORIGINAL;

    private final ActivityResultLauncher<Intent> imagePicker=registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),result->{
            if(result.getResultCode()==RESULT_OK&&result.getData()!=null){
                Intent data=result.getData();
                if(data.getClipData()!=null){int c=data.getClipData().getItemCount();for(int i=0;i<c;i++)selectedUris.add(data.getClipData().getItemAt(i).getUri());}
                else if(data.getData()!=null)selectedUris.add(data.getData());
                imageAdapter.updateList(selectedUris); updatePreview();}});

    @Override protected void onCreate(Bundle s){
        super.onCreate(s); setContentView(R.layout.activity_image_to_pdf);
        Toolbar tb=findViewById(R.id.toolbar_img2pdf); setSupportActionBar(tb);
        if(getSupportActionBar()!=null){getSupportActionBar().setDisplayHomeAsUpEnabled(true);getSupportActionBar().setTitle("Image to PDF");}
        rvImages=findViewById(R.id.rv_images); ivPreview=findViewById(R.id.iv_filter_preview);
        rgFilter=findViewById(R.id.rg_filter); btnConvert=findViewById(R.id.btn_convert);
        btnAddMore=findViewById(R.id.btn_add_images); pb=findViewById(R.id.pb_convert);
        adContainer=findViewById(R.id.ad_container);
        imageAdapter=new ImagePreviewAdapter(this,selectedUris);
        rvImages.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        rvImages.setAdapter(imageAdapter);
        rgFilter.setOnCheckedChangeListener((g,id)->{
            if(id==R.id.rb_original)currentFilter=FILTER_ORIGINAL;
            else if(id==R.id.rb_document)currentFilter=FILTER_DOCUMENT;
            else if(id==R.id.rb_grayscale)currentFilter=FILTER_GRAYSCALE;
            else if(id==R.id.rb_bw)currentFilter=FILTER_BW;
            else if(id==R.id.rb_bright)currentFilter=FILTER_BRIGHT;
            updatePreview();});
        btnAddMore.setOnClickListener(v->pickImages());
        btnConvert.setOnClickListener(v->convertToPdf());
        AdManager.loadBanner(this,adContainer);
        pickImages();}

    private void pickImages(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); i.addCategory(Intent.CATEGORY_OPENABLE);
        imagePicker.launch(Intent.createChooser(i,"Select Images"));}

    private void updatePreview(){
        if(selectedUris.isEmpty())return;
        AsyncTask.execute(()->{try{InputStream is=getContentResolver().openInputStream(selectedUris.get(0));Bitmap bmp=BitmapFactory.decodeStream(is);if(is!=null)is.close();Bitmap filtered=applyFilter(bmp,currentFilter);runOnUiThread(()->ivPreview.setImageBitmap(filtered));}catch(Exception e){e.printStackTrace();}});}

    public static Bitmap applyFilter(Bitmap src,int filter){
        if(src==null)return null;
        Bitmap out=Bitmap.createBitmap(src.getWidth(),src.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(out); Paint paint=new Paint();
        switch(filter){
            case FILTER_GRAYSCALE:{ColorMatrix cm=new ColorMatrix();cm.setSaturation(0);paint.setColorFilter(new ColorMatrixColorFilter(cm));canvas.drawBitmap(src,0,0,paint);break;}
            case FILTER_BW:{ColorMatrix cm=new ColorMatrix();cm.setSaturation(0);float[]arr=cm.getArray();arr[0]=2f;arr[6]=2f;arr[12]=2f;arr[18]=1f;arr[4]=-100;arr[9]=-100;arr[14]=-100;paint.setColorFilter(new ColorMatrixColorFilter(cm));canvas.drawBitmap(src,0,0,paint);break;}
            case FILTER_DOCUMENT:{ColorMatrix cm=new ColorMatrix();cm.set(new float[]{1.3f,0,0,0,-30,0,1.3f,0,0,-30,0,0,1.3f,0,-30,0,0,0,1,0});paint.setColorFilter(new ColorMatrixColorFilter(cm));canvas.drawBitmap(src,0,0,paint);break;}
            case FILTER_BRIGHT:{ColorMatrix cm=new ColorMatrix();cm.set(new float[]{1,0,0,0,40,0,1,0,0,40,0,0,1,0,40,0,0,0,1,0});paint.setColorFilter(new ColorMatrixColorFilter(cm));canvas.drawBitmap(src,0,0,paint);break;}
            default:return src;}
        return out;}

    private void convertToPdf(){
        if(selectedUris.isEmpty()){Toast.makeText(this,"Select at least one image",Toast.LENGTH_SHORT).show();return;}
        pb.setVisibility(View.VISIBLE); btnConvert.setEnabled(false);
        AsyncTask.execute(()->{try{
            String fname="SwiftDoc_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date())+".pdf";
            File outFile=new File(getExternalFilesDir(null),fname);
            Document doc=new Document(PageSize.A4,20,20,20,20);
            PdfWriter.getInstance(doc,new FileOutputStream(outFile)); doc.open();
            for(Uri uri:selectedUris){InputStream is=getContentResolver().openInputStream(uri);Bitmap bmp=BitmapFactory.decodeStream(is);if(is!=null)is.close();Bitmap filtered=applyFilter(bmp,currentFilter);ByteArrayOutputStream baos=new ByteArrayOutputStream();filtered.compress(Bitmap.CompressFormat.JPEG,90,baos);Image img=Image.getInstance(baos.toByteArray());img.scaleToFit(PageSize.A4.getWidth()-40,PageSize.A4.getHeight()-40);img.setAlignment(Image.ALIGN_CENTER);doc.add(img);}
            doc.close();
            File downloads=android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);if(!downloads.exists())downloads.mkdirs();File dest=new File(downloads,fname);outFile.renameTo(dest);
            runOnUiThread(()->{pb.setVisibility(View.GONE);btnConvert.setEnabled(true);new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("✅ Conversion Complete!").setMessage("Saved to Downloads:\n"+fname).setPositiveButton("Open PDF",(d,w)->{startActivity(new Intent(this,PDFViewerActivity.class).putExtra("pdf_path",dest.getAbsolutePath()).putExtra("pdf_name",fname));}).setNegativeButton("OK",null).show();});}
        catch(Exception e){runOnUiThread(()->{pb.setVisibility(View.GONE);btnConvert.setEnabled(true);Toast.makeText(this,"Conversion failed: "+e.getMessage(),Toast.LENGTH_LONG).show();});e.printStackTrace();}});}

    @Override public boolean onOptionsItemSelected(MenuItem item){if(item.getItemId()==android.R.id.home){onBackPressed();return true;}return super.onOptionsItemSelected(item);}
}
