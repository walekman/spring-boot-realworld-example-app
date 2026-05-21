package io.spring.application.user;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UserService {
  private UserRepository userRepository;
  private String defaultImage;
  private PasswordEncoder passwordEncoder;

  @Autowired
  public UserService(
      UserRepository userRepository,
      @Value("${image.default}") String defaultImage,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.defaultImage = defaultImage;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public User createUser(@Valid RegisterParam registerParam) {
    User user =
        new User(
            registerParam.getEmail(),
            registerParam.getUsername(),
            passwordEncoder.encode(registerParam.getPassword()),
            "",
            defaultImage);
    userRepository.save(user);
    return user;
  }

  @Transactional
  public void updateUser(@Valid UpdateUserCommand command) {
    User user = command.getTargetUser();
    UpdateUserParam updateUserParam = command.getParam();
    String rawPassword = updateUserParam.getPassword();
    String encodedPassword =
        (rawPassword == null || rawPassword.isEmpty())
            ? rawPassword
            : passwordEncoder.encode(rawPassword);
    user.update(
        updateUserParam.getEmail(),
        updateUserParam.getUsername(),
        encodedPassword,
        updateUserParam.getBio(),
        updateUserParam.getImage());
    userRepository.save(user);
  }
}
