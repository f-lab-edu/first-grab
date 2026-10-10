package com.firstgrab.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockUserIdSecurityContextFactory
        implements WithSecurityContextFactory<WithMockUserId> {


    @Override
    public SecurityContext createSecurityContext(WithMockUserId annotation) {
        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.authenticated(
                        annotation.value(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + annotation.role().name())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticationToken);
        return context;
    }
}
