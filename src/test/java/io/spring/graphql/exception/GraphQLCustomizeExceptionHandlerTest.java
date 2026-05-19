package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  @Test
  void should_return_unauthenticated_error_for_invalid_authentication_exception() {
    DataFetcherExceptionHandlerParameters params =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(new InvalidAuthenticationException());
    when(params.getPath()).thenReturn(ResultPath.rootPath());

    DataFetcherExceptionHandlerResult result = handler.handleException(params).join();

    assertEquals(1, result.getErrors().size());
    assertEquals("invalid email or password", result.getErrors().get(0).getMessage());
  }

  @Test
  void should_return_bad_request_error_for_constraint_violation_exception() {
    ConstraintViolationException cve = mock(ConstraintViolationException.class);
    when(cve.getConstraintViolations()).thenReturn(Collections.emptySet());
    when(cve.getMessage()).thenReturn("constraint violation");

    DataFetcherExceptionHandlerParameters params =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(cve);
    when(params.getPath()).thenReturn(ResultPath.rootPath());

    DataFetcherExceptionHandlerResult result = handler.handleException(params).join();

    assertEquals(1, result.getErrors().size());
    assertEquals("constraint violation", result.getErrors().get(0).getMessage());
  }

  @Test
  void should_delegate_other_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerParameters params =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(new RuntimeException("unexpected"));
    when(params.getPath()).thenReturn(ResultPath.rootPath());

    DataFetcherExceptionHandlerResult result = handler.handleException(params).join();

    assertNotNull(result);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ConstraintViolation<Object> buildViolation(String propertyPathString) {
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);

    Path path = mock(Path.class);
    when(path.toString()).thenReturn(propertyPathString);
    doReturn(path).when(violation).getPropertyPath();
    doReturn(Object.class).when(violation).getRootBeanClass();
    when(violation.getMessage()).thenReturn("must not be blank");

    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    Annotation annotation = mock(Annotation.class);
    doReturn(NotBlank.class).when(annotation).annotationType();
    doReturn(annotation).when(descriptor).getAnnotation();
    doReturn(descriptor).when(violation).getConstraintDescriptor();

    return violation;
  }

  @Test
  void should_map_dotted_property_path_to_last_segment_as_key() {
    ConstraintViolation<Object> violation = buildViolation("createUser.input.email");
    ConstraintViolationException cve = mock(ConstraintViolationException.class);
    when(cve.getConstraintViolations()).thenReturn(Set.of(violation));

    io.spring.graphql.types.Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(1, error.getErrors().size());
    assertEquals("email", error.getErrors().get(0).getKey());
  }

  @Test
  void should_map_plain_field_name_unchanged() {
    ConstraintViolation<Object> violation = buildViolation("email");
    ConstraintViolationException cve = mock(ConstraintViolationException.class);
    when(cve.getConstraintViolations()).thenReturn(Set.of(violation));

    io.spring.graphql.types.Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertEquals(1, error.getErrors().size());
    assertEquals("email", error.getErrors().get(0).getKey());
  }
}
