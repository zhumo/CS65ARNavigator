package edu.dartmouth.com.arnavigation;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.MapFragment;

import java.util.ArrayList;

import edu.dartmouth.com.arnavigation.views.NonSwipingViewPager;
import edu.dartmouth.com.arnavigation.views.ViewPagerAdapter;

public class NavigationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        NonSwipingViewPager viewPager = findViewById(R.id.navigation_view_pager);

        final ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(new CameraFragment());
        fragments.add(new NavigationMapFragment());

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(viewPagerAdapter);
    }
}
