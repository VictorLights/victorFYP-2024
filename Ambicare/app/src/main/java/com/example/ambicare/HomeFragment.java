package com.example.ambicare;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap dashMap;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private TextView lastModifiedText;

    private TextView emergencyFallStatsText;

    private static final String CHANNEL_ID = "fall_detection_channel";

    private TextView dateTimeText;
    private TextView batteryStatus;
    private TextView deviceNameDisplay;
    private String batteryLevel;
    private String deviceName;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy\nzzzz z\nHH:mm:ss", Locale.getDefault());
            String dateString = dateFormat.format(currentDate);
            dateTimeText.setText(dateString);
            handler.postDelayed(this, 1000);
        }
    };

    private Marker elderMarker;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        emergencyFallStatsText = view.findViewById(R.id.emergency_fall_stats_text);
        createNotificationChannel();

        // Date and Time
        dateTimeText = view.findViewById(R.id.date_time_text);
        handler.post(runnable);

        // Find the TextView by its ID
        lastModifiedText = view.findViewById(R.id.last_modified_text);
        deviceNameDisplay = view.findViewById(R.id.device_name);
        batteryStatus = view.findViewById(R.id.battery_status);

        getDeviceId();
        displayVideoCount();
        getLastModifiedFile();

        // Add the Firestore listener here
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DocumentReference docRef = db.collection("fallrecLogs").document(uid);
            docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        boolean isFallen = snapshot.getBoolean("fallen");
                        TextView statusTextView = view.findViewById(R.id.status_text); // replace with your TextView id
                        if (isFallen) {
                            statusTextView.setText("Camera Status: Fall Detected");
                            showNotification();
                        } else {
                            statusTextView.setText("Camera Status: Elderly is Safe");
                        }
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                }
            });
        }

        return view;
    }

    @SuppressLint("MissingPermission")
    private void showNotification() {
        Intent videoPlayerIntent = new Intent(getActivity(), VideoPlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, videoPlayerIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_visibility_24)
                .setContentTitle("Fall Detected!")
                .setContentText("A fall has been detected by the camera.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
        notificationManager.notify(0, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Fall Detection Channel";
            String description = "Channel for fall detection notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        dashMap = googleMap;
        LatLng me = new LatLng(3.0639354, 101.6173037);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        CollectionReference devListRef = FirebaseFirestore.getInstance().collection("deviceList");
        String userEmail = firebaseAuth.getCurrentUser().getEmail();
        devListRef
                .whereEqualTo("email", userEmail)
                .whereEqualTo("deviceType", "Watch")
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            String deviceId = documentSnapshot.getString("deviceId");
                            deviceName = documentSnapshot.getString("deviceName");
                            if (deviceName != null) {
                                elderMarker = dashMap.addMarker(new MarkerOptions().position(me).title("Elder"));

                                float zoomLevel = 15.0f; // Specify the desired zoom level here

                                dashMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, zoomLevel)); // Set the zoom level
                            }

                        }
                    }
                });


    }


    public void displayVideoCount() {
        CameraFragment cameraFragment = new CameraFragment();
        cameraFragment.getVideoCount(new CameraFragment.VideoCountCallback() {
            @Override
            public void onCallback(int videoCount) {
                TextView videoCountTextView = getView().findViewById(R.id.video_count_text);
                String displayText = "Fall Detected Footage: " + videoCount;
                videoCountTextView.setText(displayText);
            }
        });
    }

    public void getLastModifiedFile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user.getUid();
        FirebaseStorage.getInstance().getReference().child("recordings/" + uid).listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                List<StorageReference> items = listResult.getItems();
                if (!items.isEmpty()) {
                    Collections.sort(items, new Comparator<StorageReference>() {
                        @Override
                        public int compare(StorageReference o1, StorageReference o2) {
                            return Long.compare(getUpdatedTime(o2), getUpdatedTime(o1));
                        }
                    });

                    // The first item is the last modified file
                    StorageReference lastModifiedFile = items.get(0);
                    lastModifiedFile.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                        @Override
                        public void onSuccess(StorageMetadata storageMetadata) {
                            long updatedTimeMillis = storageMetadata.getUpdatedTimeMillis();
                            Date updatedDate = new Date(updatedTimeMillis);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy\nHH:mm:ss", Locale.getDefault());
                            String updatedDateString = dateFormat.format(updatedDate);
                            lastModifiedText.setText("Latest Fall Detected: " + updatedDateString.split("\n")[0] + "\nTime of Fall: " + updatedDateString.split("\n")[1]);
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getActivity(), "Failed to retrieve files", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private long getUpdatedTime(StorageReference reference) {
        Task<StorageMetadata> metadataTask = reference.getMetadata();
        if (metadataTask.isSuccessful()) {
            StorageMetadata metadata = metadataTask.getResult();
            if (metadata != null) {
                return metadata.getUpdatedTimeMillis();
            }
        }
        return 0;
    }

    public void getDeviceId() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        CollectionReference devListRef = FirebaseFirestore.getInstance().collection("deviceList");
        String userEmail = firebaseAuth.getCurrentUser().getEmail();
        devListRef
                .whereEqualTo("email", userEmail)
                .whereEqualTo("deviceType", "Watch")
                .get()
                .addOnCompleteListener(task -> {
                    Log.d("BAT", "Battery ");
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            String deviceId = documentSnapshot.getString("deviceId");
                            deviceName = documentSnapshot.getString("deviceName");
                            getBatteryStatus(deviceId);
                            deviceNameDisplay.setText(deviceName);
                            Log.d("BAT", "Battery " + deviceId);
                        }
                    } else {
                        batteryStatus.setText("No wearable registered.");
                        Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void getBatteryStatus(String deviceId) {
        CollectionReference devList = FirebaseFirestore.getInstance().collection("devices");
        devList
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            batteryLevel = documentSnapshot.getString("batteryLevel");
                        }
                        batteryStatus.setText("Battery Status: " + batteryLevel);
                        Log.d("BAT", "Battery " + batteryStatus);
                    } else {
                        Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        batteryStatus.setText("No wearable registered.");
                    }
                });
    }
}
