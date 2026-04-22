package com.example.gitlinked.models;

import java.util.List;

public class Job {
    private long id;
    private String title;
    private String company;
    private String description;
    private String location;
    private String type; // "Full-time", "Freelance", "Gig"
    private List<String> skills;
    private String postedDate;
    private String salary;
    private String contactEmail;

    public Job() {}

    public Job(long id, String title, String company, String description,
               String location, String type, List<String> skills, String postedDate, String salary) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.description = description;
        this.location = location;
        this.type = type;
        this.skills = skills;
        this.postedDate = postedDate;
        this.salary = salary;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public String getPostedDate() { return postedDate; }
    public void setPostedDate(String postedDate) { this.postedDate = postedDate; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
}
