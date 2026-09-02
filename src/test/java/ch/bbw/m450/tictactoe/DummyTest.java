package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke tests that only prove that JUnit 5 and AssertJ are correctly wired into the build.
 * They do not test any production code.
 * Method names and bodies follow the GIVEN_WHEN_THEN pattern.
 */
class DummyTest {

	@Test
	@DisplayName("GIVEN JUnit 5 on the test classpath WHEN plain assertions are evaluated THEN they hold")
	void GIVEN_junitOnClasspath_WHEN_plainAssertionsEvaluated_THEN_theyHold() {
		// GIVEN JUnit 5 (Jupiter) on the test classpath
		var truth = true;

		// WHEN plain JUnit assertions are evaluated
		// THEN they hold
		assertFalse(!truth);
		assertTrue(truth);
		assertEquals(2, 1 + 1);
	}

	@Test
	@DisplayName("GIVEN AssertJ on the test classpath WHEN fluent assertions are evaluated THEN they hold")
	void GIVEN_assertJOnClasspath_WHEN_fluentAssertionsEvaluated_THEN_theyHold() {
		// GIVEN AssertJ on the test classpath
		var game = "tic-tac-toe";

		// WHEN fluent AssertJ assertions are evaluated
		// THEN they hold
		assertThat(false).isFalse();
		assertThat(game).startsWith("tic")
				.contains("tac")
				.hasSize(11);
		assertThat(1 + 1).isEqualTo(2);
	}
}
