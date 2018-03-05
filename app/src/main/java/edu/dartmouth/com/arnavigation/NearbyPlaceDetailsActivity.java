package edu.dartmouth.com.arnavigation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import edu.dartmouth.com.arnavigation.location.NearbyPlace;
import edu.dartmouth.com.arnavigation.location.PlacesManager;

public class NearbyPlaceDetailsActivity extends AppCompatActivity {
    public static String PLACE_ID_KEY = "placeID";
    private PlacesManager placesManager = PlacesManager.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_place_details);

        String placeID = getIntent().getExtras().getString(PLACE_ID_KEY);
        NearbyPlace place = placesManager.getPlaceByPlaceID(placeID);

        TextView nameLabel = findViewById(R.id.place_name);
        nameLabel.setText(place.name);

        TextView addressLabel = findViewById(R.id.place_address);
        addressLabel.setText(place.vicinity);

        ImageView placeImage = findViewById(R.id.place_image);
        placeImage.setImageBitmap(place.imageBitmap);
    }
}
