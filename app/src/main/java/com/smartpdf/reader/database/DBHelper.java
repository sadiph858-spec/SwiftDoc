package com.smartpdf.reader.database;
import android.content.ContentValues; import android.content.Context;
import android.database.Cursor; import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper; import android.util.Log;
import com.smartpdf.reader.models.PDFFile;
import java.util.ArrayList; import java.util.List;
public class DBHelper extends SQLiteOpenHelper {
    private static final String TAG="DBHelper",DB="leafpdf.db";
    public static final String TR="recent_files",TF="favorite_files";
    public static final String CI="_id",CN="name",CP="path",CD="date",CS="size",CL="last_opened";
    private static DBHelper inst;
    public static synchronized DBHelper getInstance(Context c){if(inst==null)inst=new DBHelper(c.getApplicationContext());return inst;}
    private DBHelper(Context c){super(c,DB,null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE "+TR+"("+CI+" INTEGER PRIMARY KEY AUTOINCREMENT,"+CN+" TEXT NOT NULL,"+CP+" TEXT NOT NULL UNIQUE,"+CD+" TEXT,"+CS+" INTEGER DEFAULT 0,"+CL+" INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE "+TF+"("+CI+" INTEGER PRIMARY KEY AUTOINCREMENT,"+CN+" TEXT NOT NULL,"+CP+" TEXT NOT NULL UNIQUE,"+CD+" TEXT,"+CS+" INTEGER DEFAULT 0)");}
    @Override public void onUpgrade(SQLiteDatabase db,int o,int n){db.execSQL("DROP TABLE IF EXISTS "+TR);db.execSQL("DROP TABLE IF EXISTS "+TF);onCreate(db);}
    public void addToRecent(PDFFile f){
        if(f==null||f.getPath()==null)return; SQLiteDatabase db=getWritableDatabase();
        try{ContentValues v=new ContentValues();v.put(CN,f.getName());v.put(CP,f.getPath());v.put(CD,f.getDate());v.put(CS,f.getSize());v.put(CL,System.currentTimeMillis());
            db.insertWithOnConflict(TR,null,v,SQLiteDatabase.CONFLICT_REPLACE);
            db.execSQL("DELETE FROM "+TR+" WHERE "+CI+" NOT IN(SELECT "+CI+" FROM "+TR+" ORDER BY "+CL+" DESC LIMIT 50)");}
        catch(Exception e){Log.e(TAG,e.getMessage());}finally{db.close();}}
    public List<PDFFile> getRecentFiles(){
        List<PDFFile> l=new ArrayList<>(); SQLiteDatabase db=getReadableDatabase(); Cursor c=null;
        try{c=db.query(TR,null,null,null,null,null,CL+" DESC");
            if(c!=null&&c.moveToFirst())do{l.add(new PDFFile(c.getLong(c.getColumnIndexOrThrow(CI)),c.getString(c.getColumnIndexOrThrow(CN)),c.getString(c.getColumnIndexOrThrow(CP)),c.getString(c.getColumnIndexOrThrow(CD)),c.getLong(c.getColumnIndexOrThrow(CS)),false,c.getLong(c.getColumnIndexOrThrow(CL))));}while(c.moveToNext());}
        catch(Exception e){Log.e(TAG,e.getMessage());}finally{if(c!=null)c.close();db.close();}return l;}
    public void removeFromRecent(String p){SQLiteDatabase db=getWritableDatabase();try{db.delete(TR,CP+"=?",new String[]{p});}finally{db.close();}}
    public void clearRecent(){SQLiteDatabase db=getWritableDatabase();try{db.delete(TR,null,null);}finally{db.close();}}
    public boolean addToFavorites(PDFFile f){
        if(f==null||f.getPath()==null)return false; SQLiteDatabase db=getWritableDatabase();
        try{ContentValues v=new ContentValues();v.put(CN,f.getName());v.put(CP,f.getPath());v.put(CD,f.getDate());v.put(CS,f.getSize());
            return db.insertWithOnConflict(TF,null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1;}
        catch(Exception e){return false;}finally{db.close();}}
    public boolean removeFromFavorites(String p){SQLiteDatabase db=getWritableDatabase();try{return db.delete(TF,CP+"=?",new String[]{p})>0;}catch(Exception e){return false;}finally{db.close();}}
    public boolean isFavorite(String p){SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.query(TF,new String[]{CI},CP+"=?",new String[]{p},null,null,null);return c!=null&&c.getCount()>0;}
        catch(Exception e){return false;}finally{if(c!=null)c.close();db.close();}}
    public List<PDFFile> getFavoriteFiles(){
        List<PDFFile> l=new ArrayList<>(); SQLiteDatabase db=getReadableDatabase(); Cursor c=null;
        try{c=db.query(TF,null,null,null,null,null,CN+" ASC");
            if(c!=null&&c.moveToFirst())do{l.add(new PDFFile(c.getLong(c.getColumnIndexOrThrow(CI)),c.getString(c.getColumnIndexOrThrow(CN)),c.getString(c.getColumnIndexOrThrow(CP)),c.getString(c.getColumnIndexOrThrow(CD)),c.getLong(c.getColumnIndexOrThrow(CS)),true,0));}while(c.moveToNext());}
        catch(Exception e){Log.e(TAG,e.getMessage());}finally{if(c!=null)c.close();db.close();}return l;}
}
