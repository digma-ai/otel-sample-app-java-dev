package org.springframework.samples.petclinic.patientrecords;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/patient-records")
@Profile({"postgres", "mysql"})
public class PatientRecordUiController {

    private final JdbcTemplate mysqlJdbcTemplate;

    @Autowired
    public PatientRecordUiController(@Qualifier("mysqlJdbcTemplate") JdbcTemplate mysqlJdbcTemplate) {
        this.mysqlJdbcTemplate = mysqlJdbcTemplate;
    }

    @GetMapping("/query-records")
    public String showQueryRecordsPage() {
        return "patientrecords/query-records";
    }
} 