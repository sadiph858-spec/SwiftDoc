package com.smartpdf.reader.activities;
import android.graphics.Bitmap; import android.graphics.pdf.PdfRenderer;
import android.media.MediaScannerConnection; import android.os.AsyncTask; import android.os.Bundle;
import android.os.ParcelFileDescriptor; import android.view.MenuItem; import android.view.View;
import android.widget.EditText; import android.widget.ProgressBar; import android.widget.TextView; import android.widget.Toast;
import androidx.appcompat.app.AlertDialog; import androidx.appcompat.app.AppCompatActivity; import androidx.appcompat.widget.Toolbar;
import com.itextpdf.text.Document; import com.itextpdf.text.PageSize; import com.itextpdf.text.pdf.*;
import com.smartpdf.reader.R;
import java.io.*; import java.text.SimpleDateFormat; import java.util.*; import java.util.Locale;

public class PdfToolsActivity extends AppCompatActivity {
    private ProgressBar pb; private TextView tvStatus; private String tool,pdfPath,pdfName;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s); setContentView(R.layout.activity_pdf_tools);
        tool=getIntent().getStringExtra("tool"); pdfPath=getIntent().getStringExtra("pdf_path"); pdfName=getIntent().getStringExtra("pdf_name");
        Toolbar tb=findViewById(R.id.toolbar_tools); setSupportActionBar(tb);
        if(getSupportActionBar()!=null){getSupportActionBar().setDisplayHomeAsUpEnabled(true);getSupportActionBar().setTitle(getToolTitle());}
        pb=findViewById(R.id.pb_tool); tvStatus=findViewById(R.id.tv_tool_status);
        runTool();}

    private String getToolTitle(){if(tool==null)return "PDF Tools";switch(tool){case "compress":return "Compress PDF";case "split":return "Split PDF";case "pdf2img":return "PDF to Images";case "lock":return "Lock PDF";case "unlock":return "Unlock PDF";case "watermark":return "Add Watermark";case "rotate":return "Rotate Pages";case "txt2pdf":return "Text to PDF";case "merge":return "Merge PDFs";default:return "PDF Tools";}}

    private void runTool(){
        if(tool==null)return;
        switch(tool){
            case "compress": compressPdf(); break;
            case "split": showSplitDialog(); break;
            case "pdf2img": convertPdfToImages(); break;
            case "lock": showLockDialog(); break;
            case "unlock": showUnlockDialog(); break;
            case "watermark": showWatermarkDialog(); break;
            case "rotate": showRotateDialog(); break;
            case "txt2pdf": convertTextToPdf(); break;
            case "merge": Toast.makeText(this,"Pick 2 PDFs to merge",Toast.LENGTH_LONG).show(); break;
            default: Toast.makeText(this,"Tool: "+tool,Toast.LENGTH_SHORT).show();}}

    private void setStatus(String msg){runOnUiThread(()->{tvStatus.setText(msg);tvStatus.setVisibility(View.VISIBLE);});}

    private File getOutputFile(String suffix,String ext){
        File dir=android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
        if(!dir.exists())dir.mkdirs();
        String base=pdfName!=null?pdfName.replace(".pdf",""):"document";
        return new File(dir,base+"_"+suffix+"_"+new SimpleDateFormat("HHmmss",Locale.getDefault()).format(new Date())+"."+ext);}

    // 1. COMPRESS PDF
    private void compressPdf(){
        if(pdfPath==null){Toast.makeText(this,"No PDF selected",Toast.LENGTH_SHORT).show();return;}
        pb.setVisibility(View.VISIBLE); setStatus("Compressing PDF...");
        AsyncTask.execute(()->{try{
            File input=new File(pdfPath); File output=getOutputFile("compressed","pdf");
            PdfReader reader=new PdfReader(pdfPath);
            PdfStamper stamper=new PdfStamper(reader,new FileOutputStream(output));
            stamper.getWriter().setCompressionLevel(9); stamper.setFullCompression();
            int pages=reader.getNumberOfPages();
            for(int i=1;i<=pages;i++){PdfDictionary page=reader.getPageN(i);PdfObject contents=page.get(PdfName.CONTENTS);if(contents!=null){}}
            stamper.close(); reader.close();
            long origSize=input.length(); long newSize=output.length();
            int savings=(int)((origSize-newSize)*100/origSize);
            runOnUiThread(()->{pb.setVisibility(View.GONE);new AlertDialog.Builder(this).setTitle("✅ Compressed!").setMessage("Original: "+formatSize(origSize)+"\nCompressed: "+formatSize(newSize)+"\nSaved: "+Math.max(0,savings)+"%\n\nSaved to Downloads").setPositiveButton("Open",(d,w)->openResult(output)).setNegativeButton("OK",null).show();});}
        catch(Exception e){runOnUiThread(()->{pb.setVisibility(View.GONE);Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();});e.printStackTrace();}});}

    // 2. SPLIT PDF
    private void showSplitDialog(){EditText et=new EditText(this);et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);et.setHint("Split after page number");
        new AlertDialog.Builder(this).setTitle("Split PDF").setMessage("Enter page number to split at:").setView(et)
            .setPositiveButton("Split",(d,w)->{try{int pg=Integer.parseInt(et.getText().toString());splitPdf(pg);}catch(Exception e){Toast.makeText(this,"Invalid number",Toast.LENGTH_SHORT).show();}}).setNegativeButton("Cancel",(d,w)->finish()).show();}

    private void splitPdf(int splitPage){
        if(pdfPath==null)return; pb.setVisibility(View.VISIBLE); setStatus("Splitting PDF...");
        AsyncTask.execute(()->{try{
            PdfReader reader=new PdfReader(pdfPath); int total=reader.getNumberOfPages();
            if(splitPage<1||splitPage>=total){runOnUiThread(()->{Toast.makeText(this,"Invalid split page (1-"+(total-1)+")",Toast.LENGTH_SHORT).show();pb.setVisibility(View.GONE);});reader.close();return;}
            File p1=getOutputFile("part1","pdf"); File p2=getOutputFile("part2","pdf");
            Document d1=new Document(); PdfCopy c1=new PdfCopy(d1,new FileOutputStream(p1)); d1.open();
            for(int i=1;i<=splitPage;i++)c1.addPage(c1.getImportedPage(reader,i)); d1.close();
            Document d2=new Document(); PdfCopy c2=new PdfCopy(d2,new FileOutputStream(p2)); d2.open();
            for(int i=splitPage+1;i<=total;i++)c2.addPage(c2.getImportedPage(reader,i)); d2.close();
            reader.close();
            runOnUiThread(()->{pb.setVisibility(View.GONE);new AlertDialog.Builder(this).setTitle("✅ Split Complete!").setMessage("Part 1: Pages 1-"+splitPage+"\nPart 2: Pages "+(splitPage+1)+"-"+total+"\n\nBoth saved to Downloads").setPositiveButton("OK",null).show();});}
        catch(Exception e){runOnUiThread(()->{pb.setVisibility(View.GONE);Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();});e.printStackTrace();}});}

    // 3. PDF TO IMAGES
    private void convertPdfToImages(){
        if(pdfPath==null){Toast.makeText(this,"No PDF selected",Toast.LENGTH_SHORT).show();return;}
        pb.setVisibility(View.VISIBLE); setStatus("Converting pages to images...");
        AsyncTask.execute(()->{try{
            File imgDir=new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),"LeafPDF");
            if(!imgDir.exists())imgDir.mkdirs();
            ParcelFileDescriptor pfd=ParcelFileDescriptor.open(new File(pdfPath),ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer=new PdfRenderer(pfd); int count=renderer.getPageCount();
            List<String> saved=new ArrayList<>();
            for(int i=0;i<count;i++){
                PdfRenderer.Page page=renderer.openPage(i);
                int w=page.getWidth()*2; int h=page.getHeight()*2;
                Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas=new android.graphics.Canvas(bmp);
                canvas.drawColor(android.graphics.Color.WHITE);
                page.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                File imgFile=new File(imgDir,"page_"+(i+1)+".jpg");
                FileOutputStream fos=new FileOutputStream(imgFile);
                bmp.compress(Bitmap.CompressFormat.JPEG,95,fos); fos.close(); bmp.recycle();
                saved.add(imgFile.getAbsolutePath());
                MediaScannerConnection.scanFile(this,new String[]{imgFile.getAbsolutePath()},null,null);}
            renderer.close(); pfd.close();
            runOnUiThread(()->{pb.setVisibility(View.GONE);new AlertDialog.Builder(this).setTitle("✅ Export Complete!").setMessage(count+" images saved to:\nPictures/LeafPDF/").setPositiveButton("OK",null).show();});}
        catch(Exception e){runOnUiThread(()->{pb.setVisibility(View.GONE);Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();});e.printStackTrace();}});}

    // 4. LOCK PDF
    private void showLockDialog(){EditText et=new EditText(this);et.setHint("Enter password");et.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("Lock PDF").setMessage("Set a password for this PDF:").setView(et)
            .setPositiveButton("Lock",(d,w)->{String pwd=et.getText().toString().trim();if(pwd.isEmpty()){Toast.makeText(this,"Enter password",Toast.LENGTH_SHORT).show();return;}lockPdf(pwd);}).setNegativeButton("Cancel",(d,w)->finish()).show();}

    private void lockPdf(String pwd){
        if(pdfPath==null)return; pb.setVisibility(View.VISIBLE); setStatus("Locking PDF...");
        AsyncTask.execute(()->{try{
            File output=getOutputFile("locked","pdf");
            PdfReader reader=new PdfReader(pdfPath);
            PdfEncryptor.encrypt(reader,new FileOutputStream(output),true,pwd,null,PdfWriter.ALLOW_PRINTING|PdfWriter.ALLOW_COPY);
            reader.close();
            runOnUiThread(()->{pb.setVisibility(View.GONE);new AlertDialog.Builder(this).setTitle("✅ PDF Locked!").setMessage("Password: "+pwd+"\nSaved to Downloads\n\n⚠️ Remember your password!").setPositiveButton("OK",null).show();});}
        catch(Exception e){runOnUiThread(()->{pb.setVisibility(View.GONE);Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();});e.printStackTrace();}});}

    // 5. UNLOCK PDF
    private void showUnlockDialog(){EditText et=new EditText(this);et.setHint("Enter current password");et.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("Unlock PDF").setView(et)
            .setPositiveButton("Unlock",(d,w)->{String pwd=et.getText().toString();unlockPdf(pwd);}).setNegativeButton("Cancel",(d,w)->finish()).show();}

    private void unlockPdf(String pwd){
        if(pdfPath==null)return; pb.setVisibility(View.VISIBLE); setStatus("Unlocking PDF...");
        AsyncTask.execute(()->{try{
            File output=getOutputFile("unlocked","pdf");
            PdfReader reader=new PdfReader(pdfPath,pwd.getBytes());
            PdfEncryptor.encrypt(reader,new FileOutputStream(output),true,null,null,PdfWriter.ALLOW_PRINTING|PdfWriter.ALLOW_COPY|PdfWriter.ALLOW_MODIFY_CONTENTS);
            reader.close();
            runOnUiThread(()->{pb.setVisibility(View.GONE);new AlertDialog.Builder(this).setTitle("✅ PDF Unlocked!").setMessage("Saved to Downloads").setPositiveButton("Open",(d,w)->openResult(output)).setNegativeButton("OK",null).show();});}
        catch(Exception e){runOnUiThread(()->{pb.setVisibility(View.GONE);Toast.makeText(this,"Wrong password or error: "+e.getMessage(),Toast.LENGTH_LONG).show();});e.printStackTrace();}});}

    // 6. WATERMARK
    private void showWatermarkDialog(){EditText et=new EditText(this);et.setHint("Watermark text (e.g. CONFIDENTIAL)");et.setText("CONFIDENTIAL");
        new AlertDialog.Builder(this).setTitle("Add Watermark").setView(et)
            .setPositiveButton("Add",(d,w)->{String txt=et.getText().toString().trim();if(!txt.isEmpty())addWatermark(txt);}).setNegativeButton("Cancel",(d,w)->finish()).show();}

    private void addWatermark(String text){
        if(pdfPath==null)return; pb.setVisibility(View.VISIBLE); setStatus("Adding watermark...");
        AsyncTask.execute(()->{try{
            File output=getOutputFile("watermarked","pdf");
            PdfReader reader=new PdfReader(pdfPath);
            PdfStamper stamper=new PdfStamper(reader,new FileOutputStream(output));
            int pages=reader.getNumberOfPages();
            for(int i=1;i<=pages;i++){
                PdfContentByte canvas=stamper.getUnderContent(i);
                canvas.saveState();
                com.itextpdf.text.pdf.PdfGState gs=new com.itextpdf.text.pdf.PdfGState();
                gs.setFillOpacity(0.3f); canvas.setGState(gs);
                canvas.setColorFill(com.itextpdf.text.BaseColor.GRAY);
                canvas.beginText();
                canvas.setFontAndSize(BaseFont.createFont(),48);
                com.itextpdf.text.Rectangle rect=reader.getPageSizeWithRotation(i);
                canvas.showTextAligned(com.itextpdf.text.Element.ALIGN_CENTER,text,rect.getWidth()/2,rect.getHeight()/2,45);
                canvas.endText(); canvas.restoreState();}
            stamper.close(); reader.close();
            runOnUiThread(()->{pb.setVisibility(View.GONE);new AlertDialog.Builder(this).setTitle("✅ Watermark Added!").setMessage("Saved to Downloads").setPositiveButton("Open",(d,w)->openResult(output)).setNegativeButton("OK",null).show();});}
        catch(Exception e){runOnUiThread(()->{pb.setVisibility(View.GONE);Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();});e.printStackTrace();}});}

    // 7. ROTATE
    private void showRotateDialog(){String[]opts={"Rotate 90° Clockwise","Rotate 90° Counter-Clockwise","Rotate 180°"};
        new AlertDialog.Builder(this).setTitle("Rotate Pages").setItems(opts,(d,w)->{int deg=w==0?90:(w==1?270:180);rotatePdf(deg);}).setNegativeButton("Cancel",(d,w)->finish()).show();}

    private void rotatePdf(int degrees){
        if(pdfPath==null)return; pb.setVisibility(View.VISIBLE); setStatus("Rotating pages...");
        AsyncTask.execute(()->{try{
            File output=getOutputFile("rotated","pdf");
            PdfReader reader=new PdfReader(pdfPath);
            PdfStamper stamper=new PdfStamper(reader,new FileOutputStream(output));
            int pages=reader.getNumberOfPages();
            for(int i=1;i<=pages;i++){PdfDictionary dict=reader.getPageN(i);int rot=reader.getPageRotation(i);dict.put(PdfName.ROTATE,new PdfNumber((rot+degrees)%360));}
            stamper.close(); reader.close();
            runOnUiThread(()->{pb.setVisibility(View.GONE);new AlertDialog.Builder(this).setTitle("✅ Rotated!").setMessage("All pages rotated "+degrees+"°\nSaved to Downloads").setPositiveButton("Open",(d,w)->openResult(output)).setNegativeButton("OK",null).show();});}
        catch(Exception e){runOnUiThread(()->{pb.setVisibility(View.GONE);Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();});e.printStackTrace();}});}

    // 8. TEXT TO PDF
    private void convertTextToPdf(){
        String text=getIntent().getStringExtra("text");
        if(text==null||text.isEmpty()){Toast.makeText(this,"No text provided",Toast.LENGTH_SHORT).show();finish();return;}
        pb.setVisibility(View.VISIBLE); setStatus("Creating PDF from text...");
        AsyncTask.execute(()->{try{
            File output=getOutputFile("text","pdf");
            Document doc=new Document(PageSize.A4,50,50,50,50);
            PdfWriter.getInstance(doc,new FileOutputStream(output)); doc.open();
            com.itextpdf.text.Font font=new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,12);
            com.itextpdf.text.Paragraph para=new com.itextpdf.text.Paragraph(text,font);
            para.setLeading(20); doc.add(para); doc.close();
            runOnUiThread(()->{pb.setVisibility(View.GONE);new AlertDialog.Builder(this).setTitle("✅ PDF Created!").setMessage("Saved to Downloads").setPositiveButton("Open",(d,w)->openResult(output)).setNegativeButton("OK",null).show();});}
        catch(Exception e){runOnUiThread(()->{pb.setVisibility(View.GONE);Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();});e.printStackTrace();}});}

    private void openResult(File f){startActivity(new android.content.Intent(this,PDFViewerActivity.class).putExtra("pdf_path",f.getAbsolutePath()).putExtra("pdf_name",f.getName()));}
    private String formatSize(long bytes){if(bytes<1024)return bytes+" B";if(bytes<1024*1024)return String.format(Locale.getDefault(),"%.1f KB",bytes/1024.0);return String.format(Locale.getDefault(),"%.1f MB",bytes/1024.0/1024.0);}
    @Override public boolean onOptionsItemSelected(MenuItem item){if(item.getItemId()==android.R.id.home){onBackPressed();return true;}return super.onOptionsItemSelected(item);}
}
