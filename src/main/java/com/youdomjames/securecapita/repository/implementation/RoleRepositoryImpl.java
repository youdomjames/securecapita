package com.youdomjames.securecapita.repository.implementation;

import com.youdomjames.securecapita.domain.Role;
import com.youdomjames.securecapita.exception.ApiException;
import com.youdomjames.securecapita.repository.RoleRepository;
import com.youdomjames.securecapita.rowmapper.RoleRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static com.youdomjames.securecapita.enumeration.RoleType.ROLE_USER;
import static com.youdomjames.securecapita.query.RoleQuery.*;
import static java.util.Map.of;
import static java.util.Objects.requireNonNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository<Role> {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Role create(Role data) {
        return null;
    }

    @Override
    public Collection<Role> list(int page, int pageSize) {
        return null;
    }

    @Override
    public Role get(Long id) {
        return null;
    }

    @Override
    public Role update(Role data) {
        return null;
    }

    @Override
    public Boolean delete(Long id) {
        return null;
    }

    @Override
    public void addRoleToUser(Long userId, String roleName) {
        log.info("Adding role {} to user id: {}", roleName, userId);
        try{
            Role role = jdbc.queryForObject(SELECT_ROLE_BY_NAME_QUERY, of("name", roleName), new RoleRowMapper());
            jdbc.update(INSERT_ROLE_TO_USER_QUERY, of("userId", userId, "roleId", requireNonNull(role).getId()));
        }catch (EmptyResultDataAccessException e ){
            throw new ApiException("No role found by name: " + ROLE_USER.name());
        }catch (Exception e){
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Role getRoleByUserId(Long userId) {
        log.info("Get role by userId: {}", userId);
        try{
            return jdbc.queryForObject(SELECT_ROLE_BY_ID, of("id", userId), new RoleRowMapper());
        }catch (EmptyResultDataAccessException e ){
            throw new ApiException("No role found for userId: " + userId);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Role getRoleByUserEmail(String email) {
        log.info("Get role by email: {}", email);
        try{
            return jdbc.queryForObject(SELECT_ROLE_BY_EMAIL, of("email", email), new RoleRowMapper());
        }catch (EmptyResultDataAccessException e ){
            throw new ApiException("No role found for email: " + email);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void updateUserRole(Long userId, String roleName) {

    }
}
