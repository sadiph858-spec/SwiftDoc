package com.smartpdf.reader.models;
import java.util.Locale;
public class PDFFile {
    private long id; private String name,path,date; private long size;
    private boolean isFavorite; private long lastOpened; private int lastPage,totalPages;
    public PDFFile(){}
    public PDFFile(String name,String path,String date,long size){this.name=name;this.path=path;this.date=date;this.size=size;}
    public PDFFile(long id,String name,String path,String date,long size,boolean fav,long last){this.id=id;this.name=name;this.path=path;this.date=date;this.size=size;this.isFavorite=fav;this.lastOpened=last;}
    public long getId(){return id;} public String getName(){return name;} public String getPath(){return path;}
    public String getDate(){return date;} public long getSize(){return size;} public boolean isFavorite(){return isFavorite;}
    public long getLastOpened(){return lastOpened;} public int getLastPage(){return lastPage;} public int getTotalPages(){return totalPages;}
    public void setId(long v){id=v;} public void setName(String v){name=v;} public void setPath(String v){path=v;}
    public void setDate(String v){date=v;} public void setSize(long v){size=v;} public void setFavorite(boolean v){isFavorite=v;}
    public void setLastOpened(long v){lastOpened=v;} public void setLastPage(int v){lastPage=v;} public void setTotalPages(int v){totalPages=v;}
    public String getFormattedSize(){if(size<=0)return "0 B";String[]u={"B","KB","MB","GB"};int i=0;double s=size;while(s>=1024&&i<u.length-1){s/=1024;i++;}return String.format(Locale.getDefault(),"%.1f %s",s,u[i]);}
    public int getProgressPercent(){if(totalPages<=0)return 0;return(int)((lastPage+1)*100.0/totalPages);}
    @Override public boolean equals(Object o){return(o instanceof PDFFile)&&path!=null&&path.equals(((PDFFile)o).path);}
    @Override public int hashCode(){return path!=null?path.hashCode():0;}
}
