package com.youdomjames.securecapita.resource;

import com.youdomjames.securecapita.LoginForm;
import com.youdomjames.securecapita.domain.HttpResponse;
import com.youdomjames.securecapita.domain.User;
import com.youdomjames.securecapita.domain.UserPrincipal;
import com.youdomjames.securecapita.dto.UserDTO;
import com.youdomjames.securecapita.exception.ApiException;
import com.youdomjames.securecapita.provider.TokenProvider;
import com.youdomjames.securecapita.service.RoleService;
import com.youdomjames.securecapita.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static com.youdomjames.securecapita.dto.mapper.UserDTOMapper.toUser;
import static com.youdomjames.securecapita.filter.CustomAuthorizationFilter.TOKEN_PREFIX;
import static com.youdomjames.securecapita.utils.ExceptionUtils.processError;
import static java.time.LocalDateTime.now;
import static java.util.Map.of;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.unauthenticated;

@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserResource {
    private final TokenProvider tokenProvider;
    private final UserService userService;
    private final RoleService roleService;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<HttpResponse> login(@RequestBody @Valid LoginForm loginForm) {
        UserDTO user = getAuthenticatedUser(authenticate(loginForm.getEmail(), loginForm.getPassword()));
        return user.isUsingMfa() ? sendVerificationCode(user) : sendResponse(user);
    }

    @PostMapping("/register")
    public ResponseEntity<HttpResponse> saveUser(@RequestBody @Valid User user) {
        UserDTO userDTO = userService.createUser(user);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", userDTO))
                        .message("User created")
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build()
        );
    }

    @GetMapping("/verify/code")
    public ResponseEntity<HttpResponse> verifyCode(@RequestParam("email") String email, @RequestParam("code") String code) {
        UserDTO user = userService.verifyCode(email, code);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user, "access_token", tokenProvider.createAccessToken(getUserPrincipal(user)),
                                "refresh_token", tokenProvider.createRefreshToken(getUserPrincipal(user))))
                        .message("Login Success")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<HttpResponse> profile(Authentication authentication) {
        UserDTO user = (UserDTO) authentication.getPrincipal();
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user))
                        .message("Profile fetched")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    //START - Reset password when user is not logged In
    @PostMapping("/reset/password")
    public ResponseEntity<HttpResponse> requestPasswordReset(@RequestParam("email") String email) {
        userService.resetPassword(email);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .message("Email sent. Please check your email to reset your password")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    @PostMapping("/verify/password/{key}")
    public ResponseEntity<HttpResponse> verifyPasswordKey(@PathVariable("key") String key) {
        UserDTO user = userService.verifyPasswordKey(key);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user))
                        .message("Please enter a new password")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    @PostMapping("/reset/password/{key}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("key") String key, @RequestParam("password") String password, @RequestParam("confirmPassword") String confirmPassword) {
        userService.renewPassword(key, password, confirmPassword);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .message("Password reset successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    //END - Reset password when user is not logged In

    @PostMapping("/verify/account/{key}")
    public ResponseEntity<HttpResponse> verifyAccount(@PathVariable("key") String key) {
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .message(userService.verifyAccount(key).isEnabled() ? "Account already verified" : "Account verified")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    @GetMapping("/refresh/token")
    public ResponseEntity<HttpResponse> verifyAccount(HttpServletRequest request) {
        if (isHeaderTokenValid(request)){
        String token = request.getHeader(AUTHORIZATION).substring(TOKEN_PREFIX.length());
        UserDTO user = userService.getUserByEmail(tokenProvider.getSubject(token, request));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user, "access_token", tokenProvider.createAccessToken(getUserPrincipal(user)),
                                "refresh_token", token))
                        .message("Token refreshed")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
        } else
            return ResponseEntity.badRequest().body(
                    HttpResponse.builder()
                            .timeStamp(now().toString())
                            .message("Refresh Token missing or invalid")
                            .status(BAD_REQUEST)
                            .statusCode(BAD_REQUEST.value())
                            .build()
            );
    }

    private boolean isHeaderTokenValid(HttpServletRequest request) {
        return request.getHeader(AUTHORIZATION) != null &&
                request.getHeader(AUTHORIZATION).startsWith(TOKEN_PREFIX) &&
                tokenProvider.isTokenValid(tokenProvider.getSubject(request.getHeader(AUTHORIZATION).substring(TOKEN_PREFIX.length()), request),
                        request.getHeader(AUTHORIZATION).substring(TOKEN_PREFIX.length()));
    }

    @RequestMapping("/error")
    public ResponseEntity<HttpResponse> handleError(HttpServletRequest request) {
        return new ResponseEntity<>(HttpResponse.builder()
                .timeStamp(now().toString())
                .reason("There is no mapping for a " + request.getMethod() + " request for this path on the server")
                .status(NOT_FOUND)
                .statusCode(NOT_FOUND.value())
                .build(), NOT_FOUND);
    }

    private ResponseEntity<HttpResponse> sendResponse(UserDTO user) {
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user, "access_token", tokenProvider.createAccessToken(getUserPrincipal(user)),
                                "refresh_token", tokenProvider.createRefreshToken(getUserPrincipal(user))))
                        .message("Login Success")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    private URI getUri() {
        return URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/get/<userId>").toUriString());
    }

    private UserPrincipal getUserPrincipal(UserDTO user) {
        return new UserPrincipal(toUser(userService.getUserByEmail(user.getEmail())), roleService.getRoleByUserId(user.getId()));
    }

    private ResponseEntity<HttpResponse> sendVerificationCode(UserDTO user) {
        userService.sendVerificationCode(user);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", UserDTO.builder().email(user.getEmail()).phone(user.getPhone().substring(user.getPhone().length() - 4)).build()))
                        .message("VERIFICATION_CODE_SENT")
                        .status(OK)
                        .statusCode(OK.value())
                        .build()
        );
    }

    private Authentication authenticate(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(unauthenticated(email, password));
            return authentication;
        } catch (Exception exception) {
            throw new ApiException(exception.getMessage());
        }
    }

    private UserDTO getAuthenticatedUser(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUser();
    }
}
