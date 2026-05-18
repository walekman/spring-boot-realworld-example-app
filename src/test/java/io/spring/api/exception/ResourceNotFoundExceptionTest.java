package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

class ResourceNotFoundExceptionTest {

  @Test
  void should_have_not_found_response_status() {
    ResponseStatus annotation = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);
    assertNotNull(annotation);
    assertTrue(annotation.value() == HttpStatus.NOT_FOUND);
  }

  @Test
  void should_be_a_runtime_exception() {
    ResourceNotFoundException ex = new ResourceNotFoundException();
    assertTrue(ex instanceof RuntimeException);
  }
}
