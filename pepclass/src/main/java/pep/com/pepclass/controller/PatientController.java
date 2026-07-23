package pep.com.pepclass.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import pep.com.pepclass.model.Patient;
import pep.com.pepclass.repository.MedicineRepository;
import pep.com.pepclass.repository.PatientRepository;

@Controller
public class PatientController {

    private final PatientRepository patientRepository;
    private final MedicineRepository medicineRepository;

    public PatientController(PatientRepository patientRepository, MedicineRepository medicineRepository) {
        this.patientRepository = patientRepository;
        this.medicineRepository = medicineRepository;
    }

    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String listPatients(Model model) {
        List<Patient> patients = patientRepository.findAll();
        model.addAttribute("patients", patients);
        model.addAttribute("medicines", medicineRepository.findAll());
        return "patients";
    }

    @GetMapping("/patients/new")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String newPatient(Model model) {
        model.addAttribute("patient", new Patient());
        return "patient-form";
    }

    @PostMapping("/patients")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String savePatient(@Valid @ModelAttribute Patient patient, BindingResult result) {
        if (result.hasErrors()) {
            return "patient-form";
        }
        patientRepository.save(patient);
        return "redirect:/patients";
    }

    @GetMapping("/patients/{id}/edit")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String editPatient(@PathVariable String id, Model model) {
        Patient patient = patientRepository.findById(id).orElseThrow();
        model.addAttribute("patient", patient);
        return "patient-form";
    }
}
