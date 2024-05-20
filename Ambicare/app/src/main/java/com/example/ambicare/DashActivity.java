package com.example.ambicare;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.ambicare.databinding.ActivityDashBinding;

public class DashActivity extends AppCompatActivity {

    ActivityDashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding  = ActivityDashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.devices) {
                replaceFragment(new DevicesFragment());
            } else if (item.getItemId() == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.settings) {
                replaceFragment(new SettingFragment());
            } else if (item.getItemId() == R.id.camera) {
                replaceFragment(new CameraFragment());
        }

            return true;
        });
        binding.bottomNavigationView.setSelectedItemId(R.id.home);
    }
    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_dash,fragment);
        fragmentTransaction.commit();
    }

}