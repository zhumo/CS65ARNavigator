package edu.dartmouth.com.arnavigation.view_pages;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<Fragment> fragments;
    public ViewPagerAdapter(FragmentManager fragmentManager, ArrayList<Fragment> frags) {
        super(fragmentManager);
        fragments = frags;
    }

    @Override
    public Fragment getItem(int position) { return fragments.get(position); }

    @Override
    public int getCount() { return fragments.size(); }
}
