package edu.dartmouth.com.arnavigation.location;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class GetNearbyPlacesRequest extends AsyncTask<LatLng, Void, JSONObject> {
    private static String NEARBY_PLACES_AUTHORITY = "maps.googleapis.com";
    private static String API_KEY = "AIzaSyDtlvUxYCusIgmEU6ACg6Wm5KEu-5UM3aM";
    private OnPostExecute listener;

    public GetNearbyPlacesRequest(OnPostExecute listener) {
        this.listener = listener;
    }

    @Override
    protected JSONObject doInBackground(LatLng... latLngs) {
        LatLng originLatLng = latLngs[0];

        HttpsURLConnection httpsURLConnection = null;
        JSONObject responseJSON = new JSONObject();
        try {
            // This will be overridden if the HTTP request goes well.
            responseJSON.put("status", "JSON_OBJECT_ERROR");

            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.scheme("https")
                    .authority(NEARBY_PLACES_AUTHORITY)
                    .appendPath("maps")
                    .appendPath("api")
                    .appendPath("place")
                    .appendPath("nearbysearch")
                    .appendPath("json")
                    .appendQueryParameter("key", API_KEY)
                    .appendQueryParameter("location", originLatLng.latitude + "," + originLatLng.longitude)
                    .appendQueryParameter("radius", "1000");
            Uri nearbyPlacesUri = uriBuilder.build();
//            Log.d("mztag", "Attempting: " + nearbyPlacesUri.toString());
            URL nearbyPlacesURL = new URL(nearbyPlacesUri.toString());

            httpsURLConnection = (HttpsURLConnection) nearbyPlacesURL.openConnection();
            httpsURLConnection.connect();

            //set up file reading streams
            InputStream inputStream = httpsURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";

            //append to buffer
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            //get response string
            responseJSON = new JSONObject(stringBuffer.toString());

            //close streams
            inputStream.close();
            inputStreamReader.close();
            bufferedReader.close();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //disconnect from url
            if (httpsURLConnection != null) {
                httpsURLConnection.disconnect();
            }
        }

        return responseJSON;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        super.onPostExecute(jsonObject);
        if(listener != null) { listener.onPostExecute(jsonObject); }
    }

    public interface OnPostExecute {
        void onPostExecute(JSONObject responseJSON);
    }
}
