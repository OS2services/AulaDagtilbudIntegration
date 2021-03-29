package dk.digitalidentity.service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
	private String uuid;
	private String ssn;
	private String userId;
	private String phone;
	private String email;
	private String dtrId;
	private boolean manager;
}
