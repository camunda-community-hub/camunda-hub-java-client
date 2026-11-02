package org.camunda.community.hub.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AbstractCommandTest {

  @CommandLine.Command(name = "fixed")
  static class FixedCommand extends AbstractCommand {
    private final Object response;
    private final Exception failure;
    private final Exception validationFailure;

    FixedCommand(Object response, Exception failure, Exception validationFailure) {
      this.response = response;
      this.failure = failure;
      this.validationFailure = validationFailure;
    }

    @Override
    protected void validate() throws Exception {
      if (validationFailure != null) {
        throw validationFailure;
      }
    }

    @Override
    protected Object createResponse() throws Exception {
      if (failure != null) {
        throw failure;
      }
      return response;
    }
  }

  private record Outcome(int exitCode, String out, String err) {}

  private static Outcome execute(AbstractCommand command) {
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    try {
      int exitCode = new CommandLine(command).execute();
      return new Outcome(
          exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  @Test
  void shouldPrintResponseAsPrettyJson() {
    Outcome outcome = execute(new FixedCommand(Map.of("a", 1), null, null));

    assertEquals(0, outcome.exitCode());
    assertTrue(outcome.out().contains("\"a\" : 1"), outcome.out());
  }

  @Test
  void shouldPrintNothingForNullResponse() {
    Outcome outcome = execute(new FixedCommand(null, null, null));

    assertEquals(0, outcome.exitCode());
    assertEquals("", outcome.out());
  }

  @Test
  void shouldReturnCodeOfCommandException() {
    Outcome outcome = execute(new FixedCommand(null, new CommandException("nope", 42), null));

    assertEquals(42, outcome.exitCode());
    assertEquals("", outcome.err());
  }

  @Test
  void shouldReturnCodeOfCommandExceptionFromValidation() {
    Outcome outcome =
        execute(new FixedCommand("never printed", null, new CommandException("invalid", 7)));

    assertEquals(7, outcome.exitCode());
    assertEquals("", outcome.out());
  }

  @Test
  void shouldReturnOneAndPrintStackTraceForOtherExceptions() {
    Outcome outcome = execute(new FixedCommand(null, new IllegalStateException("boom"), null));

    assertEquals(1, outcome.exitCode());
    assertTrue(outcome.err().contains("IllegalStateException: boom"), outcome.err());
  }

  @Test
  void commandExceptionExposesCauseAndReturnCode() {
    Exception cause = new RuntimeException("root");
    CommandException exception = new CommandException("msg", cause, 3);

    assertEquals(3, exception.getReturnCode());
    assertEquals(cause, exception.getCause());
    assertEquals("msg", exception.getMessage());
  }
}
