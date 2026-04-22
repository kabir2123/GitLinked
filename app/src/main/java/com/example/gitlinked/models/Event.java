package com.example.gitlinked.models;

public class Event {
    private long id;
    private String title;
    private String description;
    private String location;
    private String date;
    private int attendees;
    private int maxAttendees;
    private String organizer;
    private boolean isCheckedIn;
    private double latitude;
    private double longitude;

    public Event() {}

    public Event(long id, String title, String description, String location,
                 String date, int attendees, String organizer) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.date = date;
        this.attendees = attendees;
        this.organizer = organizer;
        this.isCheckedIn = false;
        this.maxAttendees = 0; // 0 = unlimited
    }

    public Event(long id, String title, String description, String location,
                 String date, int attendees, int maxAttendees, String organizer) {
        this(id, title, description, location, date, attendees, organizer);
        this.maxAttendees = maxAttendees;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getAttendees() { return attendees; }
    public void setAttendees(int attendees) { this.attendees = attendees; }

    public int getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(int maxAttendees) { this.maxAttendees = maxAttendees; }

    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }

    public boolean isCheckedIn() { return isCheckedIn; }
    public void setCheckedIn(boolean checkedIn) { isCheckedIn = checkedIn; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isFull() {
        return maxAttendees > 0 && attendees >= maxAttendees;
    }
}
