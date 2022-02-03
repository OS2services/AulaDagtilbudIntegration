package dk.digitalidentity.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

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
	private List<String> supportedRoles;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private AppConfiguration configuration;

	@PostConstruct
	public void init() {
		supportedRoles = Arrays.asList(configuration.getRoles().getSupportedRoles());
	}

	public void update(List<Employee> employees) {
		log.info("Fetching existing employments from KMD I2");
		List<Employee> existingEmployees = fetchEmployments();

		Set<String> institutionFilter = getInstitutionFilter(employees);
		log.info("Filtering on institutions: " + String.join(",", institutionFilter));

		// apply DTR-ID filter on existing employees in KMD I2
		existingEmployees = existingEmployees.stream().filter(e -> institutionFilter.contains(e.getInstitutionDtrId())).collect(Collectors.toList());
		log.info(existingEmployees.size() + " employees from KMD I2 after filtering");
		
		// apply DTR-ID filter on employees from Organisation
		employees = employees.stream().filter(e -> institutionFilter.contains(e.getInstitutionDtrId())).collect(Collectors.toList());
		log.info(employees.size() + " employees from Organisation after filtering");

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
					updateEmployee(employee, existingEmployee, false);
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
	
	private Set<String> getInstitutionFilter(List<Employee> employees) {
		Set<String> implicitFilter = employees.stream().map(e -> e.getInstitutionDtrId()).collect(Collectors.toSet());
		
		Set<String> filter = new HashSet<>(implicitFilter);
		if (configuration.getRegistration().getDtrFilter() != null && configuration.getRegistration().getDtrFilter().length > 0) {
			List<String> configuredFilter = Arrays.asList(configuration.getRegistration().getDtrFilter());
			
			filter.removeIf(f -> !configuredFilter.contains(f));
		}
		
		return filter;
	}

	private void createEmployee(Employee employee) {
		if (employee.getRoles().size() == 0) {
			log.info("Not creating employee (no roles): " + employee.stringIdentifier());
			return;
		}

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

	private void updateEmployee(Employee employee, Employee existingEmployee, boolean forceUpdate) {
		boolean changes = forceUpdate;

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

		// ensure sane data
		if (existingEmployee.getRoles() == null) {
			existingEmployee.setRoles(new HashSet<String>());
		}
		if (employee.getRoles() == null) {
			employee.setRoles(new HashSet<String>());
		}

		// roles to add
		for (String role : employee.getRoles()) {
			boolean found = false;

			for (String existingRole : existingEmployee.getRoles()) {
				if (Objects.equals(existingRole, role)) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				existingEmployee.getRoles().add(role);
				changes = true;
			}
		}
		
		// roles to remove
		for (Iterator<String> iterator = existingEmployee.getRoles().iterator(); iterator.hasNext();) {
			String existingRole = iterator.next();

			// skip unsupported roles
			if (!supportedRoles.contains(existingRole)) {
				continue;
			}
			
			boolean found = false;
			for (String role : employee.getRoles()) {
				if (Objects.equals(role, existingRole)) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				changes = true;
				iterator.remove();
			}
		}
		
		if (changes) {
			if (!forceUpdate && existingEmployee.getRoles().size() == 0) {
				// unless we are forcing an update, if the employee has no roles, we delete him/her
				deleteEmployee(existingEmployee);
			}
			else {
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
	}
	
	private void deleteEmployee(Employee existingEmployee) {
		// do we have any roles left if we remove supported roles
		boolean changes = existingEmployee.getRoles().removeIf(r -> supportedRoles.contains(r));

		// then we just need to update the user
		if (existingEmployee.getRoles().size() > 0) {
			log.warn("NOT deleting " + existingEmployee.stringIdentifier() + " because of roles: " + String.join(",", existingEmployee.getRoles()));

			if (changes) {
				updateEmployee(existingEmployee, existingEmployee, true);
			}

			return;
		}
		
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
