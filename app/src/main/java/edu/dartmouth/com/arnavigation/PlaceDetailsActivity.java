package edu.dartmouth.com.arnavigation;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;

import edu.dartmouth.com.arnavigation.location.NearbyPlace;
import edu.dartmouth.com.arnavigation.location.PlacesManager;

public class PlaceDetailsActivity extends AppCompatActivity {
    public static String PLACE_ID_KEY = "placeID";
    public static String ORIGIN_LAT_KEY = "originLat";
    public static String ORIGIN_LNG_KEY = "originLng";

    private PlacesManager placesManager = PlacesManager.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_details);

        String placeID = getIntent().getExtras().getString(PLACE_ID_KEY);
        NearbyPlace place = placesManager.getPlaceByPlaceID(placeID);

        TextView nameLabel = findViewById(R.id.place_name);
        nameLabel.setText(place.name);

        TextView addressLabel = findViewById(R.id.place_address);
        addressLabel.setText(place.vicinity);

        ImageView placeImage = findViewById(R.id.place_image);
        if(place.photoReference == null) {
            placeImage.setImageResource(R.drawable.photo_not_available);
        } else {
            placeImage.setImageBitmap(place.imageBitmap);
        }

        TextView distanceLabel = findViewById(R.id.place_distance);
        float[] distance = new float[1];
        float originLat = getIntent().getExtras().getFloat(ORIGIN_LAT_KEY);
        float originLng = getIntent().getExtras().getFloat(ORIGIN_LNG_KEY);
        Location.distanceBetween(originLat, originLng, place.latitude, place.longitude, distance);

        String formattedDistance = new DecimalFormat("#.##").format(distance[0] / 1000.0f );
        distanceLabel.setText(formattedDistance + " km away");
    }
}