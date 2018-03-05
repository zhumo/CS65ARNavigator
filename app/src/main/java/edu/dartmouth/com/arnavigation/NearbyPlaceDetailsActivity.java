package edu.dartmouth.com.arnavigation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

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
    }
}
