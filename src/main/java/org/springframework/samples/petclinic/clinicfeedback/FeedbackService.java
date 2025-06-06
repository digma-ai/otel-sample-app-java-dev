package org.springframework.samples.petclinic.clinicfeedback;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.ClinicFeedback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeedbackService {
	private final FeedbackRepository repository;
	private final Lorem lorem = LoremIpsum.getInstance();

	public FeedbackService(FeedbackRepository repository) {
		this.repository = repository;
	}

	@WithSpan
	public void populate(int numberOfDocs){
		var feedbacks = new ArrayList<ClinicFeedback>();
		for (int i = 0; i < numberOfDocs; i++) {
			var feedback = new ClinicFeedback(
				lorem.getEmail(),
				lorem.getParagraphs(1, 3)
			);
			feedbacks.add(feedback);
		}
		repository.saveMultiple(feedbacks);
	}

	public long count() {
		return repository.count();
	}

	public long clear() {
		return repository.clear();
	}

	public List<ClinicFeedback> list(int page, int pageSize) {
		return repository.findAll(Pageable.ofSize(pageSize).withPage(page));
	}

	public void add(ClinicFeedback newFeedback) {
		repository.saveSingle(newFeedback);
	}
}
