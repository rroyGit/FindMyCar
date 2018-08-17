package com.release.rroycsdev.findmycar;


import android.Manifest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;


/**
 * A simple {@link Fragment} subclass.
 */
public class LastImageFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener,
        View.OnLongClickListener {

    private static final String TAG = LastImageFragment.class.getName();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int LOCATION_SERVICE_SETTINGS_REQUEST_CODE = 901;
    private static final float DEFAULT_ZOOM = 15;
    private static final float DEFAULT_ZOOM_CLOSEUP = 17;
    private static final int TOAST_VERTICAL_OFFSET = 600;
    private Context context;
    private boolean allViewsSet = false;

    public Boolean mLocationPermissionGranted = false;
    private Boolean mReadWritePermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private static final int REQUEST_LOAD_IMG = 900;
    private static final int WRITE_PERMISSION_REQUEST_CODE = 901;
    //@Doc: If your app uses the WRITE_EXTERNAL_STORAGE permission, then it implicitly has
    // permission to read the external storage as well.

    private PhotoParser photoParser;

    private MapView mMapView;
    private GoogleMap googleMap;
    private ImageButton currentLocationImage, currentCarImage, drivingModeImage, walkingModeImage;
    private View rootView;
    private Bundle savedInstanceState = null;
    private Boolean onWakingFromPaused = false;
    private AlertDialog mDialog;

    private LatLng currentLocationLatLng, carLocationLatLng;
    private LastImageFragment lastImageFragmentRef;

    private Handler myUIHandler = new Handler();

    private enum TravelMode{
        DRIVING, WALKING
    }

    TravelMode currentMode = TravelMode.DRIVING;

    public LastImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this.getContext();
        lastImageFragmentRef = this;
        getPermissions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_last_image, container, false);
        return rootView;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(mLocationPermissionGranted && mReadWritePermissionGranted) initViews(view,
                savedInstanceState);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        photoParser = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        onWakingFromPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(allViewsSet && !isHidden() && onWakingFromPaused &&
                mReadWritePermissionGranted && mLocationPermissionGranted) {
            onWakingFromPaused = false;
            clearAllIconsAfterResumeShown();
            Runnable runnable = ()->{if (checkLocationServiceOn() && !setCarLocationMarker())
                moveCameraToCurrentLocation();};
            new Thread(runnable).start();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Toast.makeText(getActivity(), "OnAttach LastImage",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //Toast.makeText(getActivity(), "OnDetach LastImage",Toast.LENGTH_LONG).show();
    }

    private void initViews(View rootView, Bundle savedInstanceState) {
        currentLocationImage = rootView.findViewById(R.id.imgMyLocation);
        currentCarImage = rootView.findViewById(R.id.imgMyCarLocation);
        drivingModeImage = rootView.findViewById(R.id.imgMyCarDriving);
        walkingModeImage = rootView.findViewById(R.id.imgMyCarWalking);


        currentLocationImage.setOnLongClickListener(this);
        currentCarImage.setOnLongClickListener(this);
        drivingModeImage.setOnLongClickListener(this);
        walkingModeImage.setOnLongClickListener(this);

        currentLocationImage.setOnClickListener(this);
        currentCarImage.setOnClickListener(this);
        walkingModeImage.setOnClickListener(this);
        drivingModeImage.setOnClickListener(this);


        mMapView = rootView.findViewById(R.id.mapView);

        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();
        try {
            MapsInitializer.initialize(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMapView.getMapAsync(this);
        allViewsSet = true;
    }

    @Override
    public boolean onLongClick(View view) {
        Toast toast = null;
        switch (view.getId()) {
            case R.id.imgMyLocation:
                toast = Toast.makeText(context,
                        getResources().getString(R.string.currentLocationTip), Toast.LENGTH_SHORT);
                break;

            case R.id.imgMyCarLocation:
                toast = Toast.makeText(context,
                        getResources().getString(R.string.carLocationTip), Toast.LENGTH_SHORT);
                break;

            case R.id.imgMyCarDriving:
                toast = Toast.makeText(context,
                        getResources().getString(R.string.drivingTip), Toast.LENGTH_SHORT);
                break;

            case R.id.imgMyCarWalking:
                toast = Toast.makeText(context,
                        getResources().getString(R.string.walkingTip), Toast.LENGTH_SHORT);
                break;
        }

        if(toast != null){
            View view1 = toast.getView();
            TextView text = (TextView) view1.findViewById(android.R.id.message);
            text.setTextColor(Color.WHITE);
            view1.setBackgroundResource(R.drawable.toast_background);
            toast.setGravity(Gravity.CENTER, 0, TOAST_VERTICAL_OFFSET);
            toast.show();

            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imgMyLocation:
                currentLocationImage.setActivated(true);
                currentCarImage.setActivated(false);
                drivingModeImage.setActivated(false);
                walkingModeImage.setActivated(false);
                moveCameraToCurrentLocation();
                break;

            case R.id.imgMyCarLocation:
                if (carLocationLatLng != null &&
                        carLocationLatLng.latitude != 0 && carLocationLatLng.longitude !=0){
                    currentCarImage.setActivated(true);
                    currentLocationImage.setActivated(false);
                    drivingModeImage.setActivated(false);
                    walkingModeImage.setActivated(false);
                    moveCameraToCarLocation();
                }
                break;

            case R.id.imgMyCarDriving:
                if (carLocationLatLng != null &&
                        carLocationLatLng.latitude != 0 && carLocationLatLng.longitude !=0){
                    drivingModeImage.setActivated(true);
                    currentCarImage.setActivated(false);
                    currentLocationImage.setActivated(false);
                    walkingModeImage.setActivated(false);

                    currentMode = TravelMode.DRIVING;
                    googleMap.clear();

                    getLocationShowDirections(false);
                }
                break;

            case R.id.imgMyCarWalking:
                if (carLocationLatLng != null &&
                        carLocationLatLng.latitude != 0 && carLocationLatLng.longitude !=0){
                    walkingModeImage.setActivated(true);
                    drivingModeImage.setActivated(false);
                    currentCarImage.setActivated(false);
                    currentLocationImage.setActivated(false);

                    currentMode = TravelMode.WALKING;
                    googleMap.clear();
                    getLocationShowDirections(false);
                }
                break;
        }
    }

    public interface mLastImageInterface {
        void hideFragment();
        void showFragment();
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

            new Handler().postDelayed(() -> new Thread(() -> {
                if (!setCarLocationMarker()) moveCameraToCurrentLocation();
            }).start(),200);
        }
    }

    private void getLocationShowDirections(final Boolean moveCameraToLocationOnly){

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        try {
            if (mLocationPermissionGranted) {
                final Task<Location> location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Location currentLocation = task.getResult();
                        if (currentLocation == null) {
                            Toast toast = Toast.makeText(context,
                                    getResources().getString(R.string.location_failure),
                                    Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER_HORIZONTAL,0,
                                    TOAST_VERTICAL_OFFSET);
                            toast.show();
                            checkLocationServiceOn();
                            return;
                        }
                        currentLocationLatLng = new LatLng(currentLocation.getLatitude(),
                                currentLocation.getLongitude());
                        if(moveCameraToLocationOnly) moveCamera(currentLocationLatLng,
                                DEFAULT_ZOOM_CLOSEUP);
                        else new GetDirectionsData(lastImageFragmentRef).
                                execute(getDirectionsUrl());
                    }
                });
            }
        } catch (SecurityException e) {
            Toast.makeText(context, "Security Exception" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void clearAllIconsAfterResumeShown(){
        currentCarImage.setActivated(false);
        currentLocationImage.setActivated(false);
        drivingModeImage.setActivated(false);
        walkingModeImage.setActivated(false);
    }

    private void moveCameraToCurrentLocation() {
        getLocationShowDirections(true);
    }

    private void moveCameraToCarLocation(){
        moveCamera(carLocationLatLng, DEFAULT_ZOOM_CLOSEUP);
    }

    private void moveCamera(LatLng latLng, float zoom) {
        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(zoom).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void getPermissions() {
        assert getActivity() != null;

        String[] locationPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        String[] writeReadPermissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        //check FINE_LOCATION
        if (context.checkCallingOrSelfPermission(locationPermissions[0]) ==
                PackageManager.PERMISSION_GRANTED) {
            //check COARSE_LOCATION
            if (context.checkCallingOrSelfPermission(locationPermissions[1]) ==
                    PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                if (context.checkCallingOrSelfPermission(writeReadPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED) {
                    mReadWritePermissionGranted = true;

                    new Thread(() -> {
                        checkLocationServiceOn();
                        photoParser = new PhotoParser(context);
                    }).start();

                } else {
                    ActivityCompat.requestPermissions(getActivity(), writeReadPermissions,
                            WRITE_PERMISSION_REQUEST_CODE);
                }
            } else {
                ActivityCompat.requestPermissions(getActivity(), locationPermissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(), locationPermissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                mLocationPermissionGranted = checkPermissionResults(grantResults);
            }

            case WRITE_PERMISSION_REQUEST_CODE: {
                mReadWritePermissionGranted = checkPermissionResults(grantResults);

            }
        }

        if (mLocationPermissionGranted && mReadWritePermissionGranted) {
            if(rootView != null) {
                initViews(rootView, savedInstanceState);
                photoParser = new PhotoParser(context);
            }

            new Thread(this::checkLocationServiceOn).start();

        } else {
            Toast toast = Toast.makeText(context,
                    getString(R.string.permissions_denied), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, TOAST_VERTICAL_OFFSET);
            toast.show();
        }
    }

    private Boolean checkPermissionResults(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private Boolean setCarLocationMarker() {
        carLocationLatLng = photoParser.getLatLng(null, null, true);

        if (carLocationLatLng != null) {

            if(carLocationLatLng.latitude ==0 && carLocationLatLng.longitude ==0) {
                myUIHandler.post(() -> {
                    Toast toast = Toast.makeText(context,
                            getString(R.string.geo_location_not_found_error), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, TOAST_VERTICAL_OFFSET);
                    toast.show();
                });

                return false;
            }

            final MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(carLocationLatLng);
            if(!isDetached())markerOptions.title(getString(R.string.car_marker_title));

            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                googleMap.clear();
                googleMap.addMarker(markerOptions).showInfoWindow();
                moveCamera(carLocationLatLng, DEFAULT_ZOOM);
            });

            return true;
        }else{

            myUIHandler.post(() -> {
                Toast toast = Toast.makeText(context,
                        getString(R.string.image_found_error), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, TOAST_VERTICAL_OFFSET);
                toast.show();
            });
            return false;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && rootView != null) {
            clearAllIconsAfterResumeShown();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.
                    ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.
                            ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_GRANTED ) {

                Runnable runnable = ()->{if (checkLocationServiceOn() && !setCarLocationMarker())
                    moveCameraToCurrentLocation();};
                new Thread(runnable).start();
            }
        }
    }

    private Boolean checkLocationServiceOn() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.
                LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        boolean locationServiceOn = false;


        try {
            if (locationManager != null) {
                gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                network_enabled = locationManager.isProviderEnabled(LocationManager.
                        NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!gps_enabled && !network_enabled) {
            // notify user


            AlertDialog.Builder dialog = new AlertDialog.Builder(context);

            dialog.setTitle(context.getResources().getString(R.string.gps_network_not_enabled));

            LayoutInflater layoutInflater = LayoutInflater.from(context);
            final View view = layoutInflater.inflate(R.layout.alert_dialog_image, (ViewGroup)
                    rootView, false);
            dialog.setView(view);

            dialog.setPositiveButton(context.getString(R.string.open_location_settings),
                    (paramDialogInterface, paramInt) -> {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(myIntent, LOCATION_SERVICE_SETTINGS_REQUEST_CODE);
            });

            dialog.setNegativeButton(getActivity().getString(R.string.Cancel),
                    (paramDialogInterface, paramInt) -> {
                // TODO Auto-generated method stub
            });
            if(mDialog == null) {
                myUIHandler.post(()->{
                    mDialog = dialog.create();
                    mDialog.show();
                });

            }else if(!mDialog.isShowing()){
                myUIHandler.post(()->{
                    mDialog = dialog.create();
                    mDialog.show();
                });
            }

        }else locationServiceOn = true;
        return locationServiceOn;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
       if(requestCode == LOCATION_SERVICE_SETTINGS_REQUEST_CODE){
           if(checkLocationServiceOn()) {
               Toast toast = Toast.makeText(context, "You have enabled GPS", Toast.LENGTH_LONG);
               toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, TOAST_VERTICAL_OFFSET);
               toast.show();
           }else{
               Toast toast = Toast.makeText(context, "You have not enabled GPS",
                       Toast.LENGTH_LONG);
               toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, TOAST_VERTICAL_OFFSET);
               toast.show();
           }
       }
    }

    private static class GetDirectionsData extends AsyncTask<String, Void, List<Route>>{
        private WeakReference<LastImageFragment> activityReference;

        GetDirectionsData(LastImageFragment context){
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            activityReference.get().myUIHandler.post(() -> {
                Toast toast = Toast.makeText(activityReference.get().context, activityReference.
                        get().getResources().getString(R.string.directionsMsg), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, TOAST_VERTICAL_OFFSET);
                toast.show();
            });
        }

        @Override
        protected List<Route> doInBackground(String... strings) {
            String link = strings[0];
            try {
                URL url = new URL(link);
                InputStream inputStream = url.openConnection().getInputStream();
                StringBuffer stringBuffer = new StringBuffer();
                BufferedReader bufferedReader =  new BufferedReader(new
                        InputStreamReader(inputStream));

                String line;
                while((line = bufferedReader.readLine())!= null){
                    stringBuffer.append(line).append('\n');
                }

                if(!stringBuffer.toString().isEmpty()){
                    return activityReference.get().parseJson(stringBuffer.toString());
                }else return null;

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<Route> routes) {
            super.onPostExecute(routes);
            if(routes != null){
                activityReference.get().onDirectionFinderSuccess(routes);
            }
        }
    }


    public class Duration {
        public String text;
        public int value;

        public Duration(String text, int value) {
            this.text = text;
            this.value = value;
        }
    }

    public class Distance {
        public String text;
        public int value;

        public Distance(String text, int value) {
            this.text = text;
            this.value = value;
        }
    }

    public class Route {
        private Distance distance;
        private Duration duration;

        private String endAddress;
        private LatLng endLocation;
        private String startAddress;
        private LatLng startLocation;
        private List<LatLng> points;
    }

    private List<LatLng> decodePolyLine(final String poly) {
        return PolyUtil.decode(poly);
    }


    private List<Route> parseJson(String result){
        if (result == null) return null;

        List<Route> routes = new ArrayList<Route>();
        JSONObject jsonData = null;
        JSONArray jsonRoutes = null;
        try {
            jsonData = new JSONObject(result);
            jsonRoutes = jsonData.getJSONArray("routes");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < jsonRoutes.length(); i++) {

            JSONObject jsonRoute = null;
            JSONObject overview_polylineJson = null;
            JSONArray jsonLegs = null;
            JSONObject jsonLeg = null;
            JSONObject jsonDistance = null;
            JSONObject jsonDuration = null;
            JSONObject jsonEndLocation = null;
            JSONObject jsonStartLocation = null;
            Route route = new Route();

            try {
                jsonRoute = jsonRoutes.getJSONObject(i);
                overview_polylineJson = jsonRoute.getJSONObject("overview_polyline");
                jsonLegs = jsonRoute.getJSONArray("legs");
                jsonLeg = jsonLegs.getJSONObject(0);
                jsonDistance = jsonLeg.getJSONObject("distance");
                jsonDuration = jsonLeg.getJSONObject("duration");
                jsonEndLocation = jsonLeg.getJSONObject("end_location");
                jsonStartLocation = jsonLeg.getJSONObject("start_location");

                route.distance = new Distance(jsonDistance.getString("text"),
                        jsonDistance.getInt("value"));
                route.duration = new Duration(jsonDuration.getString("text"),
                        jsonDuration.getInt("value"));
                route.endAddress = jsonLeg.getString("end_address");
                route.startAddress = jsonLeg.getString("start_address");
                route.startLocation = new LatLng(jsonStartLocation.getDouble("lat"),
                        jsonStartLocation.getDouble("lng"));
                route.endLocation = new LatLng(jsonEndLocation.getDouble("lat"),
                        jsonEndLocation.getDouble("lng"));
                route.points = decodePolyLine(overview_polylineJson.getString("points"));
                routes.add(route);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return routes;
    }

    private String getDirectionsUrl(){
        String currentLocationLatLngString = String.valueOf(currentLocationLatLng.latitude) + ',' +
                String.valueOf(currentLocationLatLng.longitude);
        String carLocationLatLngString = String.valueOf(carLocationLatLng.latitude) + ',' +
                String.valueOf(carLocationLatLng.longitude);
        String origin = "";
        String destination = "";
        try {
            origin = URLEncoder.encode(currentLocationLatLngString, "utf-8");
            destination = URLEncoder.encode(carLocationLatLngString, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if(currentMode.equals(TravelMode.DRIVING)) {
            return "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin +
                    "&destination=" + destination + "&key=" +
                    getString(R.string.google_maps_api_key);
        }else if(currentMode.equals(TravelMode.WALKING)){
            return "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                    origin + "&destination=" + destination + "&mode=" +"walking" + "&key=" +
                    getString(R.string.google_maps_api_key);
        }
        return null;
    }

    private void onDirectionFinderSuccess(List<Route> routes) {
         List<Marker> originMarkers = new ArrayList<>();
         List<Marker> destinationMarkers = new ArrayList<>();
         List<Polyline> polylinePaths = new ArrayList<>();

        for (final Route route : routes) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));

            myUIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(context,"Duration: "+route.duration.text +
                            " Distance: " + route.distance.text, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, TOAST_VERTICAL_OFFSET);
                    toast.show();
                }
            },300);


            originMarkers.add(googleMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.person_current_place))
                    .title(route.startAddress)
                    .position(route.startLocation)));

            destinationMarkers.add(googleMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.parked_car))
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));


            polylinePaths.add(googleMap.addPolyline(polylineOptions));

        }

    }
}