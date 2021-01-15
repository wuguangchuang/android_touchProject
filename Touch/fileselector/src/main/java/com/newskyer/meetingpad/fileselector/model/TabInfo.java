package com.newskyer.meetingpad.fileselector.model;


import androidx.fragment.app.Fragment;

/**
 * @author liziyang
 * @since 2018/1/26
 */
public class TabInfo {
    private String tab;
    private Fragment cls;
    private int resource,selectType, selectItem;


    public TabInfo(String tab, Fragment cls, int resource,int selectType, int selectItem) {
        this.tab = tab;
        this.cls = cls;
        this.resource = resource;
        this.selectType = selectType;
        this.selectItem = selectItem;
    }

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }

    public Fragment getCls() {
        return cls;
    }

    public void setCls(Fragment cls) {
        this.cls = cls;
    }

    public int getResource() {
        return resource;
    }

    public void setResource(int resource) {
        this.resource = resource;
    }

    public int getSelectType() {
        return selectType;
    }
    public int getSelectItem() {
        return selectItem;
    }

    public void setSelectType(int selectType) {
        this.selectType = selectType;
    }
}
