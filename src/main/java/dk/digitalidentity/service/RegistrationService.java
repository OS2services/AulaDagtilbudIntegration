package dk.digitalidentity.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.config.AppConfiguration;
import dk.digitalidentity.service.model.Employee;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RegistrationService {

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private AppConfiguration configuration;

	public void update(List<Employee> employees) {
		log.info("Fetching existing employments from KMD I2");
		List<Employee> existingEmployees = fetchEmployments();
		
		// find any we need to delete
		for (Employee existingEmployee : existingEmployees) {
			boolean found = false;
			
			for (Employee employee : employees) {
				if (compare(employee, existingEmployee)) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				deleteEmployee(existingEmployee);
			}
		}

		// find any we need to update
		for (Employee existingEmployee : existingEmployees) {			
			for (Employee employee : employees) {
				if (compare(employee, existingEmployee)) {
					updateEmployee(employee, existingEmployee);
					break;
				}
			}
		}
		
		// find any we need to create
		for (Employee employee : employees) {
			boolean found = false;
			
			for (Employee existingEmployee : existingEmployees) {
				if (compare(employee, existingEmployee)) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				createEmployee(employee);
			}
		}
	}
	
	private void createEmployee(Employee employee) {
		log.info("Creating employee: " + employee.stringIdentifier());

		try {
			String url = configuration.getRegistration().getUrl() + "/api/employments";
	
			HttpHeaders headers = new HttpHeaders();
			headers.add("ApiKey", configuration.getRegistration().getApiKey());
	
			HttpEntity<Employee> request = new HttpEntity<>(employee, headers);
			restTemplate.exchange(url, HttpMethod.POST, request, String.class);
		}
		catch (Exception ex) {
			log.error("Failed to create employee", ex);
		}			
	}

	private void updateEmployee(Employee employee, Employee existingEmployee) {
		boolean changes = false;

		// note: startDate, endDate and workPhone is not currently updateable (no input data)
		
		if (!Objects.equals(employee.getEmail(), existingEmployee.getEmail())) {
			existingEmployee.setEmail(employee.getEmail());
			changes = true;
		}

		if (!Objects.equals(employee.getMobilePhone(), existingEmployee.getMobilePhone())) {			
			existingEmployee.setMobilePhone(employee.getMobilePhone());
			changes = true;
		}
		
		if (!Objects.equals(employee.getUserId(), existingEmployee.getUserId())) {			
			existingEmployee.setUserId(employee.getUserId());
			changes = true;
		}

		long existingRoleCount = existingEmployee.getRoles() != null ? existingEmployee.getRoles().size() : 0;
		long roleCount = employee.getRoles() != null ? employee.getRoles().size() : 0;
		if (existingRoleCount != roleCount) {
			existingEmployee.setRoles(employee.getRoles());
			changes = true;
		}
		else if (existingRoleCount == 0) {
			; // do nothing, they are both empty
		}
		else {
			// more complex comparison needed - but we know that they are equal and > 1 in count

			boolean missingRole = false;
			for (String existingRole : existingEmployee.getRoles()) {
				boolean found = false;
				
				for (String role : employee.getRoles()) {
					if (Objects.equals(role, existingRole)) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					missingRole = true;
					break;
				}
			}
			
			if (missingRole) {
				existingEmployee.setRoles(employee.getRoles());
				changes = true;
			}
		}
		
		if (changes) {
			log.info("Updating employee: " + existingEmployee.stringIdentifier());

			try {
				String url = configuration.getRegistration().getUrl() + "/api/employments/" + existingEmployee.getEmploymentId();
	
				HttpHeaders headers = new HttpHeaders();
				headers.add("ApiKey", configuration.getRegistration().getApiKey());
	
				HttpEntity<Employee> request = new HttpEntity<>(existingEmployee, headers);
				restTemplate.exchange(url, HttpMethod.POST, request, String.class);
			}
			catch (Exception ex) {
				log.info("Failed to updated employee", ex);
			}
		}
	}
	
	private void deleteEmployee(Employee existingEmployee) {
		log.info("Deleting employee: " + existingEmployee.stringIdentifier());

		try {
			String url = configuration.getRegistration().getUrl() + "/api/employments/" + existingEmployee.getEmploymentId();
	
			HttpHeaders headers = new HttpHeaders();
			headers.add("ApiKey", configuration.getRegistration().getApiKey());
	
			HttpEntity<String> request = new HttpEntity<>(headers);
			restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
		}
		catch (Exception ex) {
			log.error("Failed to delete employee", ex);
		}
	}

	private List<Employee> fetchEmployments() {
		String url = configuration.getRegistration().getUrl() + "/api/employments";

		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getRegistration().getApiKey());

		HttpEntity<String> request = new HttpEntity<>(headers);
		ResponseEntity<Employee[]> response = restTemplate.exchange(url, HttpMethod.GET, request, Employee[].class);
		
		return Arrays.asList(response.getBody());
	}
	
	private boolean compare(Employee e1, Employee e2) {
		// sanity check
		if (e1 == null || e2 == null) {
			return false;
		}
		
		if (Objects.equals(e1.getSsn(), e2.getSsn()) &&
			Objects.equals(e1.getInstitutionDtrId(), e2.getInstitutionDtrId())) {
			return true;
		}
		
		return false;
	}
}
