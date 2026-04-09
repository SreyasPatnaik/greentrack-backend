package com.greentrack.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "waste_reports")
public class WasteReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnore
    private User user;

    private String wasteType;
    private String severity;
    private String description;
    private String locationData;
    
    // Precise GPS coordinates for map pinning
    private Double latitude;
    private Double longitude;
    
    private String status = "PENDING";
    private Integer coinsEarned = 0;

    private String cleanupStatus; // null -> PROCESSING -> ASSIGNED -> CLEANED
    
    @Lob
    @Column(name = "image_data_base64", columnDefinition = "LONGTEXT")
    private String imageBase64;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public WasteReport() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getWasteType() { return wasteType; }
    public void setWasteType(String wasteType) { this.wasteType = wasteType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocationData() { return locationData; }
    public void setLocationData(String locationData) { this.locationData = locationData; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getCoinsEarned() { return coinsEarned; }
    public void setCoinsEarned(Integer coinsEarned) { this.coinsEarned = coinsEarned; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getCleanupStatus() { return cleanupStatus; }
    public void setCleanupStatus(String cleanupStatus) { this.cleanupStatus = cleanupStatus; }
}
