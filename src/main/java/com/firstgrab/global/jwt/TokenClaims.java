package com.firstgrab.global.jwt;

import com.firstgrab.domain.user.entity.Role;

public record TokenClaims(Long userId, Role role) {
}
