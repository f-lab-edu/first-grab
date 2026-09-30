package com.firstgrab.domain.user.service;

import com.firstgrab.domain.user.entity.User;
import com.firstgrab.domain.user.repository.UserRepository;
import com.firstgrab.domain.user.service.dto.SignupCommand;
import com.firstgrab.global.exception.DuplicateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static com.firstgrab.global.exception.ErrorMessage.DUPLICATE_EMAIL;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupCommand signupCommand) {
        validateDuplicateEmail(signupCommand.getEmail());
        String encodedPassword = passwordEncoder.encode(signupCommand.getPassword());
        User user = User.createUser(signupCommand.getEmail(), encodedPassword, signupCommand.getName());
        userRepository.save(user);
        log.info("User signed up, userId={}", user.getId());
    }

    private void validateDuplicateEmail(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new DuplicateException(DUPLICATE_EMAIL);
                });
    }
}
