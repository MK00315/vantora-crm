package com.mtcrm.security;

import com.mtcrm.user.Role;

import java.io.Serializable;
import java.util.UUID;

public record CurrentUser(UUID id, UUID tenantId, String email, Role role, long sessionVersion) implements Serializable {}
