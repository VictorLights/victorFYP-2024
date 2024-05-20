package com.example.ambicare;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DevicesFragment extends Fragment {

    // Define references to Firebase authentication and Firestore
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private CollectionReference devicesRef;
    private CollectionReference deviceListRef;

    // Other UI elements and variables
    private EditText deviceId;
    private EditText deviceOTP;
    private EditText deviceName;
    private RadioGroup radioGroup;
    private RadioButton radioButton;
    private Button saveButton;
    private Dialog dialog;
    private static final String TAG = "DevicesFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devices, container, false);

        // Initialize Firebase authentication and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Get current user's email
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String userEmail = currentUser.getEmail();

        // Reference to 'devices' and 'deviceList' collections
        devicesRef = firestore.collection("devices");
        deviceListRef = firestore.collection("deviceList");

        ListView listView = rootView.findViewById(R.id.listView);
        ImageButton addDevice = rootView.findViewById(R.id.addDevice);

        // Query 'deviceList' collection for devices associated with the current user
        Query userDevicesQuery = deviceListRef
                .whereEqualTo("email", userEmail);

        // Execute query
        userDevicesQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<String> devicesList = new ArrayList<>();
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    String deviceType = documentSnapshot.getString("deviceType");
                    String deviceName = documentSnapshot.getString("deviceName");
                    String deviceId = documentSnapshot.getString("deviceId");
                    devicesList.add(deviceName + "\n" + deviceType + "\n" + deviceId );
                }
                CustomAdapter adapter = new CustomAdapter(getContext(), R.layout.item_document, devicesList, userEmail);
                listView.setAdapter(adapter);
                listView.setVisibility(View.VISIBLE); // Ensure ListView is visible

            } else {
                Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        addDevice.setOnClickListener(v -> openDeviceRegistrationDialog());

        return rootView;
    }


    public static class CustomAdapter extends ArrayAdapter<String> {

        private String userEmail;

        public CustomAdapter(Context context, int resource, List<String> objects, String userEmail) {
            super(context, resource, objects);
            this.userEmail = userEmail;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_document, parent, false);
            }

            TextView textDeviceType = convertView.findViewById(R.id.textDeviceType);
            TextView textDeviceName = convertView.findViewById(R.id.textDeviceName);
            ImageView iconDevice = convertView.findViewById(R.id.iconDevice);
            ImageView iconDelete = convertView.findViewById(R.id.iconDelete);

            String item = getItem(position);
            if (item != null) {
                // Split the item to get device type and name
                String[] deviceInfo = item.split("\n");

                // Set device type text
                textDeviceType.setText(deviceInfo[1] + ": " + deviceInfo[2]); // Index 1 contains device type

                // Set device name text
                textDeviceName.setText(deviceInfo[0]); // Index 0 contains device name

                // Set icon based on device type
                if (deviceInfo[1].equalsIgnoreCase("watch")) {
                    iconDevice.setImageResource(R.drawable.baseline_watch_24);
                } else if (deviceInfo[1].equalsIgnoreCase("camera")) {
                    iconDevice.setImageResource(R.drawable.baseline_camera_indoor_24);
                }

                // listener for delete icon
                iconDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Call a method to handle device deletion
                        DeviceDel.deleteDevice(deviceInfo[0], deviceInfo[1], userEmail);
                        Toast.makeText(getContext(), "Deleted device successfully ", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return convertView;
        }

    }

    private void openDeviceRegistrationDialog() {
        dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_device_reg);

        // Find views inside the dialog layout
        deviceId = dialog.findViewById(R.id.device_id);
        deviceOTP = dialog.findViewById(R.id.device_OTP);
        deviceName = dialog.findViewById(R.id.device_name);
        radioGroup = dialog.findViewById(R.id.radio);
        saveButton = dialog.findViewById(R.id.save_button);

        // Close button setup
        ImageView closeButton = dialog.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Save button setup
        saveButton.setOnClickListener(view -> {
            if (isInputValid()) {
                onSaveButtonClicked();
                // Dismiss dialog
                dialog.dismiss();
            } else {
                showToast("Please fill out all the fields");
            }
        });

        // Set dialog properties
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
    }

    private boolean isInputValid() {
        String inputDeviceId = deviceId.getText().toString();
        String inputDeviceOTP = deviceOTP.getText().toString();
        String inputDeviceName = deviceName.getText().toString();
        int selectedId = radioGroup.getCheckedRadioButtonId();

        // Check if any field is empty
        return !(inputDeviceId.isEmpty() || inputDeviceOTP.isEmpty() || inputDeviceName.isEmpty() || selectedId == -1);
    }

    private void onSaveButtonClicked() {
        String inputDeviceId = deviceId.getText().toString();
        String inputDeviceOTP = deviceOTP.getText().toString();
        String inputDeviceName = deviceName.getText().toString();
        int selectedId = radioGroup.getCheckedRadioButtonId();

        // Check if radio button is selected
        if (selectedId != -1) {
            radioButton = dialog.findViewById(selectedId);
            if (radioButton == null) {
                Log.e(TAG, "Error: Radio Button");
                return;
            }
        }

        // Obtain the device type from radio button and convert to string
        String inputDeviceType = radioButton.getText().toString();

        // Perform a check if the device ID and type match with the generated device ID from the wearable
        checkDevIdType(inputDeviceId, inputDeviceOTP, inputDeviceType);
    }



    private void checkDevIdType(String inputDeviceId, String inputDeviceOTP, String inputDeviceType) {
        FirebaseFirestore firebasedb = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> deviceIdTypeQuery = devicesRef
                .whereEqualTo("deviceId", inputDeviceId)
                .whereEqualTo("deviceOTP", inputDeviceOTP)
                .whereEqualTo("deviceType", inputDeviceType)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String documentId = document.getId(); // Get the document ID
                            DocumentReference docRef = firebasedb.collection("devices").document(documentId);

                            // Update the "OTP" field to null
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("deviceOTP", null);

                            // Perform the update
                            docRef.update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Document ID: " + documentId + ", OTP updated to null successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating Document ID: " + documentId + ", OTP to null", e);
                                    });
                        }
                        handleDevIdType(task.getResult());
                    } else {
                        logError("Error getting documents", task.getException());
                    }
                });
    }



    private void handleDevIdType(QuerySnapshot querySnapshot) {
        if (querySnapshot != null && !querySnapshot.isEmpty()) {
            // If snapshot exists, check if device ID already registered under other user
            checkDevDup();
        } else {
            // If snapshot does not exist, show Toast with expected vs. inputted Device ID
            showToast("Incorrect input, please check again.\n");
        }
    }


    private void checkDevDup() {
        Task<QuerySnapshot> deviceDup = deviceListRef
                .whereEqualTo("deviceId", deviceId.getText().toString())
                .whereEqualTo("deviceType", radioButton.getText().toString())
                .get()
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        handleDevDup(task1.getResult());
                    } else {
                        logError("Error getting documents", task1.getException());
                    }
                });
    }

    private void handleDevDup(QuerySnapshot querySnapshot) {
        if (querySnapshot != null && !querySnapshot.isEmpty()) {
            showToast("Existing registered device. Kindly check Device ID and Device Type.");
        } else {
            registerNewDevice();
        }
    }

    private void registerNewDevice() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            Map<String, Object> newDevice = new HashMap<>();
            newDevice.put("email", userEmail);
            newDevice.put("deviceId", deviceId.getText().toString());
            newDevice.put("deviceType", radioButton.getText().toString());
            newDevice.put("deviceName", deviceName.getText().toString());
            // Audit Log
            String devIdForAudit = deviceId.getText().toString();
            String devNameForAudit = deviceName.getText().toString();
            AuditLog.logAction("Device " + devIdForAudit + " registered as NAME: " + devNameForAudit);

            deviceListRef.add(newDevice)
                    .addOnSuccessListener(documentReference -> {
                        showToast("Device registered successfully.");
                    })
                    .addOnFailureListener(e -> logError("Error adding document", e));
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void logError(String message, Exception exception) {
        Log.e(TAG, message, exception);
        showToast("Error: " + exception.getMessage());
    }

}
