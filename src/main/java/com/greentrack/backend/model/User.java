package com.greentrack.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String password;
    public String contactNumber;
    
    @Column(columnDefinition = "int default 0")
    private int greenCoins = 0;

    // Profile fields
    private Integer age;
    private String gender;
    private String profession;
    private String city;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Lob
    @Column(name = "profile_image_base64", columnDefinition = "LONGTEXT")
    private String profileImageBase64;

    @Column(columnDefinition = "boolean default false")
    private boolean volunteerBadge = false;

    private LocalDateTime rank1Since;

    public User() {}

    public User(String fullName, String email, String password, String contactNumber) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.contactNumber = contactNumber;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber;}

    public int getGreenCoins() { return greenCoins; }
    public void setGreenCoins(int greenCoins) { this.greenCoins = greenCoins; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }

    public boolean isVolunteerBadge() { return volunteerBadge; }
    public void setVolunteerBadge(boolean volunteerBadge) { this.volunteerBadge = volunteerBadge; }

    public LocalDateTime getRank1Since() { return rank1Since; }
    public void setRank1Since(LocalDateTime rank1Since) { this.rank1Since = rank1Since; }
}
