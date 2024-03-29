package dk.digitalidentity.service.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Employee {
	private String ssn;
	private long employmentId;
	private String institutionDtrId;
	private String email;
	private String mobilePhone;
	private boolean manager;
	private Set<String> roles = new HashSet<String>();
	
	// used for internal book-keeping
	@JsonIgnore
	private transient String uuid;

	// used for internal book-keeping
	@JsonIgnore
	private transient String userId;

	public Employee(User user) {
		this.ssn = user.getSsn();
		this.institutionDtrId = user.getDtrId();
		this.email = user.getEmail();
		this.mobilePhone = user.getPhone();
		this.uuid = user.getUuid();
		this.userId = user.getUserId();
		this.roles = new HashSet<String>();
		this.manager = user.isManager();
	}

	@JsonIgnore
	public String stringIdentifier() {
		return "userId=" + userId + ", uuid=" + uuid + ", employmentId=" + employmentId;
	}
}
