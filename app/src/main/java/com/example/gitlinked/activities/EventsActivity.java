package com.example.gitlinked.activities;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gitlinked.R;
import com.example.gitlinked.adapters.EventAdapter;
import com.example.gitlinked.database.JobDao;
import com.example.gitlinked.models.Event;
import com.example.gitlinked.utils.Constants;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays tech events with check-in functionality.
 * Users can also create their own custom events.
 */
public class EventsActivity extends AppCompatActivity {

    private RecyclerView recyclerEvents;
    private EventAdapter adapter;
    private List<Event> events = new ArrayList<>();
    private JobDao jobDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        jobDao = new JobDao(this);

        recyclerEvents = findViewById(R.id.recycler_events);
        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EventAdapter(this, events);
        recyclerEvents.setAdapter(adapter);

        adapter.setOnEventClickListener(new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event, int position) {
                // Show event details
                showEventDetails(event);
            }

            @Override
            public void onCheckInClick(Event event, int position) {
                if (!event.isCheckedIn()) {
                    if (event.isFull()) {
                        Toast.makeText(EventsActivity.this,
                                "Event is full! Max " + event.getMaxAttendees() + " attendees.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    jobDao.checkInEvent(event.getId());
                    event.setCheckedIn(true);
                    event.setAttendees(event.getAttendees() + 1);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(EventsActivity.this,
                            "Checked in to " + event.getTitle() + "! 🎉",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // FAB for creating new event
        ExtendedFloatingActionButton fabCreate = findViewById(R.id.fab_create_event);
        fabCreate.setOnClickListener(v -> showCreateEventDialog());

        loadEvents();
    }

    private void loadEvents() {
        events.clear();
        events.addAll(jobDao.getAllEvents());
        adapter.updateData(events);
    }

    /**
     * Show a dialog to create a custom event.
     */
    private void showCreateEventDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_event, null);

        EditText etName = dialogView.findViewById(R.id.et_event_name);
        EditText etDesc = dialogView.findViewById(R.id.et_event_desc);
        EditText etDate = dialogView.findViewById(R.id.et_event_date);
        EditText etLocation = dialogView.findViewById(R.id.et_event_location);
        EditText etMaxAttendees = dialogView.findViewById(R.id.et_max_attendees);

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("📅 Create New Event")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    String date = etDate.getText().toString().trim();
                    String location = etLocation.getText().toString().trim();
                    String maxStr = etMaxAttendees.getText().toString().trim();

                    if (name.isEmpty() || date.isEmpty()) {
                        Toast.makeText(this, "Event name and date are required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int maxAttendees = 0;
                    try {
                        if (!maxStr.isEmpty()) maxAttendees = Integer.parseInt(maxStr);
                    } catch (NumberFormatException e) {
                        maxAttendees = 0;
                    }

                    // Get current user as organizer
                    SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
                    String organizer = prefs.getString(Constants.PREF_USERNAME, "You");

                    Event newEvent = new Event(0, name, desc, location, date, 0, maxAttendees, organizer);
                    jobDao.insertEvent(newEvent);

                    Toast.makeText(this, "Event created! 🎉", Toast.LENGTH_SHORT).show();
                    loadEvents();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show event details in a dialog.
     */
    private void showEventDetails(Event event) {
        String details = "📅 " + event.getDate()
                + "\n📍 " + event.getLocation()
                + "\n\n" + event.getDescription()
                + "\n\n👥 " + event.getAttendees() + " attending"
                + (event.getMaxAttendees() > 0 ? " / " + event.getMaxAttendees() + " max" : "")
                + "\n🎤 Organized by " + event.getOrganizer();

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(event.getTitle())
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }
}
