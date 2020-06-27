package de.sikora.androidapp_alertclient;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Vector;

/**
 * Allows the user to monitor and control alert devices.
 */
public class AlertClientFragment extends Fragment implements AlertDeviceModel.AlertStateListener {

    // Time during which the received alert level is ignored after clicking on the alarm button
    private static final int TIME_HOLD_STATE_AFTER_CLICK = 500;

    // Tag for logging
    private static final String TAG = AlertClientFragment.class.getName();

    // Pool that manages AlertClientTcp objects
    private AlertClientPool alertClientPool;

    // Alert device object whose state is shown and manipulated by the UI
    private AlertDeviceModel device;

    private TextView deviceNameTextView;

    private TextView deviceAddressView;

    private TextView deviceConnStateView;

    private ToggleButton toggleAlertButton;

    // Time of last click on the alarm toggle button
    private long timeLastClick;


    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @param context
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        Log.i(TAG, "onCreateView");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_alert_client, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onViewCreated");

        // Retrieve UI elements
        try {
            deviceNameTextView = getView().findViewById(R.id.textview_device_name);
            deviceAddressView = getView().findViewById(R.id.textview_device_address);
            deviceConnStateView = getView().findViewById(R.id.textview_connection_state);
            toggleAlertButton = getView().findViewById(R.id.togglebutton_alert);

            // Initial setup of the connection state label
            int c = getResources().getColor(R.color.design_default_color_error, null);
            deviceConnStateView.setTextColor(c);

            // This object as source of alert state changes
            final AlertDeviceModel.AlertStateListener alertEventSource = this;

            // Listen to change of alert level
            toggleAlertButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    timeLastClick = System.currentTimeMillis();

                    if (((ToggleButton) view).isChecked()) {
                        // Set the alert level and notify the associated alert client
                        device.setAlertLevelAndNotify(1, alertEventSource);
                    } else {
                        // Set the alert level directly so that UI can be updated immediately
                        device.setAlertLevelAndNotify(0, alertEventSource);
                    }

                    updateUI();
                }
            });

        }
        catch (NullPointerException e) {
            Log.w(TAG, "Failure while setting up user interface.", e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");

        // Get an instance of AlertClientPool
        alertClientPool = AlertClientPool.getInstance();

        updateDevices();
        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    /**
     * This method is called when an alert level is received from the server.
     *
     * @param alertState AlertDeviceModel containing the current alert state provided by the
     *                   alert server.
     * @param ev         AlertStateEvent containing the type  and source of the event.
     */
    @Override
    public void onAlertLevelEvent(AlertDeviceModel alertState,
                                  AlertDeviceModel.AlertStateEvent ev) {
        // Ignore state changes shortly after click event
        if (System.currentTimeMillis() - timeLastClick >= TIME_HOLD_STATE_AFTER_CLICK) {

            // Update must be done from the UI Thread
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }
    }

    /**
     * This method is called when the state of the connection between this client and the alert
     * server changes.
     *
     * @param alertState AlertDeviceModel containing the current connection state, i.e. whether
     *                   this client is connected to the alert server.
     * @param ev         AlertStateEvent containing the type  and source of the event.
     */
    @Override
    public void onConnectionStateEvent(AlertDeviceModel alertState,
                                       AlertDeviceModel.AlertStateEvent ev) {
        // Update must be done from the UI Thread
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }

    private void updateDevices() {
        // Get list of devices from the AlertClientPool
        Vector<AlertDeviceModel> deviceList = alertClientPool.getDeviceList();

        if (!deviceList.isEmpty()) {
            device = deviceList.firstElement();

            // Register this fragment as listener for alert state changes
            device.addListener(this);

            // Start communication with the device (if not yet started)
            alertClientPool.startClient(device);
        }
        else {
            device = null;
        }
    }

    private void disconnectDevices() {
        // Unregister from notifications about alert state changes
        if (device != null) {
            device.removeListener(this);
        }

        // Stop connections and close communication
        alertClientPool.stopAllClients();
    }

    private void updateUI() {

        if (device == null) {
            int c1 = getResources().getColor(R.color.color_foreground_disabled, null);
            //int c2 = getResources().getColor(R.color.color_control_unavailable, null);

            deviceNameTextView.setText(R.string.label_device_name_na);
            //deviceNameTextView.setTextColor(c1);

            deviceAddressView.setText(R.string.label_device_address_na);
            //deviceAddressView.setTextColor(c1);

            deviceConnStateView.setText(R.string.label_connection_nok);
            deviceConnStateView.setTextColor(c1);

            toggleAlertButton.setEnabled(false);
            toggleAlertButton.setChecked(false);
            //toggleAlertButton.setTextColor(c1);
            //toggleAlertButton.setBackgroundColor(c2);

        }
        else {
            int c1 = getResources().getColor(R.color.color_foreground_enabled, null);

            deviceNameTextView.setText(device.getName());
            //deviceNameTextView.setTextColor(c1);

            deviceAddressView.setText(device.getAddress());
            //deviceAddressView.setTextColor(c1);

            //toggleAlertButton.setTextColor(c1);

            boolean connected = device.isConnected();
            boolean alertOn = device.getAlertLevel() > 0;

            if (connected) {
                // Connection state view: ok
                deviceConnStateView.setText(R.string.label_connection_ok);
                int c2 = getResources().getColor(R.color.color_text_state_ok, null);
                deviceConnStateView.setTextColor(c2);

                // Toggle alert button: clickable
                toggleAlertButton.setEnabled(true);

                if (alertOn) {
                    // Toggle alert button: alert is active
                    toggleAlertButton.setChecked(true);
                    //int c3 = getResources().getColor(R.color.color_background_active, null);
                    //toggleAlertButton.setBackgroundColor(c3);
                } else {
                    toggleAlertButton.setChecked(false);
                    //int c3 = getResources().getColor(R.color.color_control_off, null);
                    //toggleAlertButton.setBackgroundColor(c3);
                }
            } else {
                // No connection
                deviceConnStateView.setText(R.string.label_connection_nok);
                int c2 = getResources().getColor(R.color.color_text_state_nok, null);
                deviceConnStateView.setTextColor(c2);

                // Unknown alert state, alert cannot be toggled
                toggleAlertButton.setEnabled(false);
                toggleAlertButton.setChecked(false);
                //int c3 = getResources().getColor(R.color.color_control_unavailable, null);
                //toggleAlertButton.setBackgroundColor(c3);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");

        disconnectDevices();
    }

    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "onDestroyView");
    }

    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach");
    }

}