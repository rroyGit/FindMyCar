package com.release.rroycsdev.findmycar;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;

import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends FragmentActivity implements CameraFragment.mCameraInterface, LastImageFragment.mLastImageInterface{

    private static final String TAG =  "MainActivity";
    private static int ERROR_DIALOG_REQUEST = 9001;
    private Context context;

    static FragmentType currentFragmentType = FragmentType.LAST_IMAGE;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameLayout;

    private Fragment lastImageFragment;
    private Fragment cameraFragment;
    private Bundle cameraFragBundle;
    private static final String last_image = "LAST_IMAGE";
    private static final String camera_image = "CAMERA";
    public Bundle savedInst;

    private enum FragmentType{
        LAST_IMAGE, CAMERA
    }

    @Override
    public void hideFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        for (FragmentType fragmentType: FragmentType.values()) {
            if(!fragmentType.name().equals(currentFragmentType.name())){
                Fragment fragment = fragmentManager.findFragmentByTag(fragmentType.name());
                if(fragment != null) {
                    fragmentTransaction.hide(fragment);
                    fragmentTransaction.commit();
                }
            }
        }
    }

    @Override
    public void showFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Fragment fragment = fragmentManager.findFragmentByTag(currentFragmentType.name());
        if(fragment != null) {
            fragmentTransaction.show(fragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;
        cameraFragBundle = new Bundle();

        if (isServicesOK()) {
            MobileAds.initialize(MainActivity.this, getString(R.string.release_mobile_ads_init));
            init();
            navFragmentSelection();
            createOrShowFragments();
        }
    }


    //TODO: fix orientation bug
    private void createOrShowFragments(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        final int fragCount = fragmentManager.getBackStackEntryCount();
        if(fragCount > 0){
            //on orientation change
            String name = fragmentManager.getBackStackEntryAt(fragCount-1).getName();
            setCurrentFragmentTab(FragmentType.valueOf(name));
            hideFragment();
            showFragment();

            switch(currentFragmentType){
                case LAST_IMAGE:
                    bottomNavigationView.getMenu().getItem(FragmentType.LAST_IMAGE.ordinal()).setChecked(true);
                    break;
                case CAMERA:
                    bottomNavigationView.getMenu().getItem(FragmentType.CAMERA.ordinal()).setChecked(true);
                    break;
            }

        }else {

            lastImageFragment = new LastImageFragment();
            pushFragment(lastImageFragment, true);

            Thread thread = new Thread(()->{
                if(checkPermissionToInitiateCameraFragment()){
                    cameraFragment = new CameraFragment();
                    cameraFragBundle.putBoolean("PermWaiting", false);
                    cameraFragment.setArguments(cameraFragBundle);
                    setCurrentFragmentTab(FragmentType.CAMERA);
                    pushFragment(cameraFragment, false);
                    setCurrentFragmentTab(FragmentType.LAST_IMAGE);
                }});
            thread.start();
        }
    }

    private boolean checkPermissionToInitiateCameraFragment(){

        String[] locationPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        String[] writeReadPermissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (context.checkCallingOrSelfPermission(locationPermissions[0]) == PackageManager.PERMISSION_GRANTED) {
            if (context.checkCallingOrSelfPermission(locationPermissions[1]) == PackageManager.PERMISSION_GRANTED) {
                return context.checkCallingOrSelfPermission(writeReadPermissions[0]) == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }


    public void setCurrentFragmentTab(FragmentType fragmentType){
        currentFragmentType =  fragmentType;
    }


    protected int getFragmentContainerResId() {
        return R.id.main_frame;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    private void navFragmentSelection(){
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {

            if(!checkPermissionToInitiateCameraFragment()) return false;

            switch (item.getItemId()){
                case R.id.lastImage:
                    if(currentFragmentType.name().equals(FragmentType.LAST_IMAGE.name())) return false;

                    setCurrentFragmentTab(FragmentType.LAST_IMAGE);
                    if(getSupportFragmentManager().findFragmentByTag(FragmentType.LAST_IMAGE.name()) == null){
                        lastImageFragment = new LastImageFragment();
                    }else{
                        lastImageFragment = getSupportFragmentManager().findFragmentByTag(FragmentType.LAST_IMAGE.name());
                    }
                    pushFragment(lastImageFragment, true);
                    hideFragment();
                    showFragment();
                    return true;
                case R.id.cameraImage:
                    if(currentFragmentType.name().equals(FragmentType.CAMERA.name())) return false;

                    new Handler().postDelayed(()->{
                        setCurrentFragmentTab(FragmentType.CAMERA);
                        if(getSupportFragmentManager().findFragmentByTag(FragmentType.CAMERA.name()) == null){
                            cameraFragBundle.putBoolean("PermWaiting", true);
                            cameraFragment = new CameraFragment();
                            cameraFragment.setArguments(cameraFragBundle);
                        }else{
                            cameraFragment = getSupportFragmentManager().findFragmentByTag(FragmentType.CAMERA.name());
                        }
                        pushFragment(cameraFragment, true);
                        hideFragment();
                        showFragment();
                    },130);
                    return true;
                case R.id.settings:
                    startActivity(new Intent(this, Settings.class));
                    return true;
            }
            return false;
        });
    }

    private void init(){
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        frameLayout = findViewById(R.id.main_frame);
    }

    private boolean isServicesOK(){
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);

        if(available == ConnectionResult.SUCCESS){
            return true;
        }else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(context, "You can't make google maps request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    protected void setStatusBarTranslucent(boolean makeTranslucent) {
        if (makeTranslucent) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    public void pushFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        //ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);


        if(getSupportFragmentManager().findFragmentByTag(currentFragmentType.name()) == null)
            ft.add(getFragmentContainerResId(), fragment, currentFragmentType.name());
        if(addToBackStack) {
            ft.addToBackStack(currentFragmentType.name());
        }else ft.hide(fragment);

        ft.commit();
    }

    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count == 1) {
            finish();
        } else {
            switch (getSupportFragmentManager().getBackStackEntryAt(count - 2).getName()){
                case last_image:
                    bottomNavigationView.getMenu().getItem(FragmentType.LAST_IMAGE.ordinal()).setChecked(true);
                    setCurrentFragmentTab(FragmentType.LAST_IMAGE);
                    break;
                case camera_image:
                    bottomNavigationView.getMenu().getItem(FragmentType.CAMERA.ordinal()).setChecked(true);
                    setCurrentFragmentTab(FragmentType.CAMERA);
                    break;
            }
            hideFragment();
            showFragment();
            getSupportFragmentManager().popBackStack();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentFragmentType.name());

        fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("orientationChange", true);
        savedInst = outState;
    }

    //called only when recreating activity after it was killed by the OS.
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //orientation change detected
        if(savedInstanceState != null) savedInst = savedInstanceState;
    }
}
