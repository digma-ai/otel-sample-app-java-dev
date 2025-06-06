package org.springframework.samples.petclinic.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ClinicFeedback {

	private String id;
	private String userEmail;
	private String comment;
	private Instant submittedAt;

	public ClinicFeedback() {
	}

	public ClinicFeedback(String userEmail, String comment) {
		this.id = UUID.randomUUID().toString();
		this.userEmail = userEmail;
		this.comment = comment;
		this.submittedAt = Instant.now();
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setSubmittedAt(Instant submittedAt) {
		this.submittedAt = submittedAt;
	}
}
