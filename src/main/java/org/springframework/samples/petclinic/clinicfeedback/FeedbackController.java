package org.springframework.samples.petclinic.clinicfeedback;

import io.opentelemetry.api.trace.Span;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.ClinicFeedback;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clinic-feedback")
public class FeedbackController {

	private final FeedbackService service;

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

	@GetMapping("count")
	public String count() {
		return String.valueOf(service.count());
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

	@PostMapping("add")
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
