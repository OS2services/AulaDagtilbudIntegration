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
import dk.digitalidentity.service.model.UserWithRole;

@Service
public class RoleService {

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private AppConfiguration configuration;

	public List<UserWithRole> getUsersWithRole(String role) {
		String url = configuration.getRoles().getUrl() + "api/whoHasRole?identifier=" + role;

		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getRoles().getApiKey());

		HttpEntity<String> request = new HttpEntity<>(headers);
		ResponseEntity<UserWithRole[]> response = restTemplate.exchange(url, HttpMethod.GET, request, UserWithRole[].class);
		
		return Arrays.asList(response.getBody());
	}
}
