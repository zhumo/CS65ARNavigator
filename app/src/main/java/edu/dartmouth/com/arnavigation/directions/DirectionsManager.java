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

    /**
     * Makes calls to Directions API and gets JSON data
     */

    //for URL request
    private static final String TRAVEL_MODE_WALKING = "mode_walking";
    private static final String API_KEY = "AIzaSyDCIgMjOYnQmPGmpL5AIzzfW8Uh9HwOPXc";

    private static final String HTTPS_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String HTTP_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static final String OUTPUT_TYPE = "json";

    public static String DIRECTIONS_RESULTS_ACTION = "directions.results";
    public static String DIRECTIONS_RESULTS_SUCCESS_KEY = "directionsResultsSuccess";

    private Context mContext;

    private List<List<HashMap<String, String>>> paths;
    private String overviewPoly;


    public DirectionsManager(Context context) {
        mContext = context;
    }

    private void setPaths(List<List<HashMap<String, String>>> newPaths){
        paths = newPaths;
    }

    public List<List<HashMap<String, String>>> getPaths(){
        return paths;
    }

    //prepares the URL for API call
    public void getDirectionsWithAddress(LatLng originLatLng, String address) {
        //create URL request string using address
        String origin = "origin=" + originLatLng.latitude + "," + originLatLng.longitude;
        String destination = "destination=" + address;
        String param = origin + "&" + destination + "&" + TRAVEL_MODE_WALKING;
        String urlRequest = HTTPS_URL + OUTPUT_TYPE + "?" + param + API_KEY;

        //start directions task
        startDirectionsWithURL(urlRequest);
    }


    //calls API and outputs a JSON-type response string
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

    //starts the async directions task
    private void startDirectionsWithURL(String url){
        new RequestDirectionsTask().execute(url);
    }


    // ********** ASYNC TASKS *********** //

    //async task to get direction
    private class RequestDirectionsTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... strings) {
            String responseString = requestDirectionsWithURL(strings[0]);
            JSONObject responseJSON = new JSONObject();
            try {
                responseJSON = new JSONObject(responseString);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return responseJSON;
        }

        @Override
        protected void onPostExecute(JSONObject responseJSON) {
            String responseStatus;
            try {
                responseStatus = responseJSON.getString("status");
            } catch (JSONException e) {
                responseStatus = "JSON_OBJECT_ERROR";
                e.printStackTrace();
            }

            if(responseStatus.equals("OK")) {
                try {
                    overviewPoly = responseJSON.getJSONObject("overview_polyline").getString("points");
                } catch (JSONException e) { e.printStackTrace(); }

                new ParseDirectionsTask().execute(responseJSON);
            } else {
                Intent intent = new Intent();
                intent.setAction(DIRECTIONS_RESULTS_ACTION);
                intent.putExtra(DIRECTIONS_RESULTS_SUCCESS_KEY, false);
                mContext.sendBroadcast(intent);
            }
        }

        //async task to parse JSON response from request
        class ParseDirectionsTask extends AsyncTask<JSONObject, Void, List<List<HashMap<String, String>>>> {
            @Override
            protected List<List<HashMap<String, String>>> doInBackground(JSONObject... responseJSONs) {
                List<List<HashMap<String, String>>> routes = null;

                //use DirectionsParser class to parse JSONObject
                try {
                    DirectionsParser directionsParser = new DirectionsParser();
                    routes = directionsParser.parse(responseJSONs[0].getJSONArray("routes"));
                } catch (JSONException je) {
                    je.printStackTrace();
                }
              
                return routes;
            }

            @Override
            protected void onPostExecute(List<List<HashMap<String, String>>> paths) {
                if (paths != null) {
                    setPaths(paths);
                }

                Intent intent = new Intent();
                intent.setAction(DIRECTIONS_RESULTS_ACTION);
                intent.putExtra(DIRECTIONS_RESULTS_SUCCESS_KEY, true);
                mContext.sendBroadcast(intent);
            }
        }
    }
}
