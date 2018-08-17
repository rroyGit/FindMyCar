package com.release.rroycsdev.findmycar;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public interface CameraFragListener {

    void setMarkersOnImageClick(LatLng latLng, Marker marker);
}
