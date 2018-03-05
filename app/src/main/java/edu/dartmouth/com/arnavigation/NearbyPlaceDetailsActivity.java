package edu.dartmouth.com.arnavigation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class NearbyPlaceDetailsActivity extends AppCompatActivity {
    public static String PLACE_ID_KEY = "placeID";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_place_details);

//        String placeID = getIntent().getExtras().getString(PLACE_ID_KEY);
        String name = getIntent().getExtras().getString("name");

        TextView nameLabel = findViewById(R.id.place_name);
        nameLabel.setText(name);
    }
}
