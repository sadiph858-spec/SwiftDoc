package com.swiftdoc.pdf22449.models;
public class DashboardItem {
    private int iconRes; private String title; private String subtitle; private int bgColor; private int action;
    public DashboardItem(int iconRes,String title,String subtitle,int bgColor,int action){
        this.iconRes=iconRes;this.title=title;this.subtitle=subtitle;this.bgColor=bgColor;this.action=action;}
    public int getIconRes(){return iconRes;} public String getTitle(){return title;}
    public String getSubtitle(){return subtitle;} public int getBgColor(){return bgColor;} public int getAction(){return action;}
}
