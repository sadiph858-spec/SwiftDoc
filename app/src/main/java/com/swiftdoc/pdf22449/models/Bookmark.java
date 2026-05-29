package com.swiftdoc.pdf22449.models;
public class Bookmark {
    private long id; private String pdfPath; private int page; private String title; private long createdAt;
    public Bookmark(){}
    public Bookmark(String pdfPath,int page,String title){this.pdfPath=pdfPath;this.page=page;this.title=title;this.createdAt=System.currentTimeMillis();}
    public long getId(){return id;} public String getPdfPath(){return pdfPath;} public int getPage(){return page;}
    public String getTitle(){return title;} public long getCreatedAt(){return createdAt;}
    public void setId(long v){id=v;} public void setPdfPath(String v){pdfPath=v;} public void setPage(int v){page=v;}
    public void setTitle(String v){title=v;} public void setCreatedAt(long v){createdAt=v;}
}
