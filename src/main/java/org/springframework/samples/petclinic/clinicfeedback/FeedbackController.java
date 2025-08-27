package org.springframework.samples.petclinic.clinicfeedback;

import io.opentelemetry.api.trace.Span;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.ClinicFeedback;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/clinic-feedback")public class FeedbackController {

	private final FeedbackService service;
	private static final Logger log = LoggerFactory.getLogger(FeedbackController.class);

	public FeedbackController(FeedbackService service) {
		this.service = service;
	}

	@GetMapping()
	public List<ClinicFeedback> list(
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "pageSize", defaultValue = "10") int pageSize
	) {
		var results = service.list(page, pageSize);
		return results;
	}

	@GetMapping(value = "/count", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Long> count() {
		try {
			long count = this.service.count();
			return ResponseEntity.ok().body(count);
		} catch (Exception e) {
			log.error("Error retrieving feedback count", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping("clear")
	public String clear() {
		var deleted = service.clear();
		return deleted + " feedbacks deleted";
	}

	@PostMapping("populate")
	public ResponseEntity<String> populateFeedbacks(@RequestParam(name = "count", defaultValue = "10000") int count) {
		try{
			service.populate(count);
			return ResponseEntity.ok("Populated");
		}
		catch (Exception ex){
			Span.current().recordException(ex);
			return ResponseEntity.internalServerError().body(ex.getMessage());
		}
	}
}@PostMapping("add")
	public ResponseEntity<String> addFeedback(@RequestBody ClinicFeedback newFeedback, BindingResult result) {
		if(result.hasErrors())
			return ResponseEntity.badRequest().body("Failed to parsed request");

		try{
			service.add(newFeedback);
			return ResponseEntity.ok("Inserted");
		}
		catch (Exception ex){
			Span.current().recordException(ex);
			return ResponseEntity.internalServerError().body(ex.getMessage());
		}

	}
}