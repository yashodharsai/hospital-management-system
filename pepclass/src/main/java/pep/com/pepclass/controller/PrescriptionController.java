package pep.com.pepclass.controller;

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
import org.springframework.web.bind.annotation.RequestParam;

import pep.com.pepclass.model.MedicationNotification;
import pep.com.pepclass.model.Prescription;
import pep.com.pepclass.model.Role;
import pep.com.pepclass.model.User;
import pep.com.pepclass.repository.MedicationNotificationRepository;
import pep.com.pepclass.repository.MedicineRepository;
import pep.com.pepclass.repository.PatientRepository;
import pep.com.pepclass.repository.PrescriptionRepository;
import pep.com.pepclass.repository.UserRepository;

@Controller
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;
    private final MedicationNotificationRepository notificationRepository;

    public PrescriptionController(
            PrescriptionRepository prescriptionRepository,
            PatientRepository patientRepository,
            MedicineRepository medicineRepository,
            UserRepository userRepository,
            MedicationNotificationRepository notificationRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.medicineRepository = medicineRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String prescriptions(Model model) {
        model.addAttribute("prescriptions", prescriptionRepository.findAll());
        return "prescriptions";
    }

    @GetMapping("/prescriptions/new")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String newPrescription(@RequestParam(value = "patientId", required = false) String patientId, Model model) {
        Prescription prescription = new Prescription();
        if (patientId != null && !patientId.isBlank()) {
            patientRepository.findById(patientId).ifPresent(prescription::setPatient);
        }
        model.addAttribute("prescription", prescription);
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

        if (prescription.getPatient() != null && prescription.getPatient().getId() != null) {
            patientRepository.findById(prescription.getPatient().getId()).ifPresent(p -> {
                prescription.setPatient(p);
            });
        }

        Prescription saved = prescriptionRepository.save(prescription);

        if (saved.getId() != null) {
            notificationRepository.deleteByPrescriptionId(saved.getId());
        }

        String patientUsername = saved.getPatient() != null ? saved.getPatient().getPatientUsername() : null;
        String patientName = saved.getPatient() != null ? saved.getPatient().getFullName() : "Patient";
        String foodTiming = saved.getFoodTiming() != null ? saved.getFoodTiming() : "After Food";
        String doctor = saved.getPrescribedBy();

        if (patientUsername != null && !patientUsername.isBlank()) {
            if (saved.getMorning() != null && !saved.getMorning().isBlank()) {
                notificationRepository.save(new MedicationNotification(
                        patientUsername, patientName, saved.getId(),
                        saved.getMedicineName(), saved.getDosage(), "Morning", "08:00 AM", foodTiming, doctor
                ));
            }
            if (saved.getAfternoon() != null && !saved.getAfternoon().isBlank()) {
                notificationRepository.save(new MedicationNotification(
                        patientUsername, patientName, saved.getId(),
                        saved.getMedicineName(), saved.getDosage(), "Afternoon", "01:00 PM", foodTiming, doctor
                ));
            }
            if (saved.getEvening() != null && !saved.getEvening().isBlank()) {
                notificationRepository.save(new MedicationNotification(
                        patientUsername, patientName, saved.getId(),
                        saved.getMedicineName(), saved.getDosage(), "Evening", "06:00 PM", foodTiming, doctor
                ));
            }
            if (saved.getNight() != null && !saved.getNight().isBlank()) {
                notificationRepository.save(new MedicationNotification(
                        patientUsername, patientName, saved.getId(),
                        saved.getMedicineName(), saved.getDosage(), "Night", "09:00 PM", foodTiming, doctor
                ));
            }
        }

        return "redirect:/prescriptions";
    }

    @GetMapping("/prescriptions/{id}/delete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String deletePrescription(@PathVariable String id) {
        prescriptionRepository.deleteById(id);
        notificationRepository.deleteByPrescriptionId(id);
        return "redirect:/prescriptions";
    }

    @GetMapping("/prescriptions/{id}/print")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN', 'PATIENT')")
    public String printPrescription(@PathVariable String id, Model model, Authentication authentication) {
        Prescription prescription = prescriptionRepository.findById(id).orElse(null);
        if (prescription == null) {
            return "redirect:/dashboard";
        }

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
        String username = authentication.getName();
        model.addAttribute("prescriptions", prescriptionRepository.findByPatientPatientUsername(username));
        model.addAttribute("medicines", medicineRepository.findByPatientPatientUsername(username));
        model.addAttribute("notifications", notificationRepository.findByPatientUsernameAndStatus(username, "DUE"));
        return "patient-prescriptions";
    }
}
