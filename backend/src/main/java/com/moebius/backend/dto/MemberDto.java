package com.moebius.backend.dto;

import com.moebius.backend.domain.members.ApiKey;
import com.moebius.backend.domain.members.Level;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Email;
import java.util.Set;

@Getter
@Setter
@ToString
public class MemberDto {
	private Level level;
	private String name;
	@Email
	private String email;
	private Set<ApiKey> apiKeys;
}
