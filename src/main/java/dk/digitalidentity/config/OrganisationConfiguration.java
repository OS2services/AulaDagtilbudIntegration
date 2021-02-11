package dk.digitalidentity.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganisationConfiguration {
	private String url;
	private String apiKey;
	
	public String getUrl() {
		if (url.endsWith("/")) {
			return url;
		}
		
		return url + "/";
	}
}
