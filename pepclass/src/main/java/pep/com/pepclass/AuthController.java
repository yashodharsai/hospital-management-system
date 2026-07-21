package pep.com.pepclass;

import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalReportRepository medicalReportRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            PatientRepository patientRepository,
            MedicineRepository medicineRepository,
            MedicalReportRepository medicalReportRepository,
            PrescriptionRepository prescriptionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.medicineRepository = medicineRepository;
        this.medicalReportRepository = medicalReportRepository;
        this.prescriptionRepository = prescriptionRepository;
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
