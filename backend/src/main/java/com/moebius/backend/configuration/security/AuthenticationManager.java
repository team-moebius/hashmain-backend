package com.moebius.backend.configuration.security;

import com.moebius.backend.domain.members.Role;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {
	private static final String AUTHORITIES_KEY = "roles";

	@Override
	// FIXME : Need to refactor these code as chained one
	public Mono<Authentication> authenticate(Authentication authentication) {

		String authToken = authentication.getCredentials().toString();

		Claims claims;
		try {
			claims = JwtUtil.getAllClaimsFromToken(authToken);
		} catch (Exception e) {
			claims = null;
		}
		if (claims != null && !JwtUtil.isTokenExpired(claims)) {
			String userName = claims.getSubject();
			List<String> rawRoles = claims.get(AUTHORITIES_KEY, List.class);
			Set<Role> roles = rawRoles.stream().map(Role::new).collect(Collectors.toSet());
			UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userName, authToken, roles);
			return Mono.just(auth);
		} else {
			return Mono.empty();
		}
	}
}
