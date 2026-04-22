package com.example.gitlinked.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.gitlinked.models.Event;
import com.example.gitlinked.models.Job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JobDao {

    private final DBHelper dbHelper;

    public JobDao(Context context) {
        this.dbHelper = DBHelper.getInstance(context);
    }

    // ======================== JOBS ========================

    public long insertJob(Job job) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_TITLE, job.getTitle());
        values.put(DBHelper.COL_COMPANY, job.getCompany());
        values.put(DBHelper.COL_DESCRIPTION, job.getDescription());
        values.put(DBHelper.COL_LOCATION, job.getLocation());
        values.put(DBHelper.COL_TYPE, job.getType());
        values.put(DBHelper.COL_SKILLS, job.getSkills() != null ? String.join(",", job.getSkills()) : "");
        values.put(DBHelper.COL_POSTED_DATE, job.getPostedDate());
        values.put(DBHelper.COL_SALARY, job.getSalary());
        return db.insert(DBHelper.TABLE_JOBS, null, values);
    }

    public List<Job> getAllJobs() {
        return getJobsByType(null);
    }

    public List<Job> getJobsByType(String type) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Job> jobs = new ArrayList<>();

        String selection = (type != null) ? DBHelper.COL_TYPE + "=?" : null;
        String[] args = (type != null) ? new String[]{type} : null;

        Cursor cursor = db.query(DBHelper.TABLE_JOBS, null,
                selection, args, null, null,
                DBHelper.COL_JOB_ID + " DESC");

        while (cursor.moveToNext()) {
            jobs.add(cursorToJob(cursor));
        }
        cursor.close();
        return jobs;
    }

    private Job cursorToJob(Cursor cursor) {
        Job job = new Job();
        job.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_JOB_ID)));
        job.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_TITLE)));
        job.setCompany(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_COMPANY)));
        job.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_DESCRIPTION)));
        job.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_LOCATION)));
        job.setType(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_TYPE)));
        String skills = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_SKILLS));
        job.setSkills(skills != null && !skills.isEmpty()
                ? new ArrayList<>(Arrays.asList(skills.split(","))) : new ArrayList<>());
        job.setPostedDate(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_POSTED_DATE)));
        job.setSalary(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_SALARY)));
        return job;
    }

    // ======================== EVENTS ========================

    public long insertEvent(Event event) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_EVENT_TITLE, event.getTitle());
        values.put(DBHelper.COL_EVENT_DESC, event.getDescription());
        values.put(DBHelper.COL_EVENT_LOCATION, event.getLocation());
        values.put(DBHelper.COL_EVENT_DATE, event.getDate());
        values.put(DBHelper.COL_ATTENDEES, event.getAttendees());
        values.put(DBHelper.COL_MAX_ATTENDEES, event.getMaxAttendees());
        values.put(DBHelper.COL_ORGANIZER, event.getOrganizer());
        values.put(DBHelper.COL_CHECKED_IN, event.isCheckedIn() ? 1 : 0);
        return db.insert(DBHelper.TABLE_EVENTS, null, values);
    }

    public List<Event> getAllEvents() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Event> events = new ArrayList<>();

        Cursor cursor = db.query(DBHelper.TABLE_EVENTS, null,
                null, null, null, null,
                DBHelper.COL_EVENT_DATE + " ASC");

        while (cursor.moveToNext()) {
            events.add(cursorToEvent(cursor));
        }
        cursor.close();
        return events;
    }

    public void checkInEvent(long eventId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_CHECKED_IN, 1);
        values.put(DBHelper.COL_ATTENDEES, getEventAttendees(eventId) + 1);
        db.update(DBHelper.TABLE_EVENTS, values,
                DBHelper.COL_EVENT_ID + "=?", new String[]{String.valueOf(eventId)});
    }

    private int getEventAttendees(long eventId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_EVENTS,
                new String[]{DBHelper.COL_ATTENDEES},
                DBHelper.COL_EVENT_ID + "=?", new String[]{String.valueOf(eventId)},
                null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    private Event cursorToEvent(Cursor cursor) {
        Event event = new Event();
        event.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_ID)));
        event.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_TITLE)));
        event.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_DESC)));
        event.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_LOCATION)));
        event.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_EVENT_DATE)));
        event.setAttendees(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_ATTENDEES)));
        event.setMaxAttendees(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_MAX_ATTENDEES)));
        event.setOrganizer(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_ORGANIZER)));
        event.setCheckedIn(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_CHECKED_IN)) == 1);
        return event;
    }

    /**
     * Seed mock data for demo purposes.
     * Includes comprehensive job listings and events with max attendees.
     */
    public void seedMockData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_JOBS, null);
        cursor.moveToFirst();
        int jobCount = cursor.getInt(0);
        cursor.close();

        if (jobCount > 0) return; // Already seeded

        // ==================== Mock Jobs ====================
        insertJob(new Job(0, "Android Developer", "TechCorp India",
                "Build and maintain Android applications using Java/Kotlin. Work with REST APIs and local databases.",
                "Bangalore, India", "Full-time",
                Arrays.asList("Java", "Android", "Kotlin", "REST API"), "2026-03-15", "₹8-12 LPA"));

        insertJob(new Job(0, "React Native Developer", "StartupXYZ",
                "Develop cross-platform mobile apps. Experience with React Native and JavaScript required.",
                "Mumbai, India", "Full-time",
                Arrays.asList("React Native", "JavaScript", "TypeScript"), "2026-03-20", "₹10-15 LPA"));

        insertJob(new Job(0, "Backend API Development", "FreelanceHub",
                "Build REST APIs using Node.js and Express. MongoDB experience preferred.",
                "Remote", "Freelance",
                Arrays.asList("Node.js", "Express", "MongoDB", "REST"), "2026-03-22", "₹50K/project"));

        insertJob(new Job(0, "UI/UX Design for Mobile App", "DesignStudio",
                "Design modern UI/UX for an Android health-tech application. Figma skills required.",
                "Remote", "Gig",
                Arrays.asList("Figma", "UI/UX", "Mobile Design"), "2026-03-25", "₹20K/gig"));

        insertJob(new Job(0, "Full Stack Developer", "CloudNine Solutions",
                "Work on full-stack web applications using Spring Boot and Angular.",
                "Hyderabad, India", "Full-time",
                Arrays.asList("Java", "Spring Boot", "Angular", "PostgreSQL"), "2026-03-28", "₹12-18 LPA"));

        insertJob(new Job(0, "Python ML Engineer", "AI Labs",
                "Build machine learning models for recommendation systems. TensorFlow experience required.",
                "Pune, India", "Freelance",
                Arrays.asList("Python", "TensorFlow", "ML", "Data Science"), "2026-03-29", "₹80K/project"));

        insertJob(new Job(0, "iOS Developer", "ApplePie Tech",
                "Develop native iOS applications using Swift and SwiftUI. Experience with Core Data preferred.",
                "Chennai, India", "Full-time",
                Arrays.asList("Swift", "SwiftUI", "iOS", "Core Data"), "2026-04-01", "₹10-14 LPA"));

        insertJob(new Job(0, "DevOps Engineer", "InfraCloud",
                "Set up CI/CD pipelines, manage Kubernetes clusters, and automate deployments.",
                "Bangalore, India", "Full-time",
                Arrays.asList("Docker", "Kubernetes", "Jenkins", "AWS"), "2026-04-05", "₹15-22 LPA"));

        insertJob(new Job(0, "WordPress Theme Development", "WebCraft",
                "Create custom WordPress themes with responsive design. PHP and CSS expertise needed.",
                "Remote", "Gig",
                Arrays.asList("PHP", "WordPress", "CSS", "JavaScript"), "2026-04-08", "₹15K/gig"));

        insertJob(new Job(0, "Data Analyst Intern", "DataDriven Co.",
                "Analyze large datasets using Python and SQL. Create visualizations and reports.",
                "Delhi, India", "Full-time",
                Arrays.asList("Python", "SQL", "Pandas", "Tableau"), "2026-04-10", "₹25K/month"));

        // ==================== Mock Events ====================
        insertEvent(new Event(0, "Android Dev Meetup 2026",
                "Monthly meetup for Android developers. Lightning talks, networking, and pizza!",
                "WeWork, Koramangala, Bangalore", "2026-04-15", 45, 80, "GDG Bangalore"));

        insertEvent(new Event(0, "Hackathon: Build for India",
                "24-hour hackathon focused on building solutions for Indian social challenges.",
                "IIIT Hyderabad Campus", "2026-04-20", 120, 200, "MLH India"));

        insertEvent(new Event(0, "Open Source Saturday",
                "Contribute to open source projects together. All skill levels welcome!",
                "91springboard, HSR Layout, Bangalore", "2026-04-05", 28, 50, "FOSS United"));

        insertEvent(new Event(0, "Flutter vs Native Workshop",
                "Hands-on workshop comparing Flutter and native Android development approaches.",
                "Microsoft Reactor, Bangalore", "2026-04-12", 60, 100, "Google Developers"));

        insertEvent(new Event(0, "Cloud & DevOps Conference",
                "Full-day conference on cloud-native architectures, Kubernetes, and CI/CD pipelines.",
                "Marriott Convention Center, Mumbai", "2026-04-25", 200, 500, "DevOps India"));

        insertEvent(new Event(0, "AI/ML Paper Reading Club",
                "Weekly session discussing the latest ML research papers. Bring your laptop!",
                "IISc Campus, Bangalore", "2026-04-18", 15, 30, "ML Bangalore"));

        insertEvent(new Event(0, "Startup Pitch Night",
                "Present your tech startup idea in 5 minutes. Investors and mentors in audience.",
                "The Hive, Indiranagar, Bangalore", "2026-05-02", 35, 50, "TiE Bangalore"));

        insertEvent(new Event(0, "Women in Tech Meetup",
                "Networking and mentorship for women developers. Panel discussion + lightning talks.",
                "ThoughtWorks Office, Bangalore", "2026-05-08", 50, 75, "WomenTechmakers"));
    }
}
