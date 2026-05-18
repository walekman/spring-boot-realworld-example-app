package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

class CursorPageParameterTest {

  @Test
  void should_store_cursor_limit_and_direction() {
    CursorPageParameter<String> param =
        new CursorPageParameter<>("cursor-abc", 10, Direction.NEXT);

    assertEquals("cursor-abc", param.getCursor());
    assertEquals(10, param.getLimit());
    assertEquals(Direction.NEXT, param.getDirection());
  }

  @Test
  void should_return_true_for_is_next_when_direction_is_next() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 5, Direction.NEXT);
    assertTrue(param.isNext());
  }

  @Test
  void should_return_false_for_is_next_when_direction_is_prev() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 5, Direction.PREV);
    assertFalse(param.isNext());
  }

  @Test
  void should_return_query_limit_as_limit_plus_one() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 10, Direction.NEXT);
    assertEquals(11, param.getQueryLimit());
  }

  @Test
  void should_cap_limit_at_max_when_over_1000() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 1500, Direction.NEXT);
    assertEquals(1000, param.getLimit());
  }

  @Test
  void should_use_default_limit_of_20_when_constructed_with_no_args() {
    CursorPageParameter<String> param = new CursorPageParameter<>();
    assertEquals(20, param.getLimit());
  }

  @Test
  void should_not_change_limit_when_zero_or_negative_is_passed() {
    CursorPageParameter<String> paramZero = new CursorPageParameter<>(null, 0, Direction.NEXT);
    CursorPageParameter<String> paramNeg = new CursorPageParameter<>(null, -5, Direction.NEXT);

    assertEquals(20, paramZero.getLimit());
    assertEquals(20, paramNeg.getLimit());
  }

  @Test
  void should_accept_null_cursor() {
    CursorPageParameter<String> param = new CursorPageParameter<>(null, 10, Direction.NEXT);
    assertEquals(null, param.getCursor());
  }
}
