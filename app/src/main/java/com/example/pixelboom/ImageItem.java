package com.example.pixelboom;

public class ImageItem {
    private String mUrl;

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }



    @Override
    public String toString() {
        return mUrl;
    }
}
