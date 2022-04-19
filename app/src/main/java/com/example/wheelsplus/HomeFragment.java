package com.example.wheelsplus;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    View root;

    MapView map;
    TextInputEditText if_viaje;

    boolean settingsOK = false;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    double latitude, longitude;
    GeoPoint startPoint = null;
    GeoPoint otherPoint = null;
    Geocoder geocoder;

    IMapController mapController;

    FusedLocationProviderClient mFusedLocationClient;

    ActivityResultLauncher<String> requestPermissionLocation = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if(result){
                startLocationUpdates();
            }
        }
    });

    ActivityResultLauncher<IntentSenderRequest> getLocationSettings = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(result.getResultCode() == -1){
                settingsOK = true;
                startLocationUpdates();
            }
        }
    });

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_home, container, false);

        Context ctx = getActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        geocoder = new Geocoder(getActivity().getBaseContext());

        locationRequest = createLocationRequest();
        locationCallback = createLocationCallback();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        requestPermissionLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        initMap();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if_viaje = root.findViewById(R.id.if_viaje);


        if_viaje.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i == EditorInfo.IME_ACTION_DONE){
                    String address = if_viaje.getText().toString();
                    if(!address.isEmpty()){
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(address, 2);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address addressResult = addresses.get(0);
                                GeoPoint position = new GeoPoint(addressResult.getLatitude(), addressResult.getLongitude());
                                InitLocationFragment nextFrag = new InitLocationFragment();
                                Bundle bundle = new Bundle();


                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.nav_host_fragment, nextFrag, "findThisFragment")
                                        .addToBackStack(null)
                                        .commit();
                            } else {
                                if_viaje.setError("Dirección no encontrada");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(getActivity(), "La dirección esta vacía", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        mapController = map.getController();
        mapController.setZoom(18.0);
        mapController.setCenter(this.startPoint);
        checkLocationSettings();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        stopLocationUpdates();
    }

    private void initMap(){
        map = (MapView) root.findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().activate();
    }

    private Marker createMarker(GeoPoint p, String title, String desc, int iconID){
        Marker marker = null;
        if(map!=null) {
            marker = new Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                Drawable myIcon = getResources().getDrawable(iconID, getActivity().getTheme());
                marker.setIcon(myIcon);
            }
            marker.setPosition(p);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        }
        return marker;
    }

    public void startLocationUpdates(){
        if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(settingsOK){
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        }
    }

    private void stopLocationUpdates(){
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private LocationRequest createLocationRequest(){
        LocationRequest req = LocationRequest.create().setFastestInterval(1000).setInterval(10000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return req;
    }

    private LocationCallback createLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location lastLocation = locationResult.getLastLocation();

                try {
                    if (lastLocation != null) {
                        Log.i("Callback", "Latitude: " + lastLocation.getLatitude() + " Longitude: " + lastLocation.getLongitude());
                        latitude = lastLocation.getLatitude();
                        longitude = lastLocation.getLongitude();

                        if (startPoint == null) {
                            mapController.setZoom(15.0);
                            mapController.setCenter(new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));
                        }
                        startPoint = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                        map.removeAllViews();

                        if (otherPoint != null) {
                            Marker other = createMarker(otherPoint, geocoder.getFromLocation(otherPoint.getLatitude(), otherPoint.getLongitude(), 1).get(0).getAddressLine(0), null, R.drawable.ic_outline_home_24);
                            map.getOverlays().add(other);
                        }

                        Marker other = createMarker(startPoint, geocoder.getFromLocation(startPoint.getLatitude(), startPoint.getLongitude(), 1).get(0).getAddressLine(0), null, R.drawable.ic_outline_home_24);
                        map.getOverlays().add(other);
                    }

                } catch (Exception e) {

                }
            }
        };
    }

    private void checkLocationSettings(){
        LocationSettingsRequest.Builder builder = new
                LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(getActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.i("LocationTest", "GPS is ON");
                settingsOK = true;
                startLocationUpdates();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if(((ApiException) e).getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED){
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    IntentSenderRequest isr = new IntentSenderRequest.Builder(resolvable.getResolution()).build();
                    getLocationSettings.launch(isr);
                }
            }
        });
    }

}