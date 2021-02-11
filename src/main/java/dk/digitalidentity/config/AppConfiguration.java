package dk.digitalidentity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "configuration")
public class AppConfiguration {	
	private OrganisationConfiguration organisation = new OrganisationConfiguration();
	private RegistrationConfiguration registration = new RegistrationConfiguration();
	private RoleConfiguration roles = new RoleConfiguration();
}