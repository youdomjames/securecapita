package com.youdomjames.securecapita.service;

import com.youdomjames.securecapita.domain.User;
import com.youdomjames.securecapita.dto.UserDTO;

public interface UserService {
    UserDTO createUser(User user);
}
