package org.springframework.samples.petclinic.clinicfeedback;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.ClinicFeedback;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class FeedbackRepository
{
	private final String apiBaseUrl;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public FeedbackRepository(@Value("${spring.data.flaskdb.uri}") String apiBaseUrl) {
		this.apiBaseUrl = apiBaseUrl;
		this.httpClient = HttpClient.newHttpClient();
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
		this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	public void saveMultiple(List<ClinicFeedback> feedbacks) {
		for (ClinicFeedback feedback : feedbacks){
			HttpResponse<String> response;
			try {
				var json = objectMapper.writeValueAsString(feedback);
				var request = HttpRequest.newBuilder()
					.uri(URI.create(this.apiBaseUrl))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.build();

				response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			} catch (Exception e) {
				throw new RuntimeException("Error during saving feedbacks", e);
			}

			if (response.statusCode() != 200 && response.statusCode() != 201) {
				throw new RuntimeException("Failed to save feedbacks to flaskdb: " + response.body());
			}
		}
	}

	public void saveSingle(ClinicFeedback feedback) {
		HttpResponse<String> response;
		try {
			String json = objectMapper.writeValueAsString(feedback);
			var request = HttpRequest.newBuilder()
				.uri(URI.create(this.apiBaseUrl))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();

			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (Exception e) {
			throw new RuntimeException("Error during saving feedback", e);
		}

		if (response.statusCode() != 200 && response.statusCode() != 201) {
			throw new RuntimeException("Failed to save feedback to flaskdb: " + response.body());
		}
	}

	public List<ClinicFeedback> findAll(Pageable pageable) {
		try {
			String url = String.format("%s?page=%d&size=%d", this.apiBaseUrl, pageable.getPageNumber(), pageable.getPageSize());
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				return objectMapper.readValue(response.body(), new TypeReference<List<ClinicFeedback>>() {});
			} else {
				throw new RuntimeException("Failed to fetch feedbacks. Status: " + response.statusCode());
			}
		} catch (Exception e) {
			throw new RuntimeException("Error during fetching feedbacks", e);
		}
	}

	public long count() {
		try {
			String url = this.apiBaseUrl + "/count";
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				// Assuming API returns: { "count": 42 }
				JsonNode node = objectMapper.readTree(response.body());
				return node.get("count").asLong();
			} else {
				throw new RuntimeException("Failed to fetch count. Status: " + response.statusCode());
			}
		} catch (Exception e) {
			throw new RuntimeException("Error during fetching feedback count", e);
		}
	}

	public long clear() {
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(this.apiBaseUrl))
				.DELETE()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				// Assuming API returns: { "deleted": 42 }
				JsonNode node = objectMapper.readTree(response.body());
				return node.get("deleted").asLong();
			} else {
				throw new RuntimeException("Failed to clear feedbacks. Status: " + response.statusCode());
			}
		} catch (Exception e) {
			throw new RuntimeException("Error during fetching feedback count", e);
		}
	}
}
