package pep.com.pepclass;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "bmi_records")
public class BmiRecord {

    @Id
    private String id;

    private String username;

    private String fullName;

    private double heightCm;

    private double weightKg;

    private double bmiValue;

    private String category;

    private String healthAdvice;

    private LocalDateTime createdAt = LocalDateTime.now();

    public BmiRecord() {}

    public BmiRecord(String username, String fullName, double heightCm, double weightKg, double bmiValue, String category, String healthAdvice) {
        this.username = username;
        this.fullName = fullName;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.bmiValue = bmiValue;
        this.category = category;
        this.healthAdvice = healthAdvice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(double heightCm) {
        this.heightCm = heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public double getBmiValue() {
        return bmiValue;
    }

    public void setBmiValue(double bmiValue) {
        this.bmiValue = bmiValue;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getHealthAdvice() {
        return healthAdvice;
    }

    public void setHealthAdvice(String healthAdvice) {
        this.healthAdvice = healthAdvice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
