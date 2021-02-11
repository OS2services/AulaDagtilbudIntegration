package dk.digitalidentity.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.config.AppConfiguration;
import dk.digitalidentity.service.model.User;

@Service
public class OrganisationService {

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private AppConfiguration configuration;

	public List<User> getUsers() {
		String url = configuration.getOrganisation().getUrl() + "api/DtrId";

		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getOrganisation().getApiKey());

		HttpEntity<String> request = new HttpEntity<>(headers);
		ResponseEntity<User[]> response = restTemplate.exchange(url, HttpMethod.GET, request, User[].class);
		
		return Arrays.asList(response.getBody());
	}
}
