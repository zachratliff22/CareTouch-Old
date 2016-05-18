package eubank_ratliff.caretouch;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class InfoFragment extends DialogFragment {

    public boolean clicked = false;

    SharedPreferences prefs, prefs2;
    SharedPreferences.Editor editor, editor2;
    public boolean mDonate;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_info, container, false);
        getDialog().requestWindowFeature(STYLE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.rounded_edittext);

        prefs = getActivity().getSharedPreferences("Charity", Context.MODE_PRIVATE);
        editor = prefs.edit();

        prefs2 = getActivity().getSharedPreferences("Locations", Context.MODE_PRIVATE);
        editor2 = prefs2.edit();

        String locations_charity = prefs2.getString("locations", ".... .... CLEAR: .... \n .... .... CLEAR: .... \n");

        Log.d("CharityFile: ", locations_charity);

        final Geocoder geocoder;
        geocoder = new Geocoder(getActivity(), Locale.getDefault());

        final Button button_food = (Button)rootView.findViewById(R.id.button_food);
        final TextView address = (TextView)rootView.findViewById(R.id.address);
        final TextView active_time = (TextView)rootView.findViewById(R.id.active);

        button_food.setTextColor(R.color.red);

        Bundle info = getArguments();

        final long last_cleared;

        final double ref_lat = info.getDouble("Latitude");
        final double ref_long = info.getDouble("Longitude");

        /*
        Reading through locations given charity too
         */

        StringBuilder sb = new StringBuilder(locations_charity);
        if(!locations_charity.equalsIgnoreCase("")){
            String[] lines = sb.toString().split("\\n");
            locations_charity = "";
            for(String s: lines){
                Log.d("StringBuilder: ", s);
                if(s.contains("...."))
                    continue;
                String sub1 = s.substring(s.indexOf(": ") + 1);
                Log.d("Sub1: ", sub1);
                sub1 = sub1.replaceAll("\n", "");
                sub1 = sub1.replace(" ", "");
                sub1 = sub1.trim();

                if(sub1.equalsIgnoreCase(""))
                    continue;

                long location_last_cleared = Long.parseLong(sub1);
                if(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - location_last_cleared) > 4){
                    s = "";
                }
                locations_charity += (s + "\n");
            }

            sb = new StringBuilder(locations_charity);
            lines = sb.toString().split("\\n");
            for(String s: lines){
                if(s.contains(String.valueOf(ref_lat)) && s.contains(String.valueOf(ref_long))){
                    clicked = true;
                    button_food.setBackground(getResources().getDrawable(R.drawable.clicked_rounded_button));
                    button_food.setTextColor(getResources().getColor(android.R.color.white));
                }
            }
        }


        last_cleared = prefs.getLong("last_cleared", 0);
        if (last_cleared == 0) {
            editor.putLong("last_cleared", System.currentTimeMillis());
            editor.commit();
        }

        //List<Address> temp_add = geocoder.getFromLocationName(address.getText().toString().replace("\n", " "), 1);
        ParseQuery<ParseObject> p_obj = ParseQuery.getQuery((info.getString("City") + info.getString("State")).replace(" ", ""));

        ParseGeoPoint temp_geoPoint = new ParseGeoPoint();
        temp_geoPoint.setLatitude(info.getDouble("Latitude"));
        temp_geoPoint.setLongitude(info.getDouble("Longitude"));
        //p_obj.whereEqualTo("Location", info.getDouble("Latitude"));
        //p_obj.whereEqualTo("Longitude", info.getDouble("Longitude"));
        p_obj.whereEqualTo("Location", temp_geoPoint);
        p_obj.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, com.parse.ParseException e) {
                if (e == null) {
                    if (!objects.isEmpty()) {
                        String food = String.valueOf(objects.get(0).getString("Food"));
                        String clothes = String.valueOf(objects.get(0).getString("Clothes"));

                        Date created = objects.get(0).getCreatedAt();

                        long time = System.currentTimeMillis();
                        long active_time_as_long = time - created.getTime();
                        Date active_time_as_date = new Date(active_time_as_long);
                        Log.d("Active Mill: ", String.valueOf(active_time_as_date));

                        Date now = new Date(System.currentTimeMillis());
                        button_food.setText(food);

                        if (TimeUnit.MILLISECONDS.toHours(active_time_as_date.getTime()) > 0) {
                            active_time.setText("Active: " + String.valueOf(TimeUnit.MILLISECONDS.toHours(active_time_as_date.getTime()))
                                    + " hours " + String.valueOf(TimeUnit.MILLISECONDS.toMinutes(active_time_as_date.getTime()) % 60) + " minutes");
                        } else
                            active_time.setText("Active: " + String.valueOf(TimeUnit.MILLISECONDS.toMinutes(active_time_as_date.getTime()) % 60) + " minutes");


                        Bundle info = getArguments();

                        address.setText(info.getString("Address") + "\n" + info.getString("City") + ", " + info.getString("State")
                                + " " + info.getString("Post") + "\n" + info.getString("Country"));
                        address.setText(address.getText().toString().replace("null", ""));

                    }
                } else
                    Log.d("Error: ", "Something went wrong uploading to parse.");
            }

        });

        address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocationName(address.getText().toString().replace("\n", " "), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Address add = addresses.get(0);
                LatLng move_city = new LatLng(add.getLatitude(), add.getLongitude());

                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?f=d&daddr=" + move_city.latitude + "," + move_city.longitude ));
                startActivity(intent);
            }
        });

        button_food.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d("Last Cleared Milli: ", String.valueOf(prefs.getLong("last_cleared", 0)));
                Log.d("Current Time: ", String.valueOf(System.currentTimeMillis()));
                Log.d("Last Cleared: ", String.valueOf(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - prefs.getLong("last_cleared", System.currentTimeMillis()))));
                long last_cleared = prefs.getLong("last_cleared", prefs.getLong("last_cleared", 0));
                Log.d("LC_Var: ", String.valueOf(last_cleared));


                AlertDialog donate_dialog = new AlertDialog.Builder(getActivity()).setTitle("Donate Food")
                        .setMessage("You are about to alert the CareTouch community that you have donated food." +
                                "To benefit homeless people and the CareTouch community, only click this button when" +
                                " you have given food to the homeless person at this location.").setCancelable(false).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDonate = false;
                                dialog.dismiss();
                                check_button();
                            }
                        }).setPositiveButton("Donate", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDonate = true;
                                dialog.dismiss();
                                check_button();
                            }
                        }).setIcon(android.R.drawable.ic_dialog_info).create();

                donate_dialog.show();


            }
        });

        return rootView;
    }


    public void check_button(){

        if(!mDonate){
            return;
        }

        prefs = getActivity().getSharedPreferences("Charity", Context.MODE_PRIVATE);
        editor = prefs.edit();

        prefs2 = getActivity().getSharedPreferences("Locations", Context.MODE_PRIVATE);
        editor2 = prefs2.edit();

        String locations_charity = prefs2.getString("locations", ".... .... CLEAR: .... \n .... .... CLEAR: .... \n");

        Log.d("CharityFile: ", locations_charity);

        final Geocoder geocoder;
        geocoder = new Geocoder(getActivity(), Locale.getDefault());

        Button button_food = (Button) getView().findViewById(R.id.button_food);

        Bundle info = getArguments();



        final int food_count;
        final long last_cleared;

        final double ref_lat = info.getDouble("Latitude");
        final double ref_long = info.getDouble("Longitude");


        final String ref_charity = locations_charity;

        final String locs = ref_charity;
        last_cleared = prefs.getLong("last_cleared", prefs.getLong("last_cleared", 0));


        if(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - last_cleared) > 2){
            if(clicked == true){
                Toast.makeText(getActivity(), "Already contributed", Toast.LENGTH_SHORT).show();
                return;
            }



            clicked = true;
            editor.putLong("last_cleared", System.currentTimeMillis());
            editor.putInt("food", 1);
            editor.commit();

            button_food.setBackground(getResources().getDrawable(R.drawable.clicked_rounded_button));
            button_food.setTextColor(getResources().getColor(android.R.color.white));
            final int current_count = Integer.parseInt(button_food.getText().toString());
            final ParseQuery<ParseObject> p_obj = ParseQuery.getQuery(info.getString("City") + info.getString("State"));
            button_food.setText(String.valueOf(current_count + 1));

            ParseGeoPoint temp_geoPoint = new ParseGeoPoint();


            temp_geoPoint.setLatitude(ref_lat);
            temp_geoPoint.setLongitude(ref_long);

            Log.d("Location Message: ", locs + "\n" + String.valueOf(ref_lat) + " " +
                    String.valueOf(ref_long) + " CLEAR: " + String.valueOf(System.currentTimeMillis()));
            editor2.putString("locations", locs + "\n" + String.valueOf(ref_lat) + " " +
                    String.valueOf(ref_long) + " CLEAR: " + String.valueOf(System.currentTimeMillis()) + "\n");
            editor2.commit();

            p_obj.whereEqualTo("Location", temp_geoPoint);
            p_obj.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, com.parse.ParseException e) {
                    if (e == null) {
                        if (!objects.isEmpty()) {
                            objects.get(0).put("Food", String.valueOf(current_count + 1));
                            objects.get(0).saveInBackground();
                        }
                    } else
                        Log.d("Error: ", "Something went wrong uploading to parse.");
                }

            });
        }else{
            food_count = prefs.getInt("food", 0);
            if(food_count < 3){
                if(clicked == true){
                    Toast.makeText(getActivity(), "Already contributed", Toast.LENGTH_SHORT).show();
                    return;
                }


                clicked = true;
                editor.putInt("food", food_count + 1);
                editor.commit();

                button_food.setBackground(getResources().getDrawable(R.drawable.clicked_rounded_button));
                button_food.setTextColor(getResources().getColor(android.R.color.white));
                final ParseQuery<ParseObject> p_obj = ParseQuery.getQuery(info.getString("City") + info.getString("State"));
                final int current_count = Integer.parseInt(button_food.getText().toString());
                ParseGeoPoint temp_geoPoint = new ParseGeoPoint();
                button_food.setText(String.valueOf(current_count + 1));


                temp_geoPoint.setLatitude(ref_lat);
                temp_geoPoint.setLongitude(ref_long);

                Log.d("Location Message: ", locs + "\n" + String.valueOf(ref_lat) + " " +
                        String.valueOf(ref_long) + " CLEAR: " + String.valueOf(System.currentTimeMillis()));
                editor2.putString("locations", locs + "\n" + String.valueOf(ref_lat) + " " +
                        String.valueOf(ref_long) + " CLEAR: " + String.valueOf(System.currentTimeMillis()) + "\n");
                editor2.commit();

                p_obj.whereEqualTo("Location", temp_geoPoint);
                p_obj.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, com.parse.ParseException e) {
                        if (e == null) {
                            if (!objects.isEmpty()) {
                                objects.get(0).put("Food", String.valueOf(current_count + 1));
                                objects.get(0).saveInBackground();
                            }
                        } else
                            Log.d("Error: ", "Something went wrong uploading to parse.");
                    }

                });

            }else{
                Toast.makeText(getActivity(), "Reached limit. Try again later.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

    }



}
