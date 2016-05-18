package eubank_ratliff.caretouch;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends ActionBarActivity implements LocationListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private InterstitialAd mInterstitialAd;
    private SharedPreferences.Editor editor;
    private AdView mAdView;
    private int mScreen;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        Enable Parse
         */




        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        /*
        For first time use of the application...
         */


        SharedPreferences first_time = getSharedPreferences("First", MODE_PRIVATE);
        final SharedPreferences.Editor first_edit = first_time.edit();

        boolean first = first_time.getBoolean("first", true);
        if(first){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            findViewById(R.id.autocomplete).setVisibility(View.GONE);
            getSupportActionBar().hide();
            mScreen = 1;
            final ImageView imageView = (ImageView)findViewById(R.id.intro_slides);
            imageView.setBackground(getResources().getDrawable(R.drawable.intro1));
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    switch (mScreen) {
                        case 1:
                            mScreen = 2;
                            imageView.setBackground(getResources().getDrawable(R.drawable.intro2));
                            break;
                        case 2:
                            mScreen = 3;
                            imageView.setBackground(getResources().getDrawable(R.drawable.intro3));
                            break;
                        case 3:
                            mScreen = 4;
                            imageView.setBackground(getResources().getDrawable(R.drawable.intro4));
                            break;
                        case 4:
                            mScreen = 5;
                            imageView.setVisibility(View.GONE);
                            getSupportActionBar().show();
                            findViewById(R.id.autocomplete).setVisibility(View.VISIBLE);
                            findViewById(R.id.intro_layout).setVisibility(View.GONE);
                            first_edit.putBoolean("first", false);
                            first_edit.commit();
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                            mAdView = (AdView) findViewById(R.id.adView);
                            AdRequest adRequest = new AdRequest.Builder().build();
                            mAdView.loadAd(adRequest);
                            requestNewInterstitial();


                    }

                }
            });

        }else{
            findViewById(R.id.intro_layout).setVisibility(View.GONE);
        }

        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        //StrictMode.setThreadPolicy(policy);

        /*
        Setting up Google Places API
         */

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        final AutoCompleteTextView autocompleteView = (AutoCompleteTextView) findViewById(R.id.autocomplete);
        autocompleteView.setAdapter(new PlacesAutoCompleteAdapter(MainActivity.this, R.layout.autocomplete_list_item));

        autocompleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (autocompleteView.getText().length() > 0)
                    autocompleteView.getText().clear();
            }
        });

        autocompleteView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get data associated with the specified position
                // in the list (AdapterView)

                //mInterstitialAd.show();


                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);


                String description = (String) parent.getItemAtPosition(position);

                if(description.equalsIgnoreCase("footer")){
                    autocompleteView.setText("");
                }else{
                    try {
                        Geocoder geocoder;
                        geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocationName(description, 1);

                        Address add = addresses.get(0);
                        LatLng move_city = new LatLng(add.getLatitude(), add.getLongitude());

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(move_city));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                        /*
                        Download new markers from map...
                         */

                        try {
                            ParseGeoPoint geoPoint = new ParseGeoPoint(move_city.latitude, move_city.longitude);
                            if(mLocation != null){
                                mMap.clear();
                                mLocation.setLatitude(move_city.latitude);
                                mLocation.setLongitude(move_city.longitude);
                                addMarkers();
                            }else
                                Toast.makeText(MainActivity.this, "Something is wrong with your location services", Toast.LENGTH_LONG).show();

                        } catch (com.parse.ParseException e) {
                            e.printStackTrace();
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }
        });

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

        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        //StrictMode.setThreadPolicy(policy);


        /*
        This is to check if the user has rated the app yet...
         */
        AppRater.app_launched(this);

        mMap = googleMap;

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(bestProvider);
        mLocation = location;
        if (location != null) {
            //onLocationChanged(location);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }
        locationManager.requestLocationUpdates(bestProvider, 20000, 0, (LocationListener) MainActivity.this);

        try {
            if(location != null)
                addMarkers();
            else
                Toast.makeText(MainActivity.this, "Check location services", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (com.parse.ParseException e) {
            e.printStackTrace();
        }

        final FragmentManager fm = getFragmentManager();
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("CareTouch")
                        .setMessage("Only drop a pin near a person in need. Are you sure you would like to place a pin here?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences prefs = getSharedPreferences("Pin_drops", MODE_PRIVATE);
                                editor = prefs.edit();
                                int pin_count = prefs.getInt("Count", 0);
                                long last_cleared = prefs.getLong("last_cleared", 0);
                                if(last_cleared == 0){
                                    editor.putLong("last_cleared", System.currentTimeMillis());
                                    last_cleared = System.currentTimeMillis();
                                }
                                if(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - last_cleared) < 2){
                                    // Check the count.
                                    if(pin_count < 3){

                                        Geocoder geocoder;
                                        List<Address> addresses = null;
                                        geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                        try {
                                            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                                            if(addresses.isEmpty()){
                                                Log.d("Address: ", "Empty. Must be on water...");
                                                Toast.makeText(MainActivity.this, "Location not allowed", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            //Go ahead and set stuff.

                                            final String savedClass = (addresses.get(0).getLocality() + addresses.get(0).getAdminArea()).replace(" ", "");
                                            if(savedClass.contains("null")){
                                                Toast.makeText(MainActivity.this, "Location not allowed...", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            Log.d("UPLOADING: ", "Parse Information Uploading");
                                            ParseObject parseObject = new ParseObject(savedClass.replace(" ", ""));

                                            if(pin_count >= 0){
                                                if(mInterstitialAd.isLoaded()){
                                                    mInterstitialAd.show();
                                                    Log.d("Loaded: ", "Interstitial advertisement");
                                                }
                                            }

                                            pin_count = pin_count + 1;
                                            editor.putInt("Count", pin_count);
                                            editor.commit();

                                            parseObject.put("Location", new ParseGeoPoint(latLng.latitude, latLng.longitude));
                                            parseObject.put("Food", "0");
                                            //parseObject.put("Clothes", "0");
                                            parseObject.saveInBackground();

                                            ParseQuery<ParseObject> query = ParseQuery.getQuery("CareTouch_Posts");
                                            query.whereEqualTo("City", savedClass.replace(" ", ""));
                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, com.parse.ParseException e) {
                                                    if(e == null){
                                                        if(objects.isEmpty()){
                                                            ParseObject upload = new ParseObject("CareTouch_Posts");
                                                            upload.put("City",savedClass.replace(" ", ""));
                                                            upload.saveInBackground();
                                                        }
                                                    }else
                                                        Log.d("Error: ", "Something went wrong uploading to parse.");
                                                }

                                            });


                                    /*
                                    This maps the City to the Posts for autodelete on the Cloud
                                     */


                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        mMap.addMarker(new MarkerOptions().position(latLng).title(""));

                                        //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                                    }else{
                                        new AlertDialog.Builder(MainActivity.this).setTitle("CareTouch")
                                                .setIcon(android.R.drawable.ic_dialog_info)
                                                .setMessage("You have reached the maximum number of location posts allowed." +
                                                        " Please try again later.").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        }).show();
                                    }
                                }else{
                                    // Go ahead and clear and do whatever.
                                    last_cleared = System.currentTimeMillis();
                                    editor.putLong("last_cleared", last_cleared);
                                    editor.putInt("Count", 1);
                                    editor.commit();


                                    Geocoder geocoder;
                                    List<Address> addresses = null;
                                    geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                    try {
                                        addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                                        final String savedClass = (addresses.get(0).getLocality() + addresses.get(0).getAdminArea()).replace(" ", "");
                                        Log.d("UPLOADING: ", "Parse Information Uploading");
                                        ParseObject parseObject = new ParseObject(savedClass.replace(" ", ""));


                                        parseObject.put("Location", new ParseGeoPoint(latLng.latitude, latLng.longitude));
                                        parseObject.put("Food", "0");
                                        //parseObject.put("Clothes", "0");
                                        parseObject.saveInBackground();

                                        ParseQuery<ParseObject> query = ParseQuery.getQuery("CareTouch_Posts");
                                        query.whereEqualTo("City", savedClass.replace(" ", ""));
                                        query.findInBackground(new FindCallback<ParseObject>() {
                                            @Override
                                            public void done(List<ParseObject> objects, com.parse.ParseException e) {
                                                if(e == null){
                                                    if(objects.isEmpty()){
                                                        ParseObject upload = new ParseObject("CareTouch_Posts");
                                                        upload.put("City",savedClass.replace(" ", ""));
                                                        upload.saveInBackground();
                                                    }
                                                }else
                                                    Log.d("Error: ", "Something went wrong uploading to parse.");
                                            }

                                        });


                                    /*
                                    This maps the City to the Posts for autodelete on the Cloud
                                     */


                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    mMap.addMarker(new MarkerOptions().position(latLng).title(""));

                                    //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                                }

                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_map)
                        .show();




            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {



                List<Address> temp_addresses = null;
                Geocoder temp_geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    temp_addresses = temp_geocoder.getFromLocation(marker.getPosition().latitude, marker.getPosition().longitude, 1);
                    String address = temp_addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                    String city = temp_addresses.get(0).getLocality();
                    String state = temp_addresses.get(0).getAdminArea();
                    String country = temp_addresses.get(0).getCountryName();
                    String postalCode = temp_addresses.get(0).getPostalCode();
                    //String knownName = addresses.get(0).getFeatureName();

                    InfoFragment ifrag = new InfoFragment();
                    Bundle info = new Bundle();
                    info.putString("Address", address);
                    info.putString("City", city);
                    info.putString("State", state);
                    info.putString("Country", country);
                    info.putString("Post", postalCode);
                    info.putDouble("Latitude", marker.getPosition().latitude);
                    info.putDouble("Longitude", marker.getPosition().longitude);
                    ifrag.setArguments(info);
                    ifrag.show(fm, "DF");
                } catch (IOException e) {
                    e.printStackTrace();
                }


                JSON_Adapter adt = new JSON_Adapter();
                //JSONObject j = adt.getJSON("https://maps.googleapis.com/maps/api/directions/json?origin=Waco,TX&destination=Paris,TX&&region=us&key=AIzaSyAgWlQf-ja4uw1Vec-5hLVwNYn18QCFT-Q\n");
                //JSONObject g = adt.getJSON("https://maps.googleapis.com/maps/api/geocode/json?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&key=AIzaSyBTAbwyaJYUNags6XGlx-B95muwq8A5xoM\n");
                //Log.d("DIRECTIONS: ", j.toString());
                //Log.d("GEOCODE: ", g.toString());

                                            /*
                                            Polyline line;
                                            List<LatLng> list = decodePoly(j.getJSONArray("routes").getJSONObject(0).getJSONObject("overview_polyline").getString("points"));
                                            PolylineOptions options = new PolylineOptions().width(10).color(Color.RED).geodesic(true);
                                            for (int z = 0; z < list.size(); z++) {
                                                LatLng point = list.get(z);
                                                options.add(point);
                                            }
                                            line = mMap.addPolyline(options);
                                            */


                //Log.d("Known Name: ", address + " ... " + city + ", " + state);
                //Toast.makeText(MainActivity.this, "HELLO", Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView)findViewById(R.id.autocomplete);
        autoCompleteTextView.setText("");
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        //mMap.addMarker(new MarkerOptions().position(latLng));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
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

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }


    /*
    Used for drawing the lines on the map

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }


    public String format_query(String current, String dest){
        return "https://maps.googleapis.com/maps/api/directions/json?origin=" + current + " &destination=" + dest + "&key=AIzaSyAgWlQf-ja4uw1Vec-5hLVwNYn18QCFT-Q\n";

    }

    */

    public void addMarkers() throws IOException, com.parse.ParseException {

        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        addresses = geocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 1);
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>((addresses.get(0).getLocality() + addresses.get(0).getAdminArea()).replace(" ", ""));
        Log.d("Class: ", addresses.get(0).getLocality() + addresses.get(0).getAdminArea());

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, com.parse.ParseException e) {
                if (e == null) {
                    if (!objects.isEmpty()) {
                        for(int i = 0; i < objects.size(); i++){
                            mMap.addMarker(new MarkerOptions().position(new LatLng(objects.get(i).getParseGeoPoint("Location").getLatitude(),
                                    objects.get(i).getParseGeoPoint("Location").getLongitude())));
                        }
                    }
                } else
                    Log.d("Error: ", "Something went wrong retrieving from parse.");
            }

        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView)findViewById(R.id.autocomplete);
        autoCompleteTextView.setText("");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView)findViewById(R.id.autocomplete);
        autoCompleteTextView.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.refresh:
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice("82DFB1C81AAFA80B2B9F2C85AE1DFDF2")
                .build();

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-1059286159138787/3306503555");
        //mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }
        });

        mInterstitialAd.loadAd(adRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences first_time = getSharedPreferences("First", MODE_PRIVATE);
        final SharedPreferences.Editor first_edit = first_time.edit();

        boolean first = first_time.getBoolean("first", true);
        if(first){
            // do nothing with the adview yet...
        }else{
            mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            requestNewInterstitial();
        }


    }
}

