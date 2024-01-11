package com.youdomjames.securecapita.repository;

import com.youdomjames.securecapita.domain.User;
import com.youdomjames.securecapita.dto.UserDTO;

import java.util.Collection;

public interface UserRepository <T extends User> {

    /* Basic CRUD Operations */
    T create (T data);
    Collection<T> list(int page, int pageSize);
    T get(Long id);
    T update(T data);
    Boolean delete(Long id);

    /* More complex Operations */
    User getUserByEmail(String email);

    void sendVerificationCode(UserDTO user);

    User verifyCode(String email, String code);

    void resetPassword(String email);

    User verifyPasswordKey(String key);

    void renewPassword(String key, String password, String confirmPassword);

    T verifyAccount(String key);
}
