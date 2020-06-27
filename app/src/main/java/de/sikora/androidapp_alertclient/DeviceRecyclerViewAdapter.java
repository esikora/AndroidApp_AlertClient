package de.sikora.androidapp_alertclient;

import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.sikora.androidapp_alertclient.DeviceListFragment.ListInteractionListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link AlertDeviceModel} and makes a call to the
 * specified {@link DeviceListFragment.ListInteractionListener}.
 */
public class DeviceRecyclerViewAdapter
        extends RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder> {

    // List of alert devices to be shown in the RecyclerView
    private final List<AlertDeviceModel> alertDeviceList;

    private final ListInteractionListener listener;

    private AlertDeviceModel selectedDevice;
    private int selectedPos;

    public DeviceRecyclerViewAdapter(List<AlertDeviceModel> initialDevices,
                                     ListInteractionListener listener) {
        alertDeviceList = new ArrayList<>();
        if (initialDevices != null) {
            alertDeviceList.addAll(initialDevices);
        }

        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Log.i("ViewAdapter", "onBindViewHolder for position "+ position);

        AlertDeviceModel device = alertDeviceList.get(position);
        holder.device = device;
        holder.nrTextView.setText(Integer.toString(position + 1));
        holder.addrTextView.setText(device.getAddress());
        holder.nameTextView.setText(device.getName());
        holder.idTextView.setText(device.getId());

        if (device.isRegistered()) {
            holder.regStateView.setVisibility(View.VISIBLE);
        }
        else {
            holder.regStateView.setVisibility(View.INVISIBLE);
        }

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int newSelectedPos = holder.getAdapterPosition();

                // Is there a previous selection?
                if (null == selectedDevice) { // No item selected previously
                    // Store selected device and position for later un-highlighting
                    selectedDevice = holder.device;
                    selectedPos = newSelectedPos;

                    // Update highlighted item
                    notifyItemChanged(selectedPos);
                }
                else { // Item selected previously
                    int oldSelectedPos = selectedPos;

                    // Did the user click again the previously selected item?
                    if (selectedDevice == holder.device) {
                        // Clear last clicked item data
                        selectedDevice = null;
                        selectedPos = -1;

                    }
                    else {
                        // Store new selected device and position for later un-highlighting
                        selectedDevice = holder.device;
                        selectedPos = newSelectedPos;

                        // Update newly highlighted item
                        notifyItemChanged(newSelectedPos);
                    }
                    // Un-highlighted previous item
                    notifyItemChanged(oldSelectedPos);
                }

                if (null != listener) {

                    // Notify the listener
                    listener.onListItemInteraction(holder.device, selectedDevice != null);
                }
            }
        });

        if (holder.device == selectedDevice) {
            Drawable d = holder.view.getResources().getDrawable(R.drawable.shape_listitem_selected, null);
            holder.view.setBackground(d);
        }
        else {
            Drawable d = holder.view.getResources().getDrawable(R.drawable.shape_listitem, null);
            holder.view.setBackground(d);
        }
    }

    @Override
    public int getItemCount() {
        return alertDeviceList.size();
    }

    public void addDevice(AlertDeviceModel device) {
        alertDeviceList.add(device);
        notifyDataSetChanged();
    }

    public void update() {
        notifyDataSetChanged();
    }

    public void clear() {
        alertDeviceList.clear();
        selectedDevice = null;
        selectedPos = -1;
        notifyDataSetChanged();
    }

    public AlertDeviceModel getSelectedDevice() {
        return selectedDevice;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View view;

        private final TextView nrTextView;
        private final TextView addrTextView;
        private final TextView nameTextView;
        private final TextView idTextView;
        private final ImageView regStateView;

        private AlertDeviceModel device;

        ViewHolder(View view) {
            super(view);
            this.view = view;

            nrTextView = view.findViewById(R.id.item_number);
            addrTextView = view.findViewById(R.id.textview_listitem_device_address);
            nameTextView = view.findViewById(R.id.textview_listitem_device_name);
            idTextView = view.findViewById(R.id.textview_listitem_device_id);
            regStateView = view.findViewById(R.id.imageview_listitem_registered);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + addrTextView.getText() + "'";
        }
    }
}
