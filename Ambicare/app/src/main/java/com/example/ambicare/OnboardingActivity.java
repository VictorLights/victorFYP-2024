package com.example.ambicare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager; // Declare ViewPager2 variable
    private TabLayout tabLayout; // Declare TabLayout variable

    private Button btnSkip;
    private Button btnNext;
    private Button btnSignUp;
    private FirebaseAuth auth;
    private FirebaseFirestore fStore;
    public String deviceId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fStore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        setContentView(R.layout.activity_onboarding);


        // Initialize ViewPager2
        viewPager = findViewById(R.id.view_pager);

        // Set up ViewPager2 adapter
        FragmentStateAdapter adapter = new OnboardingPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Initialize TabLayout
        tabLayout = findViewById(R.id.tab_layout);

        // Set up TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                }
        ).attach();

        // Initialize buttons
        btnSkip = findViewById(R.id.btn_skip);
        btnNext = findViewById(R.id.btn_next);
        btnSignUp = findViewById(R.id.btn_sign_up);

        // Initially, display the first onboarding fragment
        updateButtons(0);

        // Set click listeners
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(3, true);
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OnboardingActivity.this, SignupActivity.class));
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
            }
        });

        // Add a page change listener to ViewPager2
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Update buttons based on the current fragment
                updateButtons(position);
            }
        });
    }

    private void updateButtons(int position) {
        switch (position) {
            case 0:
            case 1:
                btnSkip.setVisibility(View.VISIBLE);
                btnNext.setVisibility(View.VISIBLE);
                btnSignUp.setVisibility(View.GONE);
                break;
            case 2:
                btnSkip.setVisibility(View.GONE);
                btnNext.setVisibility(View.GONE);
                btnSignUp.setVisibility(View.VISIBLE);
                break;
        }
    }

    //check if any accounts is still logged in on this device
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String currentUserEmail = currentUser.getEmail();

            //check if user has deviceId registered before
            Log.d("AAAAA", "Onboarding checked user already logged in - OnboardingActivity line125");
            startActivity(new Intent(getApplicationContext(), DashActivity.class));

            checkDeviceRegistration(currentUserEmail);
        }
    }


    private void checkDeviceRegistration(String email) {
        CollectionReference deviceListRef = fStore.collection("deviceList");

        deviceListRef.whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.exists()) {
                                // Retrieve the device ID from the document
                                deviceId = document.getString("deviceId");
                                Log.d("AAAAA", "Checking for email and deviceId: " + email + ", " + deviceId + " - OnboardingActivity line144");

                                // Registered deviceId found go home page and start listening service
                                startService(new Intent(this, ListenerDBService.class));
                                Log.d("AAAAA", "Call ListenerDBService - OnboardingActivity line 148");
                            }
                        }
                        // No document found, then let it runs as usual
                    } else {
                        // Handle failure to retrieve documents
                        Log.e("AAAAA", "Error getting documents - OnboardingActivity line154 ->", task.getException());
                    }
                });
    }
}

