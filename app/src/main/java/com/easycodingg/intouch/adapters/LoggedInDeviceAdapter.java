package com.easycodingg.intouch.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easycodingg.intouch.databinding.ItemLoggedInDeviceBinding;
import com.easycodingg.intouch.models.LoggedInDevice;
import com.easycodingg.intouch.utils.CommonMethods;
import com.easycodingg.intouch.utils.events.LoggedInDeviceItemClickEvent;

import java.util.Date;
import java.util.List;

public class LoggedInDeviceAdapter extends RecyclerView.Adapter<LoggedInDeviceAdapter.LoggedInDeviceViewHolder> {

    public List<LoggedInDevice> deviceList;
    private LoggedInDeviceItemClickEvent clickEvent;

    public LoggedInDeviceAdapter(List<LoggedInDevice> deviceList, LoggedInDeviceItemClickEvent clickEvent) {
        this.deviceList = deviceList;
        this.clickEvent = clickEvent;
    }

    @NonNull
    @Override
    public LoggedInDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoggedInDeviceBinding binding = ItemLoggedInDeviceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LoggedInDeviceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LoggedInDeviceViewHolder holder, int position) {
        LoggedInDevice device = deviceList.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() { return deviceList.size(); }

    public class LoggedInDeviceViewHolder extends RecyclerView.ViewHolder {
        private ItemLoggedInDeviceBinding binding;

        public LoggedInDeviceViewHolder(ItemLoggedInDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.imgLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(clickEvent != null) {
                        LoggedInDevice device = deviceList.get(getBindingAdapterPosition());
                        clickEvent.onLogoutButtonCLick(device);
                    }
                }
            });
        }

        public void bind(LoggedInDevice device) {
            binding.txtDeviceName.setText(device.deviceName);

            Date lastActive = device.lastActive != null ? device.lastActive : device.loggedInTime;
            String lastActiveString = "Last Active: " + CommonMethods.getLastSeenTime(lastActive);

            binding.txtLastActive.setText(lastActiveString);
        }
    }
}
