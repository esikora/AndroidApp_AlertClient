package de.sikora.androidapp_alertclient;

import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages a set of alert client devices.
 */
public class AlertClientPool implements AlertDeviceScanTcp.AlertDeviceScanListener {

    // Port of the Alert Server to which this client connects to
    public static final int ALERT_SERVER_DEFAULT_PORT = 12321;

    private static String networkAdrStr = "192.168.17";

    private static int nodeAdrFirst = 2;
    private static int nodeAdrLast = 254;

    private static int remotePort = ALERT_SERVER_DEFAULT_PORT;

    private static AlertClientPool instance = null;

    private Vector<AlertDeviceScanTcp> scannerList;

    private AtomicBoolean scanActive;

    private AlertDeviceScanTcp.AlertDeviceScanListener scanResultListener;

    private List<AlertClientTcp> alertClients;


    public static AlertClientPool getInstance() {
        if (instance == null) {
            instance = new AlertClientPool();
        }

        return instance;
    }

    /**
     * Private constructor that is used to create a singleton instance by the
     * <code>getInstance</code> method.
     */
    private AlertClientPool() {
        alertClients = new Vector<>();
        scanActive = new AtomicBoolean(false);
        scannerList = new Vector<>();
    }

    public void startScanAndListen(AlertDeviceScanTcp.AlertDeviceScanListener listener) {

        if ( scanActive.compareAndSet(false, true) ) {
            try {
                // Create the new AlertDeviceScanTcp object, register the listener for scan results
                if (scannerList.isEmpty()) {
                    AlertDeviceScanTcp scanner = new AlertDeviceScanTcp(networkAdrStr,
                            nodeAdrFirst, nodeAdrLast, remotePort);

                    scanner.addListener(this);
                    scannerList.add(scanner);
                }

                scanResultListener = listener;

                for (AlertDeviceScanTcp scanner:scannerList) {
                    if (!scanner.isRunning()) {
                        // Execute the scanning task in the background
                        new Thread(scanner).start();
                    }
                }
            }
            catch (Exception e) {
                // Stop all active scan threads when something goes wrong
                stopScan();
            }
        }
    }

    @Override
    public void onDeviceFound(AlertDeviceModel device) {
        if (scanResultListener != null) {
            scanResultListener.onDeviceFound(device);
        }
    }

    @Override
    public void onScanFinished() {

        boolean allFinished = true;

        for (AlertDeviceScanTcp scanner:scannerList) {
            if (scanner.isRunning()) {
                allFinished = false;
            }
        }

        if (allFinished) {
            scanActive.set(false);

            if (scanResultListener != null) {
                scanResultListener.onScanFinished();
                scanResultListener = null;
            }
        }
    }

    public void stopScan() {
        try {
            AlertDeviceScanTcp.AlertDeviceScanListener storedListener = scanResultListener;
            scanResultListener = null;

            for (AlertDeviceScanTcp scanner : scannerList) {
                if (scanner.isRunning()) {
                    scanner.stopScan();
                }
            }

            if (storedListener != null) {
                storedListener.onScanFinished();
            }
        }
        finally {
            scanActive.set(false);
        }
    }

    public Vector<AlertDeviceModel> getDeviceList() {
        Vector<AlertDeviceModel> deviceList = new Vector<>(alertClients.size());

        // Retrieve all devices and add to list
        Iterator<AlertClientTcp> it = alertClients.iterator();

        while (it.hasNext()) {
            AlertClientTcp client = it.next();
            AlertDeviceModel device = client.getAlertDevice();

            deviceList.add(device);
        }

        return deviceList;
    }

    /**
     * Adds a new alert device to the pool without starting a client thread.
     * Hence, no connection is established to the device.
     *
     * If a device with same address is already in the pool, the connection to this device is
     * closed and that device is removed from the pool before the new one is added.
     *
     * @param alertDevice Alert device to be added.
     */
    public void addDevice(AlertDeviceModel alertDevice) {

        // Check if there already is an AlertClientTcp object for the device address
        Iterator<AlertClientTcp> it = alertClients.iterator();

        while (it.hasNext()) {
            AlertClientTcp client = it.next();
            AlertDeviceModel device = client.getAlertDevice();

            // Check if the new device has the same address as the device from the pool
            if ( device.getAddress().equals(alertDevice.getAddress()) ) {
                // Shutdown the client
                client.shutdown();

                // Remove the previous client and device from the pool
                it.remove();

                // Clear 'registered' flag of the removed device
                device.setRegistered(false);
            }
        }

        // Create the new AlertClientTcp object for the specified device
        AlertClientTcp alertClientTcp = new AlertClientTcp(alertDevice);

        // Add the new AlertClientTcp object to the pool
        alertClients.add(alertClientTcp);

        // Set 'registered' flag of the new device
        alertDevice.setRegistered(true);
    }

    public void removeDeviceByAddr(AlertDeviceModel alertDevice) {
        // Check if there already is an AlertClientTcp object for the device address
        Iterator<AlertClientTcp> it = alertClients.iterator();

        while (it.hasNext()) {
            AlertClientTcp client = it.next();
            AlertDeviceModel device = client.getAlertDevice();

            // Shutdown the previous client and remove it from the pool
            if ( device.getAddress().equals(alertDevice.getAddress()) ) {
                if (client.isRunning()) {
                    client.shutdown();
                }
                device.setRegistered(false);
                it.remove();
            }
        }
    }

    /**
     * Starts a new thread the runs the AlertClientTcp object for a specified device.
     * Thereby a connection to the device is established and communication with the device is
     * possible.
     *
     * @param alertDevice Alert device
     */
    public void startClient(AlertDeviceModel alertDevice) {

        // Find the AlertClientTcp object for the device address
        for (AlertClientTcp client:alertClients) {
            AlertDeviceModel device = client.getAlertDevice();

            if ( device.getAddress().equals(alertDevice.getAddress()) ) {
                // Start a thread for this device
                if (!client.isRunning()) {
                    new Thread(client).start();
                }
                break;
            }
        }
    }

    public void stopClient(AlertDeviceModel alertDevice) {
        // Find the AlertClientTcp object for the device address
        for (AlertClientTcp client:alertClients) {
            AlertDeviceModel device = client.getAlertDevice();

            if ( device.getAddress().equals(alertDevice.getAddress()) ) {
                // Start a thread for this device
                if (client.isRunning()) {
                    client.shutdown();
                }
                break;
            }
        }
    }

    /**
     * Starts a new client for an alert device and adds it to the pool.
     *
     * @param alertDevice Alert device to which the client shall connect.
     */
    public void addDeviceAndStartClient(AlertDeviceModel alertDevice) {
        addDevice(alertDevice);
        startClient(alertDevice);
    }

    /**
     * Closes the connections of all clients, stops their threads, and keeps them in the pool so
     * that they can be restarted.
     */
    public void stopAllClients() {
        Iterator<AlertClientTcp> it = alertClients.iterator();

        while (it.hasNext()) {
            AlertClientTcp client = it.next();
            client.shutdown();
        }
    }

    /**
     * Starts new threads for all devices in the pool that are not active.
     */
    public void startAllClients() {
        Iterator<AlertClientTcp> it = alertClients.iterator();

        while (it.hasNext()) {
            AlertClientTcp client = it.next();

            if (!client.isRunning()) {
                new Thread(client).start();
            }
        }
    }

    /**
     * Closes the connections to all devices, stops their client threads, and removes them from
     * the pool.
     */
    public void stopAllClientsAndRemove() {
        Iterator<AlertClientTcp> it = alertClients.iterator();

        while (it.hasNext()) {
            AlertClientTcp client = it.next();

            if (client.isRunning()) {
                client.shutdown();
            }

            client.getAlertDevice().setRegistered(false);

            it.remove();
        }
    }

}