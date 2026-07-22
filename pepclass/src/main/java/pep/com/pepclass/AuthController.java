package pep.com.pepclass;

import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalReportRepository medicalReportRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            PatientRepository patientRepository,
            MedicineRepository medicineRepository,
            MedicalReportRepository medicalReportRepository,
            PrescriptionRepository prescriptionRepository,
            PasswordResetOtpRepository otpRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.medicineRepository = medicineRepository;
        this.medicalReportRepository = medicalReportRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") RegistrationForm form, BindingResult result, Model model) {
        if (userRepository.findByUsername(form.getUsername()).isPresent()) {
            result.rejectValue("username", "duplicate", "Username already exists");
        }
        if (result.hasErrors()) {
            return "register";
        }
        User user = new User();
        user.setUsername(form.getUsername());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setPhone(form.getPhone());
        user.setRole(Role.PATIENT);
        user.setEnabled(true);
        userRepository.save(user);

        Patient patient = new Patient();
        patient.setFullName(form.getFullName());
        patient.setContact(form.getPhone());
        patient.setEmail(form.getEmail());
        patient.setPatientUsername(form.getUsername());
        patientRepository.save(patient);

        return "redirect:/login?registered";
    }

    // =========================================================================
    // FORGOT PASSWORD STEP 1: Request OTP by Registered Email
    // =========================================================================
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("form", new ForgotPasswordForm());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute("form") ForgotPasswordForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "forgot-password";
        }
        String inputEmail = form.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(inputEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(inputEmail);
        }
        if (userOpt.isEmpty()) {
            result.rejectValue("email", "notFound", "No account registered with email address: " + inputEmail);
            return "forgot-password";
        }

        User user = userOpt.get();
        String targetEmail = user.getEmail() != null ? user.getEmail() : inputEmail;

        // Invalidate previous unused OTPs for this email
        List<PasswordResetOtp> previousOtps = otpRepository.findByEmail(targetEmail.toLowerCase());
        for (PasswordResetOtp oldOtp : previousOtps) {
            oldOtp.setUsed(true);
            otpRepository.save(oldOtp);
        }

        // Generate 6-digit OTP code and 5-minute expiry
        String otpCode = String.format("%06d", new Random().nextInt(900000) + 100000);
        PasswordResetOtp otpRecord = new PasswordResetOtp();
        otpRecord.setEmail(targetEmail.toLowerCase());
        otpRecord.setOtpCode(otpCode);
        otpRecord.setExpiryTime(Instant.now().plus(5, ChronoUnit.MINUTES));
        otpRecord.setUsed(false);
        otpRepository.save(otpRecord);

        // Dispatch Email via EmailService
        emailService.sendOtpEmail(targetEmail, otpCode, 5);

        return "redirect:/verify-otp?email=" + targetEmail + "&sent=true";
    }

    // Resend OTP handler
    @GetMapping("/resend-otp")
    public String resendOtp(@RequestParam("email") String email) {
        if (email == null || email.isBlank()) {
            return "redirect:/forgot-password";
        }
        ForgotPasswordForm form = new ForgotPasswordForm();
        form.setEmail(email);
        BindingResult dummyResult = new org.springframework.validation.BeanPropertyBindingResult(form, "form");
        return processForgotPassword(form, dummyResult, null);
    }

    // =========================================================================
    // FORGOT PASSWORD STEP 2: Verify 6-Digit OTP
    // =========================================================================
    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "sent", required = false) String sent,
            Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("email", email);
        model.addAttribute("sent", sent != null);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processVerifyOtp(
            @RequestParam("email") String email,
            @RequestParam("otpCode") String otpCode,
            Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/forgot-password";
        }
        String cleanEmail = email.trim().toLowerCase();
        String cleanCode = otpCode == null ? "" : otpCode.trim();

        Optional<PasswordResetOtp> otpOpt = otpRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(cleanEmail);
        if (otpOpt.isEmpty()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "No active OTP request found. Please enter your email again.");
            return "verify-otp";
        }

        PasswordResetOtp otpRecord = otpOpt.get();

        if (otpRecord.isExpired()) {
            model.addAttribute("email", email);
            model.addAttribute("error", "The OTP code has expired (valid for 5 minutes). Please request a new OTP code.");
            return "verify-otp";
        }

        if (!otpRecord.getOtpCode().equals(cleanCode)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Incorrect 6-digit verification code. Please check your email and try again.");
            return "verify-otp";
        }

        // OTP verified! Mark used and generate reset token
        otpRecord.setUsed(true);
        String resetToken = UUID.randomUUID().toString();
        otpRecord.setResetToken(resetToken);
        otpRepository.save(otpRecord);

        return "redirect:/reset-password?email=" + cleanEmail + "&token=" + resetToken;
    }

    // =========================================================================
    // FORGOT PASSWORD STEP 3: Reset Password
    // =========================================================================
    @GetMapping("/reset-password")
    public String showResetPasswordForm(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "token", required = false) String token,
            Model model) {
        if (email == null || token == null || email.isBlank() || token.isBlank()) {
            return "redirect:/forgot-password";
        }
        Optional<PasswordResetOtp> tokenOpt = otpRepository.findByEmailAndResetTokenAndUsedFalse(email.toLowerCase(), token);
        if (tokenOpt.isEmpty()) {
            return "redirect:/forgot-password";
        }

        ResetPasswordForm form = new ResetPasswordForm();
        form.setEmail(email);
        form.setResetCode(token);
        model.addAttribute("form", form);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@Valid @ModelAttribute("form") ResetPasswordForm form, BindingResult result, Model model) {
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        String cleanEmail = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(cleanEmail);
        }
        if (userOpt.isEmpty()) {
            result.rejectValue("email", "notFound", "Account not found for email: " + cleanEmail);
        }
        if (result.hasErrors()) {
            return "reset-password";
        }

        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);

        // Invalidate all remaining OTP records for this user
        List<PasswordResetOtp> otps = otpRepository.findByEmail(cleanEmail);
        for (PasswordResetOtp otp : otps) {
            otp.setUsed(true);
            otpRepository.save(otp);
        }

        return "redirect:/login?resetSuccess";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
            model.addAttribute("fullName", user.getFullName());
            model.addAttribute("role", user.getRole().name());
            model.addAttribute("roleLabel", formatRole(user.getRole()));
        });
        model.addAttribute("patientCount", patientRepository.count());
        model.addAttribute("medicineCount", medicineRepository.count());
        model.addAttribute("reportCount", medicalReportRepository.count());
        model.addAttribute("prescriptionCount", prescriptionRepository.count());
        return "dashboard";
    }

    private String formatRole(Role role) {
        String lower = role.name().toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }
}
