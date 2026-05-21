package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateUserValidatorTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UpdateUserValidator validator;

  private final User targetUser = new User("orig@test.com", "origname", "pass", "", "");

  @Test
  void should_return_true_when_email_and_username_not_taken() {
    when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());

    UpdateUserParam param =
        UpdateUserParam.builder().email("new@test.com").username("newname").build();
    UpdateUserCommand command = new UpdateUserCommand(targetUser, param);

    assertTrue(validator.isValid(command, mock(jakarta.validation.ConstraintValidatorContext.class)));
  }

  @Test
  void should_return_true_when_email_belongs_to_same_user() {
    when(userRepository.findByEmail("orig@test.com")).thenReturn(Optional.of(targetUser));
    when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());

    UpdateUserParam param =
        UpdateUserParam.builder().email("orig@test.com").username("newname").build();
    UpdateUserCommand command = new UpdateUserCommand(targetUser, param);

    assertTrue(validator.isValid(command, mock(jakarta.validation.ConstraintValidatorContext.class)));
  }

  @Test
  void should_return_false_when_email_taken_by_other_user() {
    User otherUser = new User("other@test.com", "other", "pass", "", "");
    when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));
    when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());

    UpdateUserParam param =
        UpdateUserParam.builder().email("other@test.com").username("newname").build();
    UpdateUserCommand command = new UpdateUserCommand(targetUser, param);

    jakarta.validation.ConstraintValidatorContext ctx =
        mock(jakarta.validation.ConstraintValidatorContext.class);
    jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder builder =
        mock(jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.class);
    jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext node =
        mock(jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
    when(ctx.buildConstraintViolationWithTemplate("email already exist")).thenReturn(builder);
    when(builder.addPropertyNode("email")).thenReturn(node);

    assertFalse(validator.isValid(command, ctx));
  }
}
