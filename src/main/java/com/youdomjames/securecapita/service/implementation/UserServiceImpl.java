package com.youdomjames.securecapita.service.implementation;

import com.youdomjames.securecapita.domain.User;
import com.youdomjames.securecapita.dto.UserDTO;
import com.youdomjames.securecapita.dto.mapper.UserDTOMapper;
import com.youdomjames.securecapita.repository.UserRepository;
import com.youdomjames.securecapita.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository<User> userUserRepository;
    @Override
    public UserDTO createUser(User user) {
        return UserDTOMapper.fromUser(userUserRepository.create(user));
    }
}
