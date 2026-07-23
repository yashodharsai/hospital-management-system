package pep.com.pepclass.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pep.com.pepclass.model.BmiRecord;
import pep.com.pepclass.model.User;
import pep.com.pepclass.repository.BmiRecordRepository;
import pep.com.pepclass.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
public class BmiController {

    private final BmiRecordRepository bmiRecordRepository;
    private final UserRepository userRepository;

    public BmiController(BmiRecordRepository bmiRecordRepository, UserRepository userRepository) {
        this.bmiRecordRepository = bmiRecordRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/bmi")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR', 'PATIENT')")
    public String bmiPage(Model model, Authentication authentication) {
        setupBmiModel(model, authentication);
        return "bmi-calculator";
    }

    @PostMapping("/bmi/calculate")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR', 'PATIENT')")
    public String calculateBmi(
            @RequestParam double height,
            @RequestParam double weight,
            Model model,
            Authentication authentication) {

        if (height <= 0 || weight <= 0) {
            model.addAttribute("error", "Please enter valid positive numbers for height and weight.");
            setupBmiModel(model, authentication);
            return "bmi-calculator";
        }

        double heightInMeters = height / 100.0;
        double rawBmi = weight / (heightInMeters * heightInMeters);

        BigDecimal bd = new BigDecimal(rawBmi).setScale(1, RoundingMode.HALF_UP);
        double bmiValue = bd.doubleValue();

        String category = determineCategory(bmiValue);
        String advice = determineAdvice(category);

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        String fullName = user != null ? user.getFullName() : username;

        BmiRecord record = new BmiRecord(username, fullName, height, weight, bmiValue, category, advice);
        bmiRecordRepository.save(record);

        model.addAttribute("latestRecord", record);
        setupBmiModel(model, authentication);

        return "bmi-calculator";
    }

    private void setupBmiModel(Model model, Authentication authentication) {
        String username = authentication.getName();
        List<BmiRecord> records = bmiRecordRepository.findByUsernameOrderByCreatedAtDesc(username);
        model.addAttribute("bmiRecords", records);
        if (!records.isEmpty() && !model.containsAttribute("latestRecord")) {
            model.addAttribute("latestRecord", records.get(0));
        }
    }

    private String determineCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi <= 24.9) return "Normal Weight";
        if (bmi <= 29.9) return "Overweight";
        return "Obese";
    }

    private String determineAdvice(String category) {
        return switch (category) {
            case "Underweight" -> "Focus on nutrient-dense foods, lean proteins, healthy fats (nuts, avocados), and progressive strength training to build healthy muscle mass.";
            case "Normal Weight" -> "Excellent! Maintain your healthy state through a balanced diet, proper hydration, and 150 minutes of moderate activity per week.";
            case "Overweight" -> "Incorporate regular cardio exercise, control portion sizes, limit processed sugars, and focus on gradual, sustainable lifestyle adjustments.";
            default -> "Consult a physician or certified dietitian for a structured health plan. Regular monitoring of blood pressure and glucose levels is advised.";
        };
    }
}
