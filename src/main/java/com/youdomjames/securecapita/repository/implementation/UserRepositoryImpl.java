package com.youdomjames.securecapita.repository.implementation;

import com.youdomjames.securecapita.domain.Role;
import com.youdomjames.securecapita.domain.User;
import com.youdomjames.securecapita.domain.UserPrincipal;
import com.youdomjames.securecapita.dto.UserDTO;
import com.youdomjames.securecapita.enumeration.VerificationType;
import com.youdomjames.securecapita.exception.ApiException;
import com.youdomjames.securecapita.repository.RoleRepository;
import com.youdomjames.securecapita.repository.UserRepository;
import com.youdomjames.securecapita.rowmapper.UserRowMapper;
import com.youdomjames.securecapita.utils.SmsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import static com.youdomjames.securecapita.enumeration.RoleType.ROLE_USER;
import static com.youdomjames.securecapita.enumeration.VerificationType.ACCOUNT;
import static com.youdomjames.securecapita.enumeration.VerificationType.PASSWORD;
import static com.youdomjames.securecapita.query.UserQuery.*;
import static java.util.Map.of;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.time.DateFormatUtils.format;
import static org.apache.commons.lang3.time.DateUtils.addDays;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository<User>, UserDetailsService {

    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
    private final NamedParameterJdbcTemplate jdbc;
    private final RoleRepository<Role> roleRepository;
    private final BCryptPasswordEncoder encoder;
    private final SmsUtils smsUt;

    @Override
    public User create(User user) {
        if (getEmailCount(user.getEmail().trim().toLowerCase()) > 0) throw new ApiException("Email already in use. Please use a different email and try again.");
        try{
            KeyHolder holder = new GeneratedKeyHolder();
            SqlParameterSource parameters = getSqlParameterSource(user);
            jdbc.update(INSERT_USER_QUERY, parameters, holder);
            user.setId(requireNonNull(holder.getKey()).longValue());
            roleRepository.addRoleToUser(user.getId(), ROLE_USER.name());
            String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), ACCOUNT.getType());
            jdbc.update(INSERT_VERIFICATION_QUERY, of("userId", user.getId(), "url", verificationUrl));
//            emailService.sendVerificationUrl(user.getFirstName(), user.getEmail(), verificationUrl, ACCOUNT);
            return user;
        }catch (Exception e){
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Collection<User> list(int page, int pageSize) {
        return null;
    }

    @Override
    public User get(Long id) {
        return null;
    }

    @Override
    public User update(User user) {
        return null;
    }

    @Override
    public Boolean delete(Long id) {
        return null;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = getUserByEmail(email);
        if (user == null){
            log.error("User not found in the database");
            throw new UsernameNotFoundException("User not found in the database");
        }else {
            log.info("User found in the database. UID: {}", user.getId());
            return new UserPrincipal(user, roleRepository.getRoleByUserId(user.getId()));
        }
    }
    @Override
    public User getUserByEmail(String email) {
        try{
            return jdbc.queryForObject(SELECT_USER_BY_EMAIL_QUERY, of("email", email), new UserRowMapper());
        }catch (EmptyResultDataAccessException e){
            throw new ApiException("No User found by email");
        } catch (Exception e){
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void sendVerificationCode(UserDTO user) {
        String expirationDate = format(addDays(new Date(), 1), DATE_FORMAT);
        String verificationCode = randomNumeric(6).toUpperCase();
        try{
            jdbc.update(DELETE_VERIFICATION_CODE_BY_USER_ID, of("id", user.getId()));
            jdbc.update(INSERT_VERIFICATION_CODE_QUERY, of("userId", user.getId(), "code", verificationCode, "expirationDate", expirationDate));
            log.warn("Verification Code: {}", verificationCode);//to be deleted. Used only in dev.
            //smsUtils.sendSMS(user.getFirstName(), user.getLastName(), user.getPhone(), "From: SecureCapita \nVerification code\n " + verificationCode);
        } catch (Exception e){
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User verifyCode(String email, String code) {
        if (isVerificationCodeExpired(code)) throw new ApiException("This code has expired. Please login again");
        try {
            User userByCode = jdbc.queryForObject(SELECT_BY_USER_CODE_QUERY, of("code", code), new UserRowMapper());
            User userByEmail = jdbc.queryForObject(SELECT_USER_BY_EMAIL_QUERY, of("email", email), new UserRowMapper());
            if (requireNonNull(userByCode).getEmail().equalsIgnoreCase(requireNonNull(userByEmail).getEmail())){
                jdbc.update(DELETE_CODE_BY_EMAIL_AND_CODE, of("email", email, "code", code));
                return userByCode;
            }else
                throw new ApiException("Code is invalid. Please try again");
        }catch (EmptyResultDataAccessException exception){
            throw new ApiException("Could not find record");
        }catch (Exception e){
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void resetPassword(String email) {
        if (getEmailCount(email.trim().toLowerCase()) <= 0) throw new ApiException("There is no account for this email address");
        try{
            String expirationDate = format(addDays(new Date(), 1), DATE_FORMAT);
            User user = getUserByEmail(email);
            String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), PASSWORD.getType());
            jdbc.update(DELETE_PASSWORD_VERIFICATION_BY_USER_ID_QUERY, of("userId", user.getId()));
            jdbc.update(INSERT_PASSWORD_VERIFICATION_QUERY, of("userId", user.getId(), "url", verificationUrl, "expirationDate", expirationDate));
            //TODO Send email with url to user
            log.info("Verification url: {}", verificationUrl);
        } catch (Exception e){
            log.error(e.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public User verifyPasswordKey(String key) {
        if (isLinkExpired(key)) throw new ApiException("This link has expired. Please reset your password again if needed");
        try {
            return jdbc.queryForObject(SELECT_USER_BY_PASSWORD_URL_QUERY, of("url", getVerificationUrl(key, VerificationType.PASSWORD.getType())), new UserRowMapper());
//            jdbc.update(DELETE_PASSWORD_VERIFICATION_BY_USER_ID_QUERY, of("userId", requireNonNull(user).getId()));
//            return user;
        }catch (EmptyResultDataAccessException exception){
            log.error(exception.getMessage());
            throw new ApiException("This link is not valid. Please reset password again");
        }catch (Exception exception){
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again");
        }
    }

    @Override
    public void renewPassword(String key, String password, String confirmPassword) {
        if (!password.equalsIgnoreCase(confirmPassword)) throw new ApiException("Password don't match. Please try again.");
        try {
            jdbc.update(UPDATE_USER_PASSWORD_BY_URL_QUERY, of("password", encoder.encode(password), "url", getVerificationUrl(key, PASSWORD.getType())));
            jdbc.update(DELETE_VERIFICATION_BY_URL_QUERY, of("password", encoder.encode(password), "url", getVerificationUrl(key, PASSWORD.getType())));
            return;
        }catch (Exception exception){
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again");
        }
    }

    @Override
    public User verifyAccount(String key) {
        try {
            User user = jdbc.queryForObject(SELECT_USER_BY_ACCOUNT_URL_QUERY, of("url", getVerificationUrl(key, ACCOUNT.getType())), new UserRowMapper());
            jdbc.update(UPDATE_USER_ENABLED_QUERY, of("enabled", true, "id", requireNonNull(user).getId()));
            return user;
        }catch (EmptyResultDataAccessException exception){
            log.error(exception.getMessage());
            throw new ApiException("This link is not valid.");
        }catch (Exception exception){
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again");
        }
    }

    private boolean isLinkExpired(String key) {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject(SELECT_EXPIRATION_BY_URL, of("url", getVerificationUrl(key, VerificationType.PASSWORD.getType())), Boolean.class));
        }catch (EmptyResultDataAccessException exception){
            log.error(exception.getMessage());
            throw new ApiException("This link is not valid. Please reset password again");
        }catch (Exception exception){
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again");
        }
    }

    private boolean isVerificationCodeExpired(String code) {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject(CODE_EXPIRATION_CHECK_QUERY, of("code", code), Boolean.class));
        }catch (EmptyResultDataAccessException exception){
            throw new ApiException("Could not find record");
        }catch (Exception e){
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    private int getEmailCount(String email) {
        return jdbc.queryForObject(COUNT_USER_EMAIL_QUERY, of("email", email), Integer.class);
    }

    private SqlParameterSource getSqlParameterSource(User user) {
        return new MapSqlParameterSource()
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("email", user.getEmail())
                .addValue("password", encoder.encode(user.getPassword()))
                .addValue("usingMfa", user.isUsingMfa())
                .addValue("phone", user.getPhone());
    }

    private String getVerificationUrl(String key, String type){
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/verify/"+type+"/"+key).toUriString();
    }
}
