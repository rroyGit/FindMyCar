package com.release.rroycsdev.findmycar;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class ImageDataClass {
    private String path;
    private String text;
    private Bitmap bitmap;
    private LatLng latLng;
    private Bitmap compressedBitmap;
    private Marker marker;

    ImageDataClass(String path, Bitmap bitmap){
        this.path = path;
        this.bitmap = bitmap;
    }


    public String getPath() {
        return path;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setLatLng(LatLng latLng) { this.latLng = latLng; }

    public LatLng getLatLng() { return latLng; }

    public Bitmap getCompressedBitmap() { return compressedBitmap; }

    public void setCompressedBitmap(Bitmap compressedBitmap) { this.compressedBitmap =
            compressedBitmap; }

    public Marker getMarker() { return marker; }

    public void setMarker(Marker marker) { this.marker = marker; }
}
