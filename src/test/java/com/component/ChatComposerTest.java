package com.example.aichat.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aichat.component.SpeechRecognizer.SpeechErrorEvent;
import com.example.aichat.component.SpeechRecognizer.SpeechResultEvent;
import com.example.aichat.component.SpeechRecognizer.SpeechStateEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChatComposerTest {
  private final SpeechRecognizer recognizer = new SpeechRecognizer();
  private final List<String> events = new ArrayList<>();
  private final List<Double> confidences = new ArrayList<>();
  private final List<String> stateChanges = new ArrayList<>();
  private final ChatComposer composer = new ChatComposer(recognizer,
      prompt -> events.add("submit:" + prompt), () -> {}, () -> stateChanges.add("changed"),
      (text, confidence) -> {
        events.add("transcript:" + text);
        confidences.add(confidence);
      }, (code, message) -> events.add(code + ":" + message));

  @Test
  void dictationAppendsToDraftWithoutSubmittingByDefault() {
    composer.setDraft("Typed text  ");

    recognize("  spoken words  ");

    assertEquals("Typed text spoken words", composer.getDraft());
    assertEquals(List.of("transcript:spoken words"), events);
    assertEquals(List.of(0.9), confidences);
  }

  @Test
  void autoSubmitFollowsTranscriptionEventAndRespectsBusyState() {
    composer.setAutoSubmitSpeech(true);
    recognize("first");

    assertEquals(List.of("transcript:first", "submit:first"), events);
    composer.setDraft("");
    composer.setBusy(true);
    recognize("second");

    assertEquals("second", composer.getDraft());
    assertEquals(List.of("transcript:first", "submit:first", "transcript:second"), events);
  }

  @Test
  void emptyTranscriptionDoesNotChangeDraftOrEmitEvents() {
    composer.setDraft("Existing draft");
    composer.setAutoSubmitSpeech(true);

    recognize("   ");

    assertEquals("Existing draft", composer.getDraft());
    assertTrue(events.isEmpty());
  }

  @Test
  void recognitionEventsUpdateListeningStateAndForwardErrors() {
    changeState("listening");
    assertTrue(composer.isSpeechListening());
    SpeechErrorEvent error = new SpeechErrorEvent(recognizer,
        Map.of("code", "not-allowed", "message", "Permission denied"));
    recognizer.getEventListeners(SpeechErrorEvent.class).forEach(listener -> listener.onEvent(error));
    changeState("idle");

    assertFalse(composer.isSpeechListening());
    assertEquals(2, stateChanges.size());
    assertEquals(List.of("not-allowed:Permission denied"), events);
  }

  private void recognize(String transcript) {
    SpeechResultEvent event = new SpeechResultEvent(recognizer,
        Map.of("transcript", transcript, "confidence", 0.9));
    recognizer.getEventListeners(SpeechResultEvent.class).forEach(listener -> listener.onEvent(event));
  }

  private void changeState(String state) {
    SpeechStateEvent event = new SpeechStateEvent(recognizer, Map.of("state", state));
    recognizer.getEventListeners(SpeechStateEvent.class).forEach(listener -> listener.onEvent(event));
  }
}
