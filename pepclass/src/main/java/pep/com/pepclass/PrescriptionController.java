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

    public PrescriptionController(PrescriptionRepository prescriptionRepository, PatientRepository patientRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
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

    @GetMapping("/my-prescriptions")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientPrescriptions(Model model, Authentication authentication) {
        model.addAttribute("prescriptions", prescriptionRepository.findByPatientPatientUsername(authentication.getName()));
        return "patient-prescriptions";
    }
}
