package com.example.recordsound;

import android.graphics.drawable.Drawable;

public class RecordItem {

    private Drawable itemIcon;
    private Long id;
    private String displayName;
    private String addDate;
    private String size;

    public RecordItem(Drawable itemIcon, Long id, String displayName, String addDate, String size){
        this.itemIcon = itemIcon;
        this.id = id;
        this.displayName = displayName;
        this.addDate = addDate;
        this.size = size;
    }

    public void setItemIcon(Drawable itemIcon) {
        this.itemIcon = itemIcon;
    }

    public void setId(Long id){
        this.id = id;
    }

    public void setDisplayName(String displayName){
        this.displayName = displayName;
    }

    public void setAddDate(String addDate){
        this.addDate = addDate;
    }

    public void setSize(String size){
        this.size = size;
    }

    public Drawable getItemIcon(){
        return this.itemIcon;
    }

    public Long getId(){
        return this.id;
    }

    public String getDisplayName(){
        return this.displayName;
    }

    public String getAddDate(){
        return this.addDate;
    }

    public String getSize(){
        return this.size;
    }
}
