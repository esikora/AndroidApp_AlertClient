package de.sikora.androidapp_alertclient;

import java.util.LinkedList;
import java.util.List;

/**
 * Representation of a remote alert device.
 */
public class AlertDeviceModel {

    public static String DEVICE_NAME_DEFAULT = "Alert device";

    // Name of the alert device, e.g. assigned by the user
    private String deviceName = DEVICE_NAME_DEFAULT;

    // Unique Id of the device
    private String deviceId;

    // Remote IP address of the alert device
    private String deviceAddr;

    // Remote port of the alert service
    private int devicePort;

    // Current alert level set by the user or provided by the alert device
    private int alertLevel;

    // Flag that this device is managed by the device pool of the application
    private boolean registered;

    // Connection state
    private boolean connected;

    // Listeners
    private List<AlertStateListener> listenerList;


    public AlertDeviceModel() {
        listenerList = new LinkedList<>();
    }

    public void setAlertLevel(int alertLevel) {
        this.alertLevel = alertLevel;
    }

    public void setAlertLevelAndNotify(int alertLevel, AlertStateListener source) {
        this.alertLevel = alertLevel;

        notifyListeners( new AlertStateEvent(AlertEventType.ALERT_LEVEL_EVENT, source) );
    }

    public int getAlertLevel() {
        return alertLevel;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setConnectedAndNotify(boolean connected, AlertStateListener source) {
        this.connected = connected;

        notifyListeners( new AlertStateEvent(AlertEventType.CONNECTION_STATE_EVENT, source) );
    }

    public String getName() {
        return deviceName;
    }

    public void setName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getId() {
        return deviceId;
    }

    public void setId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAddress() {
        return deviceAddr;
    }

    public void setAddress(String deviceAddr) {
        this.deviceAddr = deviceAddr;
    }

    public int getPort() {
        return devicePort;
    }

    public void setPort(int devicePort) {
        this.devicePort = devicePort;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public void addListener(AlertStateListener listener) {
        if ( !listenerList.contains(listener) ) {
            listenerList.add(listener);
        }
    }

    public void removeListener(AlertStateListener listener) {
        listenerList.remove(listener);
    }

    public void notifyListeners(AlertStateEvent ev) {
        for (AlertStateListener listener: listenerList) {
            // Notify listeners except the event source itself
            if (listener != ev.eventSource) {
                switch(ev.eventType) {
                    case ALERT_LEVEL_EVENT:
                        listener.onAlertLevelEvent(this, ev);
                        break;

                    case CONNECTION_STATE_EVENT:
                        listener.onConnectionStateEvent(this, ev);
                        break;
                }
            }
        }
    }

    public enum AlertEventType {
        ALERT_LEVEL_EVENT,
        CONNECTION_STATE_EVENT
    }

    public static class AlertStateEvent {

        private AlertEventType eventType;
        private AlertStateListener eventSource;

        public AlertStateEvent(AlertEventType type, AlertStateListener source) {
            this.eventType = type;
            this.eventSource = source;
        }
    }

    /**
     * Interface that is used to inform a registered listener about alert state changes.
     */
    public interface AlertStateListener {

        /**
         * This method is called when an alert level is received from the server.
         *
         * @param alertState AlertDeviceModel containing the current alert state provided by the
         *                   alert server.
         *
         * @param ev AlertStateEvent containing the type  and source of the event.
         */
        public void onAlertLevelEvent(AlertDeviceModel alertState, AlertStateEvent ev);

        /**
         * This method is called when the state of the connection between this client and the alert
         * server changes.
         *
         * @param alertState AlertDeviceModel containing the current connection state, i.e. whether
         *                   this client is connected to the alert server.
         *
         * @param ev AlertStateEvent containing the type  and source of the event.
         */
        public void onConnectionStateEvent(AlertDeviceModel alertState, AlertStateEvent ev);
    }

}
