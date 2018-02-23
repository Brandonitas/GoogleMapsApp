package brandon.example.com.mapsapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location location;
    private Marker marker;
    private final int REQUEST_LOCATION_CODE = 99; //cualquier numero
    private RequestQueue mQueue;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){//validamos la version
            checkLocationPermission();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mQueue = VolleySingleton.getInstance(this).getRequestQueue();
    }

    public void execute(View view){
        switch (view.getId()){
            case R.id.zoomin:
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
                break;
            case R.id.zoomout:
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
                break;
            case R.id.style:
                if(mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL){
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }else{
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                break;
            case R.id.challenge:
                jsonCities("http://geodb-free-service.wirefreethought.com/v1/geo/cities?minPopultaion=2000");
                break;
        }
    }

    public boolean checkLocationPermission()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED )
        {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
            }
            else
            {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
            }
            return false;

        }
        else
            return true;
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                ==PackageManager.PERMISSION_GRANTED){
            googleApiClient = new GoogleApiClient.Builder(this).
                    addConnectionCallbacks(this).
                    addOnConnectionFailedListener(this).
                    addApi(LocationServices.API).
                    build();
            googleApiClient.connect();
            mMap.setMyLocationEnabled(true);//mi ubi
        }
        // Add a marker in Sydney and move the camera
       /* LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    @Override
    public void onLocationChanged(Location location) {//Actializar ubicacion
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        this.location = location;
        if(marker != null){
            marker.remove();
        }
        LatLng latLng = new LatLng(lat,lon);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng).title("Aqui Mero").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        this.marker = mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(10));
        if(googleApiClient != null){
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    private void jsonCities(String url){
        //adapter.clear();
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONArray data = response.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            Ubicacion ubicacion = new Ubicacion();
                            JSONObject jsonObject  =data.getJSONObject(i);
                            ubicacion.city = jsonObject.getString("city");
                            ubicacion.lat = jsonObject.getDouble("latitude");
                            ubicacion.lon = jsonObject.getDouble("longitude");




                            LatLng latLng = new LatLng(ubicacion.lat,ubicacion.lon);

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(latLng).title(ubicacion.city).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                            mMap.addMarker(markerOptions);
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            mMap.animateCamera(CameraUpdateFactory.zoomBy(10));

                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
            mQueue.add(request);
    }



}
