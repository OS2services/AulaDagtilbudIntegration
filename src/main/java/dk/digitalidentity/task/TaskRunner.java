package dk.digitalidentity.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import dk.digitalidentity.config.AppConfiguration;
import dk.digitalidentity.service.OrganisationService;
import dk.digitalidentity.service.RegistrationService;
import dk.digitalidentity.service.RoleService;
import dk.digitalidentity.service.model.Employee;
import dk.digitalidentity.service.model.User;
import dk.digitalidentity.service.model.UserWithRole;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableScheduling
@Component
public class TaskRunner {

	@Autowired
	private RoleService roleService;
	
	@Autowired
	private RegistrationService registrationService;
	
	@Autowired
	private OrganisationService organisationService;

	@Autowired
	private AppConfiguration configuration;

	// TODO: change
	@Scheduled(fixedDelay = 60 * 60 * 1000, initialDelay = 10 * 1000)
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

			employees.add(new Employee(user));
		}

		return employees;
	}
}
