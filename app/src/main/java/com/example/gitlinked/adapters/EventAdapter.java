package com.example.gitlinked.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gitlinked.R;
import com.example.gitlinked.models.Event;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private final Context context;
    private List<Event> events;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event, int position);
        void onCheckInClick(Event event, int position);
    }

    public EventAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvTitle.setText(event.getTitle());
        holder.tvDescription.setText(event.getDescription());
        holder.tvLocation.setText("📍 " + event.getLocation());
        holder.tvDate.setText("📅 " + event.getDate());
        String attendeeText = event.getAttendees() + " attending";
        if (event.getMaxAttendees() > 0) {
            attendeeText = event.getAttendees() + "/" + event.getMaxAttendees() + " attending";
        }
        holder.tvAttendees.setText(attendeeText);
        holder.tvOrganizer.setText("By " + event.getOrganizer());

        // Check-in button state
        if (event.isCheckedIn()) {
            holder.btnCheckIn.setText("✓ Checked In");
            holder.btnCheckIn.setEnabled(false);
            holder.btnCheckIn.setAlpha(0.6f);
        } else {
            holder.btnCheckIn.setText("Check In");
            holder.btnCheckIn.setEnabled(true);
            holder.btnCheckIn.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event, position);
        });

        holder.btnCheckIn.setOnClickListener(v -> {
            if (listener != null) listener.onCheckInClick(event, position);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateData(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvLocation, tvDate, tvAttendees, tvOrganizer;
        Button btnCheckIn;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvDescription = itemView.findViewById(R.id.tv_event_description);
            tvLocation = itemView.findViewById(R.id.tv_event_location);
            tvDate = itemView.findViewById(R.id.tv_event_date);
            tvAttendees = itemView.findViewById(R.id.tv_attendees);
            tvOrganizer = itemView.findViewById(R.id.tv_organizer);
            btnCheckIn = itemView.findViewById(R.id.btn_check_in);
        }
    }
}
