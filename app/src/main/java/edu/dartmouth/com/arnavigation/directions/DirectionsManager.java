package edu.dartmouth.com.arnavigation.directions;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by jctwake on 2/27/18.
 */

public class DirectionsManager {
    //for polyline
    private PolylineOptions polylineOptions;
    private static final float POLYLINE_WIDTH = 15;

    //for URL request
    private static final String[] TRAVEL_MODES = {"mode_walking", "mode_driving"};
    private static final String API_KEY = "AIzaSyDCIgMjOYnQmPGmpL5AIzzfW8Uh9HwOPXc";

    private static final String HTTPS_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String HTTP_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String OUTPUT_TYPE = "json";

    public static String NEW_DIRECTIONS_ACTION = "directions.new";
    public static String DIRECTIONS_JSON_KEY = "DirectionsJSON";

    Context mContext;

    List<List<HashMap<String, String>>> paths;

    //use this constructor for broadcasting

    public DirectionsManager(Context context) {
        mContext = context;
    }

    private void setPaths(List<List<HashMap<String, String>>> newPaths){
        paths = newPaths;
    }

    public List<List<HashMap<String, String>>> getPaths(){
        return paths;
    }

    public void getDirectionsWithAddress(LatLng originLatLng, String address, int travelMode) {
        //create URL request string using address
        String origin = "origin=" + originLatLng.latitude + "," + originLatLng.longitude;
        String destination = "destination=" + address;
        String param = origin + "&" + destination + "&" + TRAVEL_MODES[travelMode];
        String urlRequest = HTTPS_URL + OUTPUT_TYPE + "?" + param + API_KEY;

        //start directions task
        startDirectionsWithURL(urlRequest);
    }


    public void getDirectionsWithLatLng(LatLng originLatLng, LatLng destinationLatLng, int travelMode){
        //for now assume travel mode is walking
        travelMode = 0;

        //create URL request string using LatLng
        String origin = "origin=" + originLatLng.latitude + "," + originLatLng.longitude;
        String destination = "destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude;
        String param = origin + "&" + destination + "&" + TRAVEL_MODES[travelMode];
        String urlRequest = HTTPS_URL + OUTPUT_TYPE + "?" + param + API_KEY;

        //start directions task
        startDirectionsWithURL(urlRequest);
    }

    private String requestDirectionsWithURL(String reqURL){

        //make API call with url

        String responseString = "";
        InputStream inputStream = null;
        HttpsURLConnection httpsURLConnection = null;

        try {
            //get url connection
            URL url = new URL(reqURL);
            httpsURLConnection = (HttpsURLConnection) url.openConnection();
            httpsURLConnection.connect();

            //set up file reading streams
            inputStream = httpsURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";

            //append to buffer
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            //get response string
            responseString = stringBuffer.toString();

            //close streams
            inputStream.close();
            inputStreamReader.close();
            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //disconnect from url
            httpsURLConnection.disconnect();
        }

        return responseString;
    }



    private void startDirectionsWithURL(String url){
        new RequestDirectionsTask().execute(url);
    }


    // ********** ASYNC TASKS *********** //

    //async task to get direction
    private class RequestDirectionsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";

            try {
                responseString = requestDirectionsWithURL(strings[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d("RESPONSE", responseString);

            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            new ParseDirectionsTask().execute(result);
        }

        //async task to parse JSON response from request
        private class ParseDirectionsTask extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {
            @Override
            protected List<List<HashMap<String, String>>> doInBackground(String... strings) {

                JSONObject jsonObject = null;

                List<List<HashMap<String, String>>> routes = null;

                //use DirectionsParser class to parse JSONObject
                try {
                    jsonObject = new JSONObject(strings[0]);
                    DirectionsParser directionsParser = new DirectionsParser();
                    routes = directionsParser.parse(jsonObject);

                    //String encodedPoly = jsonObject.getJSONObject("overview_polyline").getString("points");

                } catch (JSONException je) {
                    je.printStackTrace();
                }

                if (routes != null) {
                    setPaths(routes);
                }

                return routes;
            }

            @Override
            protected void onPostExecute(List<List<HashMap<String, String>>> paths) {
                Intent intent = new Intent();
                intent.setAction(NEW_DIRECTIONS_ACTION);
                mContext.sendBroadcast(intent);
            }
        }
    }
}
