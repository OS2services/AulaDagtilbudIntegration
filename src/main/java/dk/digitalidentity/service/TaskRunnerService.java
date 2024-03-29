package dk.digitalidentity.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.config.AppConfiguration;
import dk.digitalidentity.service.model.Employee;
import dk.digitalidentity.service.model.User;
import dk.digitalidentity.service.model.UserWithRole;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TaskRunnerService {

	@Autowired
	private RoleService roleService;
	
	@Autowired
	private RegistrationService registrationService;
	
	@Autowired
	private OrganisationService organisationService;

	@Autowired
	private AppConfiguration configuration;

	public void run() {
		log.info("Starting scheduled task");

		List<Employee> employees = getEmployees();
		if (employees == null || employees.size() == 0) {
			log.error("Found 0 users in organisation - terminating execution");
			return;
		}
		else {
			log.info("Got " + employees.size() + " users from organisation");
		}

		int totalRoleCount = 0;
		for (String role : configuration.getRoles().getSupportedRoles()) {
			log.info("Fetching all users with role: " + role);
			int roleCount = 0;

			List<UserWithRole> usersWithRole = roleService.getUsersWithRole(role);

			for (UserWithRole userWithRole : usersWithRole) {
				boolean found = false;
				
				for (Employee employee : employees) {
					if (Objects.equals(employee.getUuid(), userWithRole.getUuid())) {
						employee.getRoles().add(role);
						roleCount++;
						found = true;
						break;
					}
				}
				
				if (!found) {
					log.warn("User " + userWithRole.getUserId() + " was assigned " + role + " but was not in organisation");
				}
			}
			
			log.info("Found " + roleCount + " users in organisation matching role assignment for: " + role);

			totalRoleCount += roleCount;
		}

		if (totalRoleCount > 0) {
			registrationService.update(employees);
		}
		else {
			log.error("Did not find a single user with roles assigned - terminating execution");
			return;
		}
		
		log.info("Completed scheduled task");
	}
	
	private List<Employee> getEmployees() {
		log.info("Fetching users from organisation");

		List<Employee> employees = new ArrayList<>();
		
		List<User> users = organisationService.getUsers();
		for (User user : users) {
			if (!StringUtils.hasLength(user.getSsn())) {
				log.warn("User from organisation: " + user.getUserId() + " / " + user.getUuid() + " does not have a CPR number - skipping");
				continue;
			}

			// skip managers if not configured
			if (!configuration.getOrganisation().isReadManagers() && user.isManager()) {
				continue;
			}

			Employee match = null;
			for (Employee employee : employees) {
				if (Objects.equals(employee.getInstitutionDtrId(), user.getDtrId()) &&
					Objects.equals(employee.getSsn(), user.getSsn())) {
					match = employee;
					break;
				}
			}
			
			// update existing or create new
			Employee employee = null;
			if (match != null) {
				employee = match;
			}
			else {
				employee = new Employee(user);
				employees.add(employee);
			}
			
			if (configuration.getOrganisation().isImplicitManagerRole() && user.isManager()) {
				employee.getRoles().add("InstitutionManager");
			}
		}

		return employees;
	}
}
