/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.common.security.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default Spring Cloud {@link AuthoritiesExtractor}. Will assign ALL
 * {@link CoreSecurityRoles} to the authenticated OAuth2 user.
 *
 * @author Gunnar Hillert
 *
 */
public class DefaultAuthoritiesExtractor implements AuthoritiesExtractor {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DefaultAuthoritiesExtractor.class);

	private final boolean mapOauthScopesToAuthorities;
	private final OAuth2RestOperations restTemplate;

	public DefaultAuthoritiesExtractor(boolean mapOauthScopesToAuthorities, OAuth2RestOperations restTemplate) {
		super();
		this.mapOauthScopesToAuthorities = mapOauthScopesToAuthorities;
		this.restTemplate = restTemplate;
	}

	public DefaultAuthoritiesExtractor() {
		super();
		this.mapOauthScopesToAuthorities = false;
		this.restTemplate = null;
	}

	/**
	 * The returned {@link List} of {@link GrantedAuthority}s contains all roles from
	 * {@link CoreSecurityRoles}. The roles are prefixed with the value specified in
	 * {@link GrantedAuthorityDefaults}.
	 *
	 *
	 * @param  map Must not be null. Is only used for logging
	 */
	@Override
	public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
		Assert.notNull(map, "The map argument must not be null.");

		final List<String> rolesAsStrings = new ArrayList<>();

		final List<GrantedAuthority> grantedAuthorities;

		if (this.mapOauthScopesToAuthorities) {
			Set<String> scopes = this.restTemplate.getAccessToken().getScope();
			grantedAuthorities = new ArrayList<>();

			if (scopes != null) {
				for (CoreSecurityRoles roleEnum : CoreSecurityRoles.values()) {
					for (String scope : scopes) {
						if (roleEnum.getKey().equalsIgnoreCase(scope)) {
							final String roleName = SecurityConfigUtils.ROLE_PREFIX + roleEnum.getKey();
							rolesAsStrings.add(roleName);
							grantedAuthorities.add(new SimpleGrantedAuthority(roleName));
						}
					}
				}
			}
		}
		else {
			grantedAuthorities =
					Stream.of(CoreSecurityRoles.values())
						.map(roleEnum -> {
							final String roleName = SecurityConfigUtils.ROLE_PREFIX + roleEnum.getKey();
							rolesAsStrings.add(roleName);
							return new SimpleGrantedAuthority(roleName);
						})
						.collect(Collectors.toList());
		}


		logger.info("Adding ALL roles {} to user {}", StringUtils.collectionToCommaDelimitedString(rolesAsStrings), map);
		return grantedAuthorities;
	}
}
