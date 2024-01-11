package com.youdomjames.securecapita.service.implementation;

import com.youdomjames.securecapita.domain.Role;
import com.youdomjames.securecapita.repository.RoleRepository;
import com.youdomjames.securecapita.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository<Role> roleRoleRepository;

    @Override
    public Role getRoleByUserId(Long userId) {
        return roleRoleRepository.getRoleByUserId(userId);
    }
}
