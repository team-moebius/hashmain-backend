package com.moebius.backend.domain.members;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
@Document(collection = "roles")
@EqualsAndHashCode
public class Role implements GrantedAuthority {
	@Id
	private String id;

	@Override
	public String getAuthority() {
		return id;
	}
}
