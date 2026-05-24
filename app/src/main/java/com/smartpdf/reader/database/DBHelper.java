package com.smartpdf.reader.database;
import android.content.ContentValues; import android.content.Context;
import android.database.Cursor; import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper; import android.util.Log;
import com.smartpdf.reader.models.Bookmark; import com.smartpdf.reader.models.PDFFile;
import java.util.ArrayList; import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String TAG="DBHelper",DB="leafpdf2.db"; private static final int VER=1;
    private static DBHelper inst;
    public static synchronized DBHelper getInstance(Context c){if(inst==null)inst=new DBHelper(c.getApplicationContext());return inst;}
    private DBHelper(Context c){super(c,DB,null,VER);}

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE recent(_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,path TEXT UNIQUE,date TEXT,size INTEGER,last_opened INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE favorites(_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,path TEXT UNIQUE,date TEXT,size INTEGER)");
        db.execSQL("CREATE TABLE bookmarks(_id INTEGER PRIMARY KEY AUTOINCREMENT,pdf_path TEXT,page INTEGER,title TEXT,created_at INTEGER)");
        db.execSQL("CREATE TABLE reading_progress(_id INTEGER PRIMARY KEY AUTOINCREMENT,path TEXT UNIQUE,last_page INTEGER DEFAULT 0,total_pages INTEGER DEFAULT 0,total_time INTEGER DEFAULT 0,last_read INTEGER DEFAULT 0,sessions INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE notes(_id INTEGER PRIMARY KEY AUTOINCREMENT,pdf_path TEXT,page INTEGER,note_text TEXT,created_at INTEGER)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int o,int n){db.execSQL("DROP TABLE IF EXISTS recent");db.execSQL("DROP TABLE IF EXISTS favorites");db.execSQL("DROP TABLE IF EXISTS bookmarks");db.execSQL("DROP TABLE IF EXISTS reading_progress");db.execSQL("DROP TABLE IF EXISTS notes");onCreate(db);}

    // RECENT
    public void addToRecent(PDFFile f){
        if(f==null||f.getPath()==null)return;
        SQLiteDatabase db=getWritableDatabase();
        try{ContentValues v=new ContentValues();v.put("name",f.getName());v.put("path",f.getPath());v.put("date",f.getDate());v.put("size",f.getSize());v.put("last_opened",System.currentTimeMillis());
            db.insertWithOnConflict("recent",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            db.execSQL("DELETE FROM recent WHERE _id NOT IN(SELECT _id FROM recent ORDER BY last_opened DESC LIMIT 50)");}
        catch(Exception e){Log.e(TAG,e.getMessage());}finally{db.close();}}
    public List<PDFFile> getRecentFiles(){List<PDFFile> l=new ArrayList<>();SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.rawQuery("SELECT * FROM recent ORDER BY last_opened DESC",null);
            if(c!=null&&c.moveToFirst())do{l.add(new PDFFile(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4),false,c.getLong(5)));}while(c.moveToNext());}
        catch(Exception e){Log.e(TAG,""+e);}finally{if(c!=null)c.close();db.close();}return l;}
    public void removeFromRecent(String p){SQLiteDatabase db=getWritableDatabase();try{db.delete("recent","path=?",new String[]{p});}finally{db.close();}}
    public void clearRecent(){SQLiteDatabase db=getWritableDatabase();try{db.delete("recent",null,null);}finally{db.close();}}

    // FAVORITES
    public boolean addToFavorites(PDFFile f){if(f==null)return false;SQLiteDatabase db=getWritableDatabase();
        try{ContentValues v=new ContentValues();v.put("name",f.getName());v.put("path",f.getPath());v.put("date",f.getDate());v.put("size",f.getSize());return db.insertWithOnConflict("favorites",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1;}
        catch(Exception e){return false;}finally{db.close();}}
    public boolean removeFromFavorites(String p){SQLiteDatabase db=getWritableDatabase();try{return db.delete("favorites","path=?",new String[]{p})>0;}catch(Exception e){return false;}finally{db.close();}}
    public boolean isFavorite(String p){SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.rawQuery("SELECT _id FROM favorites WHERE path=?",new String[]{p});return c!=null&&c.getCount()>0;}
        catch(Exception e){return false;}finally{if(c!=null)c.close();db.close();}}
    public List<PDFFile> getFavoriteFiles(){List<PDFFile> l=new ArrayList<>();SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.rawQuery("SELECT * FROM favorites ORDER BY name ASC",null);
            if(c!=null&&c.moveToFirst())do{l.add(new PDFFile(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4),true,0));}while(c.moveToNext());}
        catch(Exception e){Log.e(TAG,""+e);}finally{if(c!=null)c.close();db.close();}return l;}

    // BOOKMARKS
    public void addBookmark(String path,int page,String title){SQLiteDatabase db=getWritableDatabase();
        try{ContentValues v=new ContentValues();v.put("pdf_path",path);v.put("page",page);v.put("title",title);v.put("created_at",System.currentTimeMillis());db.insert("bookmarks",null,v);}
        catch(Exception e){Log.e(TAG,""+e);}finally{db.close();}}
    public void removeBookmark(long id){SQLiteDatabase db=getWritableDatabase();try{db.delete("bookmarks","_id=?",new String[]{""+id});}finally{db.close();}}
    public void removeBookmarkByPage(String path,int page){SQLiteDatabase db=getWritableDatabase();try{db.delete("bookmarks","pdf_path=? AND page=?",new String[]{path,""+page});}finally{db.close();}}
    public boolean isBookmarked(String path,int page){SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.rawQuery("SELECT _id FROM bookmarks WHERE pdf_path=? AND page=?",new String[]{path,""+page});return c!=null&&c.getCount()>0;}
        catch(Exception e){return false;}finally{if(c!=null)c.close();db.close();}}
    public List<Bookmark> getBookmarks(String path){List<Bookmark> l=new ArrayList<>();SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.rawQuery("SELECT * FROM bookmarks WHERE pdf_path=? ORDER BY page ASC",new String[]{path});
            if(c!=null&&c.moveToFirst())do{Bookmark b=new Bookmark();b.setId(c.getLong(0));b.setPdfPath(c.getString(1));b.setPage(c.getInt(2));b.setTitle(c.getString(3));b.setCreatedAt(c.getLong(4));l.add(b);}while(c.moveToNext());}
        catch(Exception e){Log.e(TAG,""+e);}finally{if(c!=null)c.close();db.close();}return l;}

    // READING PROGRESS
    public void updateProgress(String path,int lastPage,int totalPages,long sessionMs){
        SQLiteDatabase db=getWritableDatabase();
        try{Cursor c=db.rawQuery("SELECT total_time,sessions FROM reading_progress WHERE path=?",new String[]{path});
            ContentValues v=new ContentValues();v.put("path",path);v.put("last_page",lastPage);v.put("total_pages",totalPages);v.put("last_read",System.currentTimeMillis());
            if(c!=null&&c.moveToFirst()){v.put("total_time",c.getLong(0)+sessionMs);v.put("sessions",c.getInt(1)+1);db.update("reading_progress",v,"path=?",new String[]{path});if(c!=null)c.close();}
            else{if(c!=null)c.close();v.put("total_time",sessionMs);v.put("sessions",1);db.insert("reading_progress",null,v);}}
        catch(Exception e){Log.e(TAG,""+e);}finally{db.close();}}
    public int getLastPage(String path){SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.rawQuery("SELECT last_page FROM reading_progress WHERE path=?",new String[]{path});if(c!=null&&c.moveToFirst())return c.getInt(0);}
        catch(Exception e){Log.e(TAG,""+e);}finally{if(c!=null)c.close();db.close();}return 0;}
    public int getProgressPercent(String path){SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.rawQuery("SELECT last_page,total_pages FROM reading_progress WHERE path=?",new String[]{path});
            if(c!=null&&c.moveToFirst()&&c.getInt(1)>0)return(int)((c.getInt(0)+1)*100.0/c.getInt(1));}
        catch(Exception e){Log.e(TAG,""+e);}finally{if(c!=null)c.close();db.close();}return 0;}
    public long getTotalReadingTime(){SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.rawQuery("SELECT SUM(total_time) FROM reading_progress",null);if(c!=null&&c.moveToFirst())return c.getLong(0);}
        catch(Exception e){Log.e(TAG,""+e);}finally{if(c!=null)c.close();db.close();}return 0;}

    // NOTES
    public void addNote(String path,int page,String text){SQLiteDatabase db=getWritableDatabase();
        try{ContentValues v=new ContentValues();v.put("pdf_path",path);v.put("page",page);v.put("note_text",text);v.put("created_at",System.currentTimeMillis());db.insert("notes",null,v);}
        catch(Exception e){Log.e(TAG,""+e);}finally{db.close();}}
    public void deleteNote(long id){SQLiteDatabase db=getWritableDatabase();try{db.delete("notes","_id=?",new String[]{""+id});}finally{db.close();}}
    public List<android.content.ContentValues> getNotes(String path){List<android.content.ContentValues> l=new ArrayList<>();SQLiteDatabase db=getReadableDatabase();Cursor c=null;
        try{c=db.rawQuery("SELECT * FROM notes WHERE pdf_path=? ORDER BY page ASC",new String[]{path});
            if(c!=null&&c.moveToFirst())do{android.content.ContentValues cv=new android.content.ContentValues();cv.put("_id",c.getLong(0));cv.put("page",c.getInt(2));cv.put("note_text",c.getString(3));l.add(cv);}while(c.moveToNext());}
        catch(Exception e){Log.e(TAG,""+e);}finally{if(c!=null)c.close();db.close();}return l;}
}
