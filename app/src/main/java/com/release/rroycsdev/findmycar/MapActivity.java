package com.release.rroycsdev.findmycar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = MapActivity.class.getName();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15;
    private Context context;

    private Boolean mLocationPermissionGranted = false;
    GoogleMap googleMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private static final int REQUEST_LOAD_IMG = 900;
    private static final int WRITE_PERMISSION_REQUEST_CODE = 901;
    //@Doc: If your app uses the WRITE_EXTERNAL_STORAGE permission, then it implicitly has permission to read the external storage as well.

    private PhotoParser photoParser;
    private LatLng picLatLng;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        setStatusBarTranslucent(true);

        context = MapActivity.this;
        getPermissions();
    }

    private Boolean setMarker(){
        picLatLng = photoParser.getLatLng(null, null, true);

        if(picLatLng != null) {
            //Toast.makeText(context, "Setting marker for " + picLatLng.latitude + " " +picLatLng.longitude, Toast.LENGTH_LONG).show();
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(picLatLng);
            markerOptions.title("Your car is here");
            googleMap.addMarker(markerOptions);
            moveCamera(picLatLng, DEFAULT_ZOOM);
            return  true;
        }

        Toast.makeText(context, "No geo location data found", Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_LOAD_IMG) {

            if (resultCode == RESULT_OK) {
                try {
                    final Uri imageUri = data.getData();
                    if(imageUri != null) {
                        setMarker();
                    } else {
                        Toast.makeText(context, "Unable to get image uri", Toast.LENGTH_LONG).show();
                    }

                    //final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    //Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    //photoParser.setSelectedImage(selectedImage);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Something went wrong", Toast.LENGTH_LONG).show();
                }

            }else{
                Toast.makeText(context, "You haven't picked Image",Toast.LENGTH_LONG).show();
            }
        }

    }


    private Boolean checkLocationServiceOn() {

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        boolean locationServiceOn = false;


        try {
            if (locationManager != null) {
                gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                }
            });

            dialog.setNegativeButton(context.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                }
            });
            dialog.show();
        }else locationServiceOn = true;

        //Toast.makeText(context, "locationServiceOn is " +locationServiceOn, Toast.LENGTH_SHORT).show();
        return locationServiceOn;
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Toast.makeText(context, "Map is Ready", Toast.LENGTH_SHORT).show();
        this.googleMap = googleMap;

        if (mLocationPermissionGranted) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setZoomControlsEnabled(true);

            if(!setMarker()) getDeviceLocation();
        }
    }

    private void getDeviceLocation(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        try{
            if(mLocationPermissionGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Location currentLocation = (Location) task.getResult();
                            if(currentLocation == null) {
                                Toast.makeText(context, "Could not get current location", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);

                        }else{
                            Toast.makeText(context, "Task not successful", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch(SecurityException e){
            Toast.makeText(context, "Security Exception" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void moveCamera(LatLng latLng, float zoom){
        //Toast.makeText(context, "Moving camera", Toast.LENGTH_SHORT).show();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void getPermissions(){
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        //check FINE_LOCATION
        if(context.checkCallingOrSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED){
            //check COARSE_LOCATION
            if(context.checkCallingOrSelfPermission(permissions[1]) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted =  true;
                if(checkLocationServiceOn()) initMap();
                if(context.checkCallingOrSelfPermission(permissions[2]) == PackageManager.PERMISSION_GRANTED){
                    photoParser =  new PhotoParser(context, null);

                }else{
                    ActivityCompat.requestPermissions(this, permissions, WRITE_PERMISSION_REQUEST_CODE);
                }
            }else{
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Toast.makeText(context, "onRequestPermissionResult", Toast.LENGTH_SHORT).show();
        mLocationPermissionGranted = false;
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermissionGranted = false;
                        return;
                    }
                }
                mLocationPermissionGranted = true;
                if(checkLocationServiceOn()) initMap();
            }

            case WRITE_PERMISSION_REQUEST_CODE: {
               if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                   photoParser =  new PhotoParser(context, null);
               }
            }
        }
    }

    protected void setStatusBarTranslucent(boolean makeTranslucent) {
        if (makeTranslucent) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

}