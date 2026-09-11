package com.example.aichat.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.aichat.component.SpeechRecognizer.SpeechErrorEvent;
import com.example.aichat.component.SpeechRecognizer.SpeechResultEvent;
import com.example.aichat.component.SpeechRecognizer.SpeechStateEvent;
import com.example.aichat.component.SpeechRecognizer.State;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpeechRecognizerTest {

  @Test
  void languageConfigurationIsFluentAndCanonical() {
    SpeechRecognizer recognizer = new SpeechRecognizer();

    assertSame(recognizer, recognizer.setLanguage("de-de"));
    assertEquals("de-DE", recognizer.getLanguage());
  }

  @Test
  void typedEventsReadBrowserPayloads() {
    SpeechRecognizer recognizer = new SpeechRecognizer();

    SpeechResultEvent result =
        new SpeechResultEvent(recognizer, Map.of("transcript", "hello", "confidence", 0.9));
    SpeechStateEvent state =
        new SpeechStateEvent(recognizer, Map.of("state", "listening"));
    SpeechErrorEvent error =
        new SpeechErrorEvent(recognizer, Map.of("code", "not-allowed", "message", "Denied"));

    assertEquals("hello", result.getTranscript());
    assertEquals(0.9, result.getConfidence());
    assertEquals(State.LISTENING, state.getState());
    assertEquals("not-allowed", error.getCode());
    assertEquals("Denied", error.getMessage());
  }
}
