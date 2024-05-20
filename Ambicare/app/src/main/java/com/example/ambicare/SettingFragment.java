package com.example.ambicare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends Fragment {

    private ListView listView;
    private SettingAdapter settingAdapter;
    private List<SettingItem> settingItems;
    private FirebaseFirestore fStore;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_setting, container, false);

        listView = rootView.findViewById(R.id.settingsListView);
        settingItems = new ArrayList<>();
        settingItems.add(new SettingItem(R.drawable.baseline_logout_24, "Logout"));
        settingItems.add(new SettingItem(R.drawable.baseline_password_24, "Change Password"));


        // Initialize the adapter with the list of items
        settingAdapter = new SettingAdapter(getContext(), settingItems);

        // Set the adapter to the ListView
        listView.setAdapter(settingAdapter);

        // Set item click listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        // Logout
                        logoutAndUpdateIsActive();
                        break;
                    case 1:
                        // Change Password
                        Intent intent = new Intent(getActivity(), changePwActivity.class);
                        startActivity(intent);
                        break;
                }
            }
        });

        fStore = FirebaseFirestore.getInstance();

        return rootView;
    }

    private void logoutAndUpdateIsActive() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d("AAAAA", "LogoutUser");
            Log.d("AAAAA", "Update isActive status = false");
            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(user.getUid())
                    .update("isActive", false)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Audit Log
                            AuditLog.logAction("Logout performed.");
                            // Successfully updated isActive status to false
                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(getContext(), "Logout successful", Toast.LENGTH_SHORT).show();

                            //stop ListenerDBService once logged out
                            Log.d("AAAAA", "Stop listener service - SettingFragment line96");
                            Intent stopServiceIntent = new Intent(getActivity(), ListenerDBService.class);
                            getActivity().stopService(stopServiceIntent);

                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle failure to update isActive status
                            Toast.makeText(getContext(), "Failed to update isActive status", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}

