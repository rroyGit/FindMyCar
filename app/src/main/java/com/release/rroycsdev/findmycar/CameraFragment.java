package com.release.rroycsdev.findmycar;


import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment implements OnMapReadyCallback, CameraFragListener {
    private String TAG = CameraFragment.class.getName();
    private AdView adView;
    private AdRequest adRequest;
    private View rootView;
    private Boolean wakingFromPause = false;
    private Context context;


    private ImageOCRAdapter postsAdapter;
    private ImageOCRViewModel imageOCRViewModel;
    private List<ImageDataClass> mImageDataList;
    private RecyclerView recyclerView;
    private MapView mMapView;
    private GoogleMap googleMap;
    private CameraFragComponent cameraFragComponent;
    private boolean startingOCRProcess = false;

    private static final int OCR_CAPTURE_MAIN = 9123;


    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getContext();

        new Thread(() -> {
            adRequest = new AdRequest.Builder().build();
            //adRequest = new AdRequest.Builder().addTestDevice(getString(R.string.pixel_test_id)).build();
            //adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        }).start();

        boolean permWaiting;
        if (getArguments() != null) {
            permWaiting = getArguments().getBoolean("PermWaiting");
            if(permWaiting && getSavedCarImageText().isEmpty()) {
                Intent intent = new Intent(context, OcrCaptureMainActivity.class);
                startActivityForResult(intent, OCR_CAPTURE_MAIN);
                getArguments().clear();
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        adView = rootView.findViewById(R.id.adView);
        recyclerView = rootView.findViewById(R.id.imageRecyclerView);
        if (adRequest != null) { adView.loadAd(adRequest); }

        cameraFragComponent = DaggerCameraFragComponent.builder().
                contextModule(new ContextModule(context)).build();
        if(!getSavedCarImageText().isEmpty()) initViewModel();

        mMapView = rootView.findViewById(R.id.mapView2);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();
        try {
            MapsInitializer.initialize(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMapView.getMapAsync(this);

        mImageDataList = new ArrayList<>();
        postsAdapter = new ImageOCRAdapter(mImageDataList, context, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(postsAdapter);

        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        return rootView;
    }

    private void initViewModel(){
        startingOCRProcess = true;

        ImageOCRViewModelFactory factory = new ImageOCRViewModelFactory(cameraFragComponent.getContext());
        imageOCRViewModel = ViewModelProviders.of(this, factory).get(ImageOCRViewModel.class);

        imageOCRViewModel.getOCRImagesDataClass().observe(this, new Observer<List<ImageDataClass>>() {
            @Override
            public void onChanged(@Nullable List<ImageDataClass> imageDataClassList) {
                startingOCRProcess = false;
                if (imageDataClassList != null && isVisible()) {
                    String toastMsg = "Found "+imageDataClassList.size()+ " image(s) with similar text" ;
                    Toast toast = Toast.makeText(context, toastMsg, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 300);
                    toast.show();
                }

                boolean zoomToFirstPicOnly = true;
                if (imageDataClassList != null) {
                    for (ImageDataClass item : imageDataClassList) {
                        MarkerOptions markerOptions = new MarkerOptions().position(item.getLatLng());
                        markerOptions.title(item.getPath().substring(item.getPath().indexOf("Camera/") + "Camera/".length()));
                        if (googleMap != null) {
                            Marker marker = googleMap.addMarker(markerOptions);
                            item.setMarker(marker);
                            if (zoomToFirstPicOnly) {
                                moveCamera(item.getLatLng(), 9);
                                zoomToFirstPicOnly = false;
                                marker.showInfoWindow();
                            }
                        }
                    }
                }
                postsAdapter.setData(imageDataClassList);
            }
        });
    }

    private void moveCamera(LatLng latLng, float zoom) {
        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(zoom).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }


    private String getSavedCarImageText(){
        SharedPreferences sharedPreferences = context.getSharedPreferences("CarImageText", Context.MODE_PRIVATE);
        return sharedPreferences.getString("myText", "");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if(hidden){
            if(adView != null) adView.setVisibility(View.GONE);
        }else{
            if(startingOCRProcess) {
                String toastMsg = "Processing...";
                Toast toast = Toast.makeText(context, toastMsg, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 300);
                toast.show();
            }
            if(adView != null) adView.setVisibility(View.VISIBLE);
            if(getSavedCarImageText().isEmpty()) {
                Intent intent = new Intent(context, OcrCaptureMainActivity.class);
                startActivityForResult(intent, OCR_CAPTURE_MAIN);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        wakingFromPause = true;
        if(adView != null) adView.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
       if(wakingFromPause){
           wakingFromPause = false;
           if(adView != null) adView.resume();
       }
    }

    @Override
    public void onDestroyView() {
        if(adView != null) {
            ((ViewGroup) rootView.getParent()).removeAllViews();
            adView.removeAllViews();
            super.onDestroyView();
        }
    }

    @Override
    public void onDestroy() {

        if (getActivity() != null) {
            boolean orientationChange = false;

            if(((MainActivity)(getActivity())).savedInst!= null)
               orientationChange = ((MainActivity)(getActivity())).savedInst.getBoolean("orientationChange");

            //don't destroy threads on orientation change - only when app is exiting
            if (!orientationChange) {
                if (imageOCRViewModel != null) imageOCRViewModel.callShutdownActiveWorkers();
            }
            //after second onDestroy call, w/ no orientation change/app exiting, shutdown active threads
            //so set @orientationChange to false
            ((MainActivity) Objects.
                    requireNonNull(getActivity())).savedInst.putBoolean("orientationChange", false);
        }
        if(adView != null && !adView.isAttachedToWindow()) adView.destroy();
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Toast.makeText(context, "OnAttach Camera",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        adView = null;
        //Toast.makeText(getActivity(), "OnDetach Camera",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.getUiSettings().setCompassEnabled(false);
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            if(imageOCRViewModel != null) imageOCRViewModel.setLiveDataClassListExplicitly();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == OCR_CAPTURE_MAIN){
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    initViewModel();
                    String text = data.getStringExtra("String");
                    Toast toast = Toast.makeText(context, "Getting photos with text: "+ text, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void setMarkersOnImageClick(LatLng latLng, Marker marker) {
        CameraPosition cameraPosition = new CameraPosition.Builder().
                target(latLng).zoom(15).build();
        marker.showInfoWindow();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }

    public interface mCameraInterface{
         void hideFragment();
         void showFragment();
    }
}
