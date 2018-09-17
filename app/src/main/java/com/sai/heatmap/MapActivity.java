package com.sai.heatmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.design.widget.Snackbar;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener {


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    private static final String TAG = "MapActivity";


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));


    private static int totalColors = 3;
    private static GoogleMap mMap;

    //vars

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private AutoCompleteTextView mSearchText;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private Place mPlace;
    private SQLHelper sqlHelper;
    private Context context = this;
    private Marker marker;
    //    private LatLng markerLatLng;
    private LatLngBounds nearbyBounds;
    private Location myLocation;
    private Map<LatLng, Marker> markers;
    private boolean infoDisplay = true;
    private ImageView ic_navigate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mSearchText = findViewById(R.id.input_search);
        ic_navigate = findViewById(R.id.ic_navigate);
        ic_navigate.setVisibility(View.GONE);
//        getLocationPermission();

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (!checkPermissions()) {
            requestPermissions();
        }

        initMap();

    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapActivity.this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d("onmapready", "map is ready");
        mMap = googleMap;

//        mMap.setMyLocationEnabled(true);
//        mMap.getUiSettings().setMapToolbarEnabled(false);


        if (mMap == null) {
            Log.wtf("wtf", "map is null");
        }

        sqlHelper = new SQLHelper(this);
        sqlHelper.open();
        markers = new HashMap<LatLng, Marker>();


//        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//            @Override
//            public boolean onMarkerClick(Marker m) {
//
//                mSearchText.setText("");
//                hideSoftKeyboard();
//                marker = m;
////                markerLatLng = m.getPosition();
//
////                    MarkerOptions options = new MarkerOptions()
////                            .position(m.getPosition())
////                            .title(m.getTitle())
////                            .snippet("Heat: " + m.getTag());
////
////                    marker = mMap.addMarker(options);
////                    marker.remove();
//
////                Log.d("marker on click:", marker.getTitle());
//
//                return false;
//            }
//        });

        initSearchSuggestions();

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {

                mMap.getUiSettings().setMapToolbarEnabled(false);
                LoadMarkersFromDB();
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                marker = null;
                mSearchText.setText("");
                //hide navigate button
                ic_navigate.setVisibility(View.GONE);

            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m) {

                ic_navigate.setVisibility(View.VISIBLE);
                mSearchText.setText("");
                hideSoftKeyboard();
                marker = m;
                return false;
            }
        });
    }

    public void LoadMarkersFromHashSet(){
        Marker m;
        Iterator iter = markers.values().iterator();

        while(iter.hasNext()){
            m = (Marker) iter.next();
            Log.d("iterator", m.getTag().toString());
            MarkerOptions options = CreateMarkerOptions(m.getTitle(),
                    Integer.parseInt(m.getTag().toString()), m.getPosition());

            mMap.addMarker(options);

        }
    };

    private void LoadMarkersFromDB() {

        //load all markers to map
//            MARKERR
        try {
            Log.d("onCreate", "loading markers");
            Cursor cursor = sqlHelper.getAllMarkers();

            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                CursorToMarker(cursor);
                cursor.moveToNext();
            }

//            infoDisplay = !infoDisplay;
            cursor.close();

        } catch (Exception e) {
            Log.i("onMapReady", "SQL db error while reading all markers");
            e.printStackTrace();
        }
    }

    private void CursorToMarker(Cursor cursor) {
        Marker m;
        Log.d("cursor info:", cursor.getString(0) + cursor.getInt(3)
                + cursor.getDouble(1) + cursor.getDouble(2));

        LatLng latLng = new LatLng(cursor.getDouble(1), cursor.getDouble(2));
        MarkerOptions options = CreateMarkerOptions(cursor.getString(0),
                cursor.getInt(3), latLng);


        m = mMap.addMarker(options);
        m.setTag(cursor.getInt(3));
        m.setPosition(latLng);
        markers.put(latLng, m);
        Log.d("loaded m: ", m.getId() + m.getTitle() + m.getTag());
//        Log.d("markertitle",markers.get(new LatLng(34.04319280000001,-117.8496541)).getTag().toString());

    }

    private MarkerOptions CreateMarkerOptions(String title, int heat, LatLng latLng){
        MarkerOptions options = new MarkerOptions()
                .title(title)
                .snippet("Heat: " + heat)
                .position(latLng);
        if (infoDisplay) {
            options = SetIconColor(heat, options);
        } else {
            //show heats only
            options.icon(BitmapDescriptorFactory.fromBitmap(writeTextOnDrawable
                    (R.drawable.ic_circle, String.valueOf(heat))));
        }
        return options;
    }

    private MarkerOptions SetIconColor(int heat, MarkerOptions options) {
        //setting color of marker
        int maxHeat = MaxHeat(null);
        Float value = (Float.valueOf(heat) / Float.valueOf(maxHeat)) * totalColors;
        if (value > 2.5)
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        else if (value > 2.0)
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        else if (value > 1.0)
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        else
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));

        return options;
    }

//    public void ToMainActivity(View view){
//        Intent intent = new Intent(MapActivity.this, MainActivity.class);
//
//        //create a Bundle object
//        Bundle extras = new Bundle();
//        //Adding key value pairs to this bundle
//        //there are quite a lot data types you can store in a bundle
//        try{
//            extras.putString("name", mPlace.getName().toString());
//            extras.putString("address", mPlace.getAddress().toString());
//            extras.putString("phone", mPlace.getPhoneNumber().toString());
//            extras.putString("rating", String.valueOf(mPlace.getRating()) + "/5");
//        }catch (NullPointerException e){
//                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage() );
//        }
//
//        //attach the bundle to the Intent object
//        intent.putExtras(extras);
//        //finally start the activity
//        startActivity(intent);
//    }


    private void initSearchSuggestions() {
        Log.d(TAG, "init: initializing");

//        LatLng nearbyLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
//        LatLngBounds nearbyBounds = NearbyBounds(nearbyLatLng, 100);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        mSearchText.setOnItemClickListener(mAutocompleteClickListener);

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, Places.getGeoDataClient(this),
                LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mPlaceAutocompleteAdapter);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                }

                return false;
            }
        });


        hideSoftKeyboard();

    }

    public LatLngBounds NearbyBounds(LatLng center, double radiusInMeters) {
        double distanceFromCenterToCorner = radiusInMeters * Math.sqrt(2.0);
        LatLng southwestCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0);
        LatLng northeastCorner =
                SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0);
        return new LatLngBounds(southwestCorner, northeastCorner);
    }

    public void CenterMyLocation(View view) throws SecurityException {
        Log.d(TAG, "onCLick: clicked gps icon");
        hideSoftKeyboard();
        mSearchText.setText("");
        getDeviceLocation();

        if (marker != null)
            marker.hideInfoWindow();

        ic_navigate.setVisibility(View.GONE);
        Log.d("mylocation:", myLocation.toString());
//        mMap.setMyLocationEnabled(true);


    }
//    private void geoLocate(){
//        Log.d(TAG, "geoLocate: geolocating");
//
//        String searchString = mSearchText.getText().toString();
//
//        Geocoder geocoder = new Geocoder(MapActivity.this);
//        List<Address> list = new ArrayList<>();
//        try{
//            list = geocoder.getFromLocationName(searchString, 1);
//        }catch (IOException e){
//            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
//        }
//
//        if(list.size() > 0){
//            Address address = list.get(0);
//
//            marker = new Marker(address.getAddressLine(0), address.getLatitude(),address.getLongitude(), 1);
//
//            LatLng nearbyLatLng = new LatLng(address.getLatitude(), address.getLongitude());
//            nearbyBounds = NearbyBounds(nearbyLatLng, 100);
//
//            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
//                    address.getAddressLine(0));
//        }
//    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mFusedLocationProviderClient.getLastLocation()
                    .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                myLocation = new Location(task.getResult());                                mMap.setMyLocationEnabled(true);
                                mMap.setMyLocationEnabled(true);

                                moveCamera(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()),
                                        DEFAULT_ZOOM, "My Location");
                            } else {
                                Log.w(TAG, "getLastLocation:exception", task.getException());

                            }
                        }
                    });


//                final Task location = mFusedLocationProviderClient.getLastLocation();
//                location.addOnCompleteListener(new OnCompleteListener() {
//                    @Override
//                    public void onComplete(@NonNull Task task) {
//                        if (task.isSuccessful()) {
//                            Log.d(TAG, "onComplete: found location!");
//                            currentLocation = (Location) task.getResult();
//
//                            Log.d("first install", currentLocation.toString());
//                            LatLng nearbyLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
//                            nearbyBounds = NearbyBounds(nearbyLatLng, 100);
//
//                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
//                                    DEFAULT_ZOOM, "My Location");
//
//                        } else {
//                            Log.d(TAG, "onComplete: current location is null");
//                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });

        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: "
                + latLng.longitude + " title: " + title);


        Marker m = markers.get(latLng);

        if (title == "My Location") {
            Log.d("moveCamtoMarker:", "my location");

        } else if (m == null) {
            Log.d("moveCamtoMarker:", "new marker");

            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .snippet("New Location");
            m = mMap.addMarker(options);
            marker = m;
            m.showInfoWindow();
            ic_navigate.setVisibility(View.VISIBLE);
        } else {
            Log.d("moveCamtoMarker:", "existing marker");
            m.showInfoWindow();
            marker = m;
            ic_navigate.setVisibility(View.VISIBLE);
        }

        if (mMap == null) {
            Log.d("nMap:", "null map");
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        hideSoftKeyboard();
    }


    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getDeviceLocation();
                return true;
            }
        }
        return false;
    }

    private void requestPermissions() {


        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) &&
                        ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.ACCESS_COARSE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            ShowSnackbar(R.string.location_required_warning, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission.
            startLocationPermissionRequest();
        }
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
//        getDeviceLocation();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
//        mMap.setMyLocationEnabled(true);

    }


//    private void getLocationPermission(){
//        Log.d(TAG, "getLocationPermission: getting location permissions");
//        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION};
//
//
//        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
//                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
//            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
//                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
//                mLocationPermissionsGranted = true;
//                initMap();
//            }else{
//                ActivityCompat.requestPermissions(this,
//                        permissions,
//                        LOCATION_PERMISSION_REQUEST_CODE);
//            }
//        }else{
//            ActivityCompat.requestPermissions(this,
//                    permissions,
//                    LOCATION_PERMISSION_REQUEST_CODE);
//        }
//    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length < 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission granted
                Log.d(TAG, "onRequestPermissionsResult: permission granted");
                getDeviceLocation();
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
//                mMap.setMyLocationEnabled(true);
            } else {
                //permission denied
                ShowSnackbar(R.string.location_required_warning, R.string.settings,
                        new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                });
            }
        }


//        switch(requestCode){
//            case LOCATION_PERMISSION_REQUEST_CODE:{
//                if(grantResults.length > 0){
//                    for(int i = 0; i < grantResults.length; i++){
//                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
//                            mLocationPermissionsGranted = false;
//                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
//                            ShowSnackbar("Device location is required to use HeatMap", "Settings",
//                                    new View.OnClickListener() {
//                                        @Override
//                                        public void onClick(View view) {
//                                            // Build intent that displays the App settings screen.
//                                            Intent intent = new Intent();
//                                            intent.setAction(
//                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                            Uri uri = Uri.fromParts("package",
//                                                    BuildConfig.APPLICATION_ID, null);
//                                            intent.setData(uri);
//                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                            startActivity(intent);
//                                        }
//                                    });
//                            return;
//                        }
//                    }
//                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
//                    mLocationPermissionsGranted = true;
//                    //initialize map
//                    initMap();
//                }
//            }
//        }
    }
    private void ShowSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }
    private void hideSoftKeyboard() {
        InputMethodManager im = (InputMethodManager) getSystemService(MapActivity.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }


    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();
            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();


            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    public void Toggle(View view){

        mMap.clear();
        LoadMarkersFromDB();
//        LoadMarkersFromHashSet();
        ic_navigate.setVisibility(View.GONE);
        infoDisplay = !infoDisplay;
    }

    private Bitmap writeTextOnDrawable(int drawableId, String text) {

        Bitmap bm = BitmapFactory.decodeResource(getResources(), drawableId)
                .copy(Bitmap.Config.ARGB_8888, true);

        Typeface tf = Typeface.create("Helvetica", Typeface.BOLD);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(tf);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(convertToPixels(context, 11));

        Rect textRect = new Rect();
        paint.getTextBounds(text, 0, text.length(), textRect);

        Canvas canvas = new Canvas(bm);

        //If the text is bigger than the canvas , reduce the font size
        if(textRect.width() >= (canvas.getWidth() - 4))     //the padding on either sides is considered as 4, so as to appropriately fit in the text
            paint.setTextSize(convertToPixels(context, 7));        //Scaling needs to be used for different dpi's

        //Calculate the positions
        int xPos = (canvas.getWidth() / 2) - 2;     //-2 is for regulating the x position offset

        //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2)) ;

        canvas.drawText(text, xPos, yPos, paint);

        return  bm;
    }



    public static int convertToPixels(Context context, int nDP)
    {
        final float conversionScale = context.getResources().getDisplayMetrics().density;

        return (int) ((nDP * conversionScale) + 0.5f) ;

    }

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            mPlace = places.get(0);

//            TODO
//            MARKERR
//            marker.setTitle(mPlace.getName().toString());
//            marker.setPosition(new LatLng(mPlace.getLatLng().latitude,mPlace.getLatLng().longitude));
//            marker.setTag(1);


//            try{
//                mPlace = new PlaceInfo();
//                mPlace.setName(place.getName().toString());
//                Log.d(TAG, "onResult: name: " + place.getName());
//                mPlace.setAddress(place.getAddress().toString());
//                Log.d(TAG, "onResult: address: " + place.getAddress());
////                mPlace.setAttributions(place.getAttributions().toString());
////                Log.d(TAG, "onResult: attributions: " + place.getAttributions());
//                mPlace.setId(place.getId());
//                Log.d(TAG, "onResult: id:" + place.ge tId());
//                mPlace.setLatlng(place.getLatLng());
//                Log.d(TAG, "onResult: latlng: " + place.getLatLng());
//                mPlace.setRating(place.getRating());
//                Log.d(TAG, "onResult: rating: " + place.getRating());
//                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
//                Log.d(TAG, "onResult: phone number: " + place.getPhoneNumber());
//                mPlace.setWebsiteUri(place.getWebsiteUri());
//                Log.d(TAG, "onResult: website uri: " + place.getWebsiteUri());
//
//                Log.d(TAG, "onResult: place: " + mPlace.toString());

//            MARKERR
            LatLng latLng = mPlace.getLatLng();
            nearbyBounds = NearbyBounds(latLng, 100);


            moveCamera(latLng, DEFAULT_ZOOM, mPlace.getName().toString());

            places.release();
        }
    };

    public void Navigate(View view){
        ic_navigate.setVisibility(View.GONE);
        mSearchText.setText("");
        Marker m = marker;

        //new marker
        if (!markers.containsKey(m.getPosition())){
            m.setTag(String.valueOf(1));
            Toast.makeText(this, "New Location added to HeatMap\n" + m.getTitle(), Toast.LENGTH_LONG).show();
            Add(m);

            //existing marker, increment heat
        }else {
//            m = markers.get(m.getPosition());
            markers.remove(m.getPosition());
//            Log.d("incrementHeat", m.getId() + m.getTag().toString());
//            int heat = Integer.parseInt(m.getTag().toString()) + 1;
//            m.setTag(heat);

            //check this if increased
//            Log.d("incrementHeat", markers.get(marker.getId()) + markers.get(marker.getPosition()).getTag().toString());
            Toast.makeText(this, "Heat Increased\n" + m.getTitle(), Toast.LENGTH_LONG).show();

            Increment(m);
        }

        MarkerOptions options = new MarkerOptions()
                .position(m.getPosition())
                .title(m.getTitle())
                .snippet("Heat: " + m.getTag());


        options = SetIconColor(Integer.parseInt(m.getTag().toString()), options);

        markers.put(m.getPosition(), m);
        m.remove();
        mMap.addMarker(options);
//        Log.d("addedmarkertohashtable", m.getId() + m.getTag().toString());

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?saddr=" + myLocation.getLatitude() + ","
                        + myLocation.getLongitude() + "&daddr=" + m.getPosition().latitude + "," + m.getPosition().longitude));
        startActivity(intent);
    }

    private void Increment(Marker m) {
        sqlHelper.updateMarker(m);
//        markers.put(m.getPosition(),m);
//        MarkerOptions options = new MarkerOptions()
//                .position(m.getPosition())
//                .title(m.getTitle())
//                .snippet("Heat: " + m.getTag())
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//        mMap.addMarker(options).showInfoWindow();

    }


    public int TotalHeat(View view){
        return sqlHelper.TotalHeat();
    }

    public int MaxHeat(View view){
        return sqlHelper.MaxHeat();
    }

    public void Add(Marker m){
//        MARKERR
        Log.d("addMarkerSQL", m.getTitle() + m.getPosition()+ m.getTag());
        Log.d("markerid", m.getId());
        sqlHelper.addMarker(m);
        Log.d("markerid", m.getId());


//        MarkerOptions options = new MarkerOptions()
//                .position(m.getPosition())
//                .title(m.getTitle())
//                .snippet("Heat: " + m.getTag())
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//        mMap.addMarker(options);
//
//        markers.put(m.getPosition(),m);
        Log.d("markerid", m.getId());
        
//        mMap.clear();
//        LoadMarkersFromDB();
//        moveCamera(marker.getPosition(), DEFAULT_ZOOM, marker.getTitle());
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(isFinishing()){
            sqlHelper.close();
            Log.d("onDestroy:", "closing database");
        }
    }

//    public void AddMarker(View view){
////        MARKERR
//        Log.d("addMarkerSQL", marker.getTitle() + marker.getPosition()+ marker.getTag());
//        marker.setTag(1);
//        Log.d("markerid", marker.getId());
//        sqlHelper.addMarker(marker);
//        Log.d("markerid", marker.getId());
//
//        marker.remove();
//
//        MarkerOptions options = new MarkerOptions()
//                .position(marker.getPosition())
//                .title(marker.getTitle())
//                .snippet("Heat: " + marker.getTag())
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//
//        Marker m = mMap.addMarker(options);
//        Log.d("markerid", m.getId());
//
//        Toast.makeText(this, "NEW LOCATION ADDED: " + marker.getTitle(), Toast.LENGTH_LONG).show();
//
////        mMap.clear();
////        LoadMarkersFromDB();
////        moveCamera(marker.getPosition(), DEFAULT_ZOOM, marker.getTitle());
//
//        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
//                Uri.parse("http://maps.google.com/maps?saddr=Your%20Location&daddr=" + m.getPosition().latitude + "," + m.getPosition().longitude));
//        startActivity(intent);
//    }


//    public void IncrementHeat(View view){
////        MARKERR
//
//        Log.d("updateMarker", marker.getTitle() + marker.getPosition()+ marker.getTag());
//
//        Marker m = sqlHelper.updateMarker(marker);
//        marker.remove();
//        Log.d("updated tag:", m.getTag() + "");
//        MarkerOptions options = new MarkerOptions()
//                .position(m.getPosition())
//                .title(m.getTitle())
//                .snippet("Heat: " + m.getTag())
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//        mMap.addMarker(options).showInfoWindow();
//
//        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
//                Uri.parse("http://maps.google.com/maps?saddr=Your%20Location&daddr=" + m.getPosition().latitude + "," + m.getPosition().longitude));
//        startActivity(intent);
//    }
}
//work on colors, switch case, formula for color, checking if location exists in db and increase heat if exists

//new Thread(() -> {
//        try {
//
//        Log.d(TAG, "initMap: initializing map");
//        SupportMapFragment mf = SupportMapFragment.newInstance();
//
//        getSupportFragmentManager().beginTransaction()
//        .add(R.id.map, mf)
//        .commit();
//
//        runOnUiThread(() -> {mf.getMapAsync(MapActivity.this);});
//
//
//        }catch (Exception ignored){
//        Log.d("mapload", "error in thread");
//        }
//        }).start();


//finished sqlhelped (changed all markers)
//next: change all markers in map activity
//

// connect to google maps
// find better formula for marker colors
//