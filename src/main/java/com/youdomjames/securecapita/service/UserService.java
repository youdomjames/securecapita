package com.youdomjames.securecapita.service;

import com.youdomjames.securecapita.domain.User;
import com.youdomjames.securecapita.dto.UserDTO;

public interface UserService {
    UserDTO createUser(User user);

    UserDTO getUserByEmail(String email);

    void sendVerificationCode(UserDTO user);

    UserDTO verifyCode(String email, String code);

    void resetPassword(String email);

    UserDTO verifyPasswordKey(String key);

    void renewPassword(String key, String password, String confirmPassword);

    UserDTO verifyAccount(String key);
}
