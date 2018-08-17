package com.release.rroycsdev.findmycar;

import android.arch.lifecycle.LiveData;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface ImageViewModelPresenter {

    void getImageInfoList(List<ImageDataClass> imageDataClassList);
}
