package io.spring.application.user;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import jakarta.validation.Constraint;

@Constraint(validatedBy = UpdateUserValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateUserConstraint {

  String message() default "invalid update param";

  Class[] groups() default {};

  Class[] payload() default {};
}
