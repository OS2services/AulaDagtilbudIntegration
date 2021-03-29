package dk.digitalidentity.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganisationConfiguration {
	private String url;
	private String apiKey;
	private boolean readManagers;
	private boolean implicitManagerRole;
	
	public String getUrl() {
		if (url.endsWith("/")) {
			return url;
		}
		
		return url + "/";
	}
}
