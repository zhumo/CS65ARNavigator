package edu.dartmouth.com.arnavigation.location;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.nearby.Nearby;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class GetPlacePhotoRequest extends AsyncTask<NearbyPlace, Void, Boolean> {
    private static String API_KEY = "AIzaSyDtlvUxYCusIgmEU6ACg6Wm5KEu-5UM3aM";

    private OnPostExecute listener;

    public GetPlacePhotoRequest(OnPostExecute listener) {
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(NearbyPlace... places) {
        NearbyPlace place = places[0];

        Uri imageUri;
        if(place.photoReference == null) {
            imageUri = Uri.parse(place.iconUrl);
        } else {
            imageUri = Uri.parse("https://maps.googleapis.com/maps/api/place/photo").buildUpon()
                    .appendQueryParameter("photoreference", place.photoReference)
                    .appendQueryParameter("maxwidth", "1600")
                    .appendQueryParameter("key", API_KEY)
                    .build();
        }

        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) new URL(imageUri.toString()).openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap imageBitmap = BitmapFactory.decodeStream(input);
            place.imageBitmap = imageBitmap;
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //disconnect from url
            if (connection != null) {
                connection.disconnect();
            }
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if(listener != null) { listener.onPostExecute(success); }
    }

    public interface OnPostExecute {
        void onPostExecute(Boolean success);
    }
}
