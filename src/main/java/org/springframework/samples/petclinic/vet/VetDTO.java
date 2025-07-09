package org.springframework.samples.petclinic.vet;

import java.util.Set;
import java.util.stream.Collectors;

public class VetDTO {
    private Integer id;
    private String firstName;
    private String lastName;
    private Set<String> specialties;

    public VetDTO(Vet vet) {
        this.id = vet.getId();
        this.firstName = vet.getFirstName();
        this.lastName = vet.getLastName();
        this.specialties = vet.getSpecialties().stream()
            .map(Specialty::getName)
            .collect(Collectors.toSet());
    }

    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Set<String> getSpecialties() {
        return specialties;
    }
}