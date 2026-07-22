package pep.com.pepclass;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;

    public PrescriptionController(
            PrescriptionRepository prescriptionRepository,
            PatientRepository patientRepository,
            MedicineRepository medicineRepository,
            UserRepository userRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.medicineRepository = medicineRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String prescriptions(Model model) {
        model.addAttribute("prescriptions", prescriptionRepository.findAll());
        return "prescriptions";
    }

    @GetMapping("/prescriptions/new")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String newPrescription(Model model) {
        model.addAttribute("prescription", new Prescription());
        model.addAttribute("patients", patientRepository.findAll());
        return "prescription-form";
    }

    @PostMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String savePrescription(
            @Valid @ModelAttribute("prescription") Prescription prescription,
            BindingResult result,
            Model model,
            Authentication authentication) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientRepository.findAll());
            return "prescription-form";
        }
        if (prescription.getPrescribedBy() == null || prescription.getPrescribedBy().isBlank()) {
            prescription.setPrescribedBy(authentication.getName());
        }
        prescriptionRepository.save(prescription);
        return "redirect:/prescriptions";
    }

    @GetMapping("/prescriptions/{id}/delete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String deletePrescription(@PathVariable String id) {
        prescriptionRepository.deleteById(id);
        return "redirect:/prescriptions";
    }

    @GetMapping("/prescriptions/{id}/print")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN', 'PATIENT')")
    public String printPrescription(@PathVariable String id, Model model, Authentication authentication) {
        Prescription prescription = prescriptionRepository.findById(id).orElse(null);
        if (prescription == null) {
            return "redirect:/dashboard";
        }

        // Security check: Patients can only print/download their own prescriptions
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user != null && user.getRole() == Role.PATIENT) {
            if (prescription.getPatient() == null || prescription.getPatient().getPatientUsername() == null ||
                !prescription.getPatient().getPatientUsername().equalsIgnoreCase(authentication.getName())) {
                return "redirect:/my-prescriptions";
            }
        }

        String doctorUsername = prescription.getPrescribedBy();
        User doctor = (doctorUsername != null) ? userRepository.findByUsername(doctorUsername).orElse(null) : null;
        String doctorName = (doctor != null && doctor.getFullName() != null) ? doctor.getFullName() : (doctorUsername != null ? doctorUsername : "Attending Physician");

        model.addAttribute("prescription", prescription);
        model.addAttribute("patient", prescription.getPatient());
        model.addAttribute("doctorName", doctorName);

        return "prescription-print";
    }

    @GetMapping("/prescriptions/{id}/download")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN', 'PATIENT')")
    public String downloadPrescription(@PathVariable String id, Model model, Authentication authentication) {
        return printPrescription(id, model, authentication);
    }

    @GetMapping("/my-prescriptions")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientPrescriptions(Model model, Authentication authentication) {
        model.addAttribute("prescriptions", prescriptionRepository.findByPatientPatientUsername(authentication.getName()));
        model.addAttribute("medicines", medicineRepository.findByPatientPatientUsername(authentication.getName()));
        return "patient-prescriptions";
    }
}
