package com.example.ambicare;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingPagerAdapter extends FragmentStateAdapter {

    public OnboardingPagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new OnboardFragment1();
            case 1:
                return new OnboardFragment2();
            case 2:
                return new OnboardFragment3();
            default:
                // Return a default fragment or throw an exception for invalid position
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        // Return the total number of fragments
        return 3;
    }
}
