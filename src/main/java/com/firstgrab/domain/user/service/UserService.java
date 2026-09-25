package com.firstgrab.domain.user.service;

import com.firstgrab.domain.user.controller.dto.SignupRequestDTO;
import com.firstgrab.domain.user.entity.User;
import com.firstgrab.domain.user.repository.UserRepository;
import com.firstgrab.global.exception.DuplicateException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static com.firstgrab.global.exception.ErrorMessage.DUPLICATE_EMAIL;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequestDTO signupRequestDTO) {
        validateDuplicateEmail(signupRequestDTO.getEmail());
        String encodedPassword = passwordEncoder.encode(signupRequestDTO.getPassword());
        User user = User.createUser(signupRequestDTO.getEmail(), encodedPassword, signupRequestDTO.getName());
        userRepository.save(user);
    }

    private void validateDuplicateEmail(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new DuplicateException(DUPLICATE_EMAIL);
                });
    }
}
