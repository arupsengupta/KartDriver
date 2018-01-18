package com.example.mapwithmarker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */
public class MapsMarkerActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private FusedLocationProviderClient locationProviderClient;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 15;
    private GoogleMap mMap;
    private Location lastLocation;
    private LatLng destLocation;
    private CameraPosition cameraPosition;
    private boolean locationPermissionGranted;
    private Context mContext;
    private FollowMeLocationSource followMeLocationSource;
    private Button button;
    private Button acceptButton;
    private Button cancelButton;
    private Button startButton;
    private Button endButton;

    private UserCard user;

    private TextView userDistance;
    private TextView userAddress;
    private TextView destinationAddress;
    private TextView userName;
    private TextView riderName;
    private TextView riderContact;
    private TextView pickupText;
    private TextView dropText;

    private HorizontalScrollView usercard;
    private ScrollView riderCard;
    private Ringtone r;
    private String userID;
    private String carID;
    private String carType;
    private String requestID;



    // Keys for storing activity state
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private Socket mSocket;
    {
        try{
            mSocket = IO.socket("https://mkart.herokuapp.com");
        }catch (URISyntaxException ex){
            ex.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve location and camera position from saved instance state
        if (savedInstanceState != null) {
            lastLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        mContext = getApplicationContext();
        followMeLocationSource = new FollowMeLocationSource();

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userName = (TextView) findViewById(R.id.user_name);
        userDistance = (TextView) findViewById(R.id.user_distance);
        userAddress = (TextView) findViewById(R.id.user_address);
        destinationAddress = (TextView) findViewById(R.id.destination_address);
        usercard = (HorizontalScrollView) findViewById(R.id.usercard);
        riderCard = (ScrollView) findViewById(R.id.ridercard);

        button = (Button) findViewById(R.id.reject_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rejectRide();
            }
        });

        acceptButton = (Button) findViewById(R.id.accept_button);
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptRide();
            }
        });

        riderName = (TextView) findViewById(R.id.riderName);
        riderContact = (TextView) findViewById(R.id.riderContact);
        pickupText = (TextView) findViewById(R.id.pickupText);
        dropText = (TextView) findViewById(R.id.dropText);

//        usercard.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                acceptRide();
//            }
//        });

        cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRide();
            }
        });

        startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRide();
            }
        });
        endButton = (Button) findViewById(R.id.end_button);
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endRide();
            }
        });

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        r = RingtoneManager.getRingtone(mContext, ringtoneUri);

        // Construct a FusedLocaitonProviderClient
//        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mSocket.on("call", onCall);
        mSocket.connect();

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        userID = sharedPref.getString("user_id", null);
        carID = sharedPref.getString("vehicle_id", null);
        carType = sharedPref.getString("vehicle_type", null);

        mSocket.emit(carType, carID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.logout_action:
                SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.commit();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            default:
                super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        followMeLocationSource.getBestAvailableProvider();

        mMap = googleMap;
        if(mMap != null){
            mMap.setLocationSource(followMeLocationSource);
        }

        getLocationPermission();
        updateLocationUI();
    }

    private void rejectRide(){
        RequestQueue queue = Volley.newRequestQueue(this);
        final String rejectRideUrl = "https://mkart.herokuapp.com/geo/reject/" + userID + "/" + requestID;

        StringRequest rejectRequset = new StringRequest(Request.Method.GET, rejectRideUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(mContext, "Ride rejected", Toast.LENGTH_LONG).show();
                if(r.isPlaying()){
                    r.stop();

                }
                usercard.setVisibility(View.GONE);
                button.setVisibility(View.GONE);
                acceptButton.setVisibility(View.GONE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(mContext, "Error rejecting ride", Toast.LENGTH_LONG).show();
                if(r.isPlaying()){
                    r.stop();

                }
                usercard.setVisibility(View.GONE);
                button.setVisibility(View.GONE);
                acceptButton.setVisibility(View.GONE);
            }
        });

        queue.add(rejectRequset);
    }

    private void acceptRide(){
        RequestQueue queue = Volley.newRequestQueue(this);
        final String acceptRideUrl = "https://mkart.herokuapp.com/geo/accept/" + userID + "/" + requestID;

        StringRequest acceptRequset = new StringRequest(Request.Method.GET, acceptRideUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(mContext, "Ride accepted", Toast.LENGTH_LONG).show();
                if(r.isPlaying()){
                    r.stop();

                }

                usercard.setVisibility(View.GONE);
                button.setVisibility(View.GONE);
                acceptButton.setVisibility(View.GONE);

                riderName.setText(user.getName());
                riderContact.setText(user.getPhone());
                pickupText.setText(user.getPickup());
                dropText.setText(user.getDrop());

                riderCard.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                startButton.setVisibility(View.VISIBLE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(mContext, "Error acepting ride", Toast.LENGTH_LONG).show();
                if(r.isPlaying()){
                    r.stop();

                }
                usercard.setVisibility(View.GONE);
                button.setVisibility(View.GONE);
                acceptButton.setVisibility(View.GONE);
            }
        });

        queue.add(acceptRequset);
    }

    private void cancelRide(){
        RequestQueue queue = Volley.newRequestQueue(this);
        final String cancelRideUrl = "https://mkart.herokuapp.com/geo/cancel/" + userID + "/" + requestID;

        StringRequest cancelRequset = new StringRequest(Request.Method.GET, cancelRideUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(mContext, "Ride canceled", Toast.LENGTH_LONG).show();
                riderCard.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(mContext, "Error canceling ride.. try again", Toast.LENGTH_LONG).show();
            }
        });

        queue.add(cancelRequset);
    }

    private void startRide(){
        RequestQueue queue = Volley.newRequestQueue(this);
        final String startRideUrl = "https://mkart.herokuapp.com/geo/start/" + userID + "/" + requestID;

        StringRequest startRequset = new StringRequest(Request.Method.GET, startRideUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(mContext, "Ride started", Toast.LENGTH_LONG).show();

                cancelButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                endButton.setVisibility(View.VISIBLE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(mContext, "Error acepting ride", Toast.LENGTH_LONG).show();
                cancelButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                riderCard.setVisibility(View.GONE);
            }
        });

        queue.add(startRequset);
    }

    private void endRide(){
        RequestQueue queue = Volley.newRequestQueue(this);
        final String endRideUrl = "https://mkart.herokuapp.com/geo/end/" + userID + "/" + requestID;

        StringRequest startRequset = new StringRequest(Request.Method.GET, endRideUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(mContext, "Ride completed", Toast.LENGTH_LONG).show();
                endButton.setVisibility(View.GONE);
                riderCard.setVisibility(View.GONE);
                TripEndDialog tripEndDialog = new TripEndDialog();
                tripEndDialog.show(getSupportFragmentManager(), "fareDialog");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(mContext, "Error completing ride.. try again", Toast.LENGTH_LONG).show();
            }
        });

        queue.add(startRequset);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastLocation);
        }
    }

    private void getLocationPermission(){
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationPermissionGranted = true;
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try{
            if(locationPermissionGranted){
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                /*Task<Location> locationTask = locationProviderClient.getLastLocation();
                locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        lastLocation = location;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
                    }
                });*/
            }else{
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastLocation = null;
                getLocationPermission();
            }
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }

    private void getDirections(LatLng newLoc){
        destLocation = newLoc;
        String url = "https://maps.googleapis.com/maps/api/directions/json?"
                + "origin=" + lastLocation.getLatitude() + "," + lastLocation.getLongitude()
                + "&destination=" + newLoc.latitude + "," + newLoc.longitude
                + "&key=" + getString(R.string.google_maps_key);
//                + "&sensor=false&units=metric&mode=driving";
        GetDirectionAsyncTask task = new GetDirectionAsyncTask();
        task.execute(new String[]{url});
    }

    private void setUserCard(UserCard userCard, LatLng newLoc){
        final String url = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                "origins="+lastLocation.getLatitude() + "," + lastLocation.getLongitude() +
                "&destinations=" + newLoc.latitude + "," + newLoc.longitude +
                "&key=" + getString(R.string.google_maps_key);

        userName.setText(userCard.getName());
        userAddress.setText(userCard.getPickup());
        destinationAddress.setText(userCard.getDrop());
        GetDistanceAsyncTask task = new GetDistanceAsyncTask();
        task.execute(new String[]{url});
        usercard.setVisibility(View.VISIBLE);
        button.setVisibility(View.VISIBLE);
        acceptButton.setVisibility(View.VISIBLE);
        r.play();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String permissions[], @NonNull int[] grantResults){
        locationPermissionGranted = false;
        switch (requestCode){
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        try{
            mMap.setMyLocationEnabled(false);
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }

    private class FollowMeLocationSource implements LocationSource, LocationListener {
        private OnLocationChangedListener mListener;
        private LocationManager locationManager;
        private final Criteria criteria = new Criteria();
        private String bestAvailableProvider;
        /*
        updates are restricted to one every 10 seconds, and only when
        movement of more than 10 meters has been detected
         */
        private final int minTime = 10000;
        private final int minDistance = 10;

        private FollowMeLocationSource(){
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            criteria.setAltitudeRequired(true);
            criteria.setBearingRequired(true);
            criteria.setSpeedRequired(true);
            criteria.setCostAllowed(true);
        }

        private void getBestAvailableProvider(){
            bestAvailableProvider = locationManager.getBestProvider(criteria, true);
            Log.i("LocationChanged", "Location provider::" + bestAvailableProvider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.i("LocationChanged", "Location Changed");
            if(mListener != null){
                mListener.onLocationChanged(location);
            }

            lastLocation = location;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));

            final String postURL = "https://mkart.herokuapp.com/geo";

            JSONObject object = new JSONObject();
            try {
                object.put("vehicletype", carType);
                JSONObject coords = new JSONObject();
                coords.put("lng", lastLocation.getLongitude());
                coords.put("lat", lastLocation.getLatitude());
                object.put("coords", coords);
                object.put("carid", carID);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            final RequestQueue requestQueue = Volley.newRequestQueue(mContext);

            JsonObjectRequest locationUpdateRequest = new JsonObjectRequest(Request.Method.POST, postURL, object, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.i("LocationChanged", "Location updated successfully");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(mContext, error.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.i("LocationChanged", error.getMessage());
                }
            });

            requestQueue.add(locationUpdateRequest);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @SuppressLint("MissingPermission")
        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {
            mListener = onLocationChangedListener;

            if(bestAvailableProvider != null){
                locationManager.requestLocationUpdates(bestAvailableProvider, minTime, minDistance, this);
                Log.i("LocationChanged", "Activate called");
            }else {
                Log.i("LocationChanged", "No location provider");
            }
        }

        @Override
        public void deactivate() {
            Log.i("LocationChanged","Listener Deactivated");
            locationManager.removeUpdates(this);
            mListener = null;
        }
    }

    private class GetDistanceAsyncTask extends AsyncTask<String, Void, JSONObject>{
        @Override
        protected JSONObject doInBackground(String... urls) {
            OkHttpClient client = new OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder().url(urls[0]).build();
            try{
                okhttp3.Response response = client.newCall(request).execute();
                if(response.isSuccessful()){
                    return new JSONObject(response.body().string());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(JSONObject response){
            super.onPostExecute(response);
            try {
                String status = response.getString("status");
                if("OK".equalsIgnoreCase(status)){
                    JSONArray rows = response.getJSONArray("rows");
                    JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
                    JSONObject element = elements.getJSONObject(0);
                    JSONObject duration = element.getJSONObject("duration");
                    String min = duration.getString("text");
                    JSONObject distance = element.getJSONObject("distance");
                    String dist = distance.getString("text");
                    userDistance.setText(dist + ", " + min);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetDirectionAsyncTask extends AsyncTask<String, Void, JSONObject>{
        @Override
        protected JSONObject doInBackground(String... urls) {
            OkHttpClient client = new OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder().url(urls[0]).build();
            try {
                okhttp3.Response response = client.newCall(request).execute();
                if(response.isSuccessful()){
                    JSONObject jsonResp = new JSONObject(response.body().string());
                    return jsonResp;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result){
            super.onPostExecute(result);
            //Log.i("LocationChanged", result == null ? "null response": result.toString());
            ParserTask parserTask = new ParserTask();
            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<JSONObject, Integer, List<List<HashMap<String, String>>>>{

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(JSONObject... jsonObjects) {
            JSONObject object = jsonObjects[0];
            List<List<HashMap<String, String>>> routes = null;
            try{
                DataParser parser = new DataParser();
                // Starts parsing data
                routes = parser.parse(object);
            }catch (Exception ex){
                Log.d("LocationChanged", ex.toString());
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result){
            ArrayList<LatLng> points;
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            PolylineOptions lineOptions = null;
            LatLng start = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            builder.include(start);

            // Traversing through all the routes
            for(int i=0; i < result.size(); i++){
                points = new ArrayList<>();
                points.add(start);

                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0; j < path.size(); j++){
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    builder.include(position);
                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.BLACK);
            }
            builder.include(destLocation);

            if(lineOptions != null){
                int padding = 350;
                mMap.clear();
                mMap.addPolyline(lineOptions);
                LatLngBounds bounds = builder.build();
                mMap.addMarker(new MarkerOptions().position(start));
                mMap.addMarker(new MarkerOptions().position(destLocation).icon(BitmapDescriptorFactory.fromResource(R.drawable.person_2)));
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }else{
                Log.d("LocationChanged", "Without Polylines drawn");
            }
        }
    }

    private Emitter.Listener onCall = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try{
                        requestID = data.getString("requestID");
                        JSONObject coords = data.getJSONObject("coords");
                        double lat = coords.getDouble("lat");
                        double lng = coords.getDouble("lng");
                        String name = coords.getString("name");
                        String phone = coords.getString("phone");
                        String source = coords.getString("source");
                        String dest = coords.getString("destination");
                        LatLng callingLoc = new LatLng(lat, lng);
                        user = new UserCard(name, phone, source, dest);
                        getDirections(callingLoc);
                        setUserCard(user, callingLoc);
                    }catch (JSONException ex){
                        ex.printStackTrace();
                    }
                }
            });
        }
    };
}