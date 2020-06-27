package de.sikora.androidapp_alertclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Vector;


/**
 * Allows the user choose alert devices that are available in the local network.
 */
public class DeviceScanActivity extends AppCompatActivity
        implements AlertDeviceScanTcp.AlertDeviceScanListener,
        DeviceListFragment.ListInteractionListener {

    // Tag for logging
    private static final String TAG = DeviceScanActivity.class.getName();

    private DeviceRecyclerViewAdapter deviceViewAdapter;

    private ToggleButton scanButton;

    private Button addDeviceButton;

    private Button removeDeviceButton;

    private TextInputEditText deviceNameInput;

    private AlertDeviceModel selectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        Toolbar toolbar = findViewById(R.id.toolbar_alert_device_finder);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Finish activity when user clicks back navigation arrow
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Configure input field for device labels
        deviceNameInput = findViewById(R.id.input_device_name);
        deviceNameInput.setEnabled(false);

        // Configure 'add' button
        addDeviceButton = findViewById(R.id.button_add_device);

        addDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerSelectedDevice();
            }
        });

        addDeviceButton.setEnabled(false);

        // Configure 'remove' button
        removeDeviceButton = findViewById(R.id.button_remove_device);

        removeDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unregisterSelectedDevice();
            }
        });

        removeDeviceButton.setEnabled(false);

        // Configure the 'scan' button
        scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });

        // Initialize the device list with registered devices
        initializeDeviceList();
    }

    private void initializeDeviceList() {
        // Initialize list with devices from pool
        Vector<AlertDeviceModel> deviceList = AlertClientPool.getInstance().getDeviceList();

        for (AlertDeviceModel device : deviceList) {
            deviceViewAdapter.addDevice(device);
        }
    }

    private void startScan() {
        // Do not allow to start another scan while scanning
        scanButton.setClickable(false);

        disableDeviceEdit();

        // Retain only registered devices in the list
        deviceViewAdapter.clear();

        initializeDeviceList();

        // Start the scan process in the background
        AlertClientPool.getInstance().startScanAndListen(this);
    }

    private void enableScan() {
        // Uncheck and deselect the scan toggle button
        scanButton.setSelected(false);
        scanButton.setChecked(false);

        // Allow user to start a new scan process
        scanButton.setClickable(true);
    }

    private void registerSelectedDevice() {
        if (selectedDevice != null) {
            // Set device name from user input
            TextInputEditText deviceNameInput = findViewById(R.id.input_device_name);
            selectedDevice.setName(deviceNameInput.getText().toString());

            // Add device to alert client pool
            AlertClientPool.getInstance().addDevice(selectedDevice);

            removeDeviceButton.setEnabled(true);

            // Update list
            deviceViewAdapter.update();
        }
    }

    private void unregisterSelectedDevice() {
        if (selectedDevice != null) {
            if (selectedDevice.isRegistered()) {
                // Remove device from alert client pool
                AlertClientPool.getInstance().removeDeviceByAddr(selectedDevice);

                removeDeviceButton.setEnabled(false);

                // Update list
                deviceViewAdapter.update();
            }
        }
    }

    private void enableDeviceEdit(AlertDeviceModel device) {
        selectedDevice = device;
        TextInputEditText deviceNameInput = findViewById(R.id.input_device_name);
        deviceNameInput.setText(device.getName());
        deviceNameInput.setEnabled(true);

        addDeviceButton.setEnabled(true);

        if (selectedDevice.isRegistered()) {
            removeDeviceButton.setEnabled(true);
        }
    }

    private void disableDeviceEdit() {
        selectedDevice = null;

        deviceNameInput.setText("");
        deviceNameInput.setEnabled(false);

        addDeviceButton.setEnabled(false);
        removeDeviceButton.setEnabled(false);
    }


    /**
     * Called from the background scan process when a device has been found.
     * @param device
     */
    @Override
    public void onDeviceFound(final AlertDeviceModel device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceViewAdapter.addDevice(device);
            }
        });
    }

    /**
     * Called from the background scan process when it is finished.
     */
    @Override
    public void onScanFinished() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                enableScan();
            }
        });
    }

    /**
     * Called by the recycler list view when the view adapter is created.
     * @param viewAdapter The viewAdapter the manages the content of the list view
     */
    @Override
    public void onViewAdapterCreated(DeviceRecyclerViewAdapter viewAdapter) {
        deviceViewAdapter = viewAdapter;
    }

    /**
     * Called by the recycler list view adapter when user interacts with a list item.
     * @param device The device represented by the list item.
     * @param isSelected True, if the item is selected.
     */
    @Override
    public void onListItemInteraction(AlertDeviceModel device, boolean isSelected) {
        if (isSelected) {
            enableDeviceEdit(device);
        }
        else {
            disableDeviceEdit();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");

        AlertClientPool.getInstance().stopScan();
    }
}
