package com.example.aichat.component;

import com.webforj.bundle.annotation.BundleEntry;
import com.webforj.component.element.ElementComposite;
import com.webforj.component.element.PropertyDescriptor;
import com.webforj.component.element.annotation.EventName;
import com.webforj.component.element.annotation.EventOptions;
import com.webforj.component.element.annotation.EventOptions.EventData;
import com.webforj.component.element.annotation.NodeName;
import com.webforj.component.event.ComponentEvent;
import com.webforj.dispatcher.EventListener;
import com.webforj.dispatcher.ListenerRegistration;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** A typed, per-instance bridge to the browser Web Speech recognition API. */
@BundleEntry("chat/speech-recognizer.ts")
@NodeName("webforj-speech-recognizer")
public final class SpeechRecognizer extends ElementComposite {

  /** Browser-side recognition states exposed to Java listeners. */
  public enum State {
    IDLE,
    LISTENING,
    UNSUPPORTED
  }

  private final PropertyDescriptor<String> language =
      PropertyDescriptor.attribute("language", "");

  /**
   * Sets the recognition language as a BCP 47 language tag.
   *
   * <p>An empty value uses the document or browser language.</p>
   */
  public SpeechRecognizer setLanguage(String languageTag) {
    String value = Objects.requireNonNull(languageTag, "languageTag").trim();
    if (value.isEmpty()) {
      set(language, "");
      return this;
    }

    try {
      value = new Locale.Builder().setLanguageTag(value).build().toLanguageTag();
    } catch (IllformedLocaleException exception) {
      throw new IllegalArgumentException("languageTag must be a valid BCP 47 tag", exception);
    }

    set(language, value);
    return this;
  }

  public String getLanguage() {
    return get(language);
  }

  public SpeechRecognizer start() {
    getElement().callJsFunctionVoidAsync("start");
    return this;
  }

  public SpeechRecognizer stop() {
    getElement().callJsFunctionVoidAsync("stop");
    return this;
  }

  public SpeechRecognizer toggle() {
    getElement().callJsFunctionVoidAsync("toggle");
    return this;
  }

  public ListenerRegistration<SpeechResultEvent> onResult(
      EventListener<SpeechResultEvent> listener) {
    return addEventListener(SpeechResultEvent.class, listener);
  }

  public ListenerRegistration<SpeechStateEvent> onStateChange(
      EventListener<SpeechStateEvent> listener) {
    return addEventListener(SpeechStateEvent.class, listener);
  }

  public ListenerRegistration<SpeechErrorEvent> onError(
      EventListener<SpeechErrorEvent> listener) {
    return addEventListener(SpeechErrorEvent.class, listener);
  }

  /** Final transcript produced for a single utterance. */
  @EventName("speech-result")
  @EventOptions(data = {
      @EventData(key = "transcript", exp = "event.detail.transcript"),
      @EventData(key = "confidence", exp = "event.detail.confidence")
  })
  public static final class SpeechResultEvent extends ComponentEvent<SpeechRecognizer> {

    public SpeechResultEvent(SpeechRecognizer component, Map<String, Object> payload) {
      super(component, payload);
    }

    public String getTranscript() {
      Object raw = getData().get("transcript");
      return raw == null ? "" : raw.toString();
    }

    public double getConfidence() {
      Object raw = getData().get("confidence");
      return raw instanceof Number number ? number.doubleValue() : 0.0;
    }
  }

  /** Recognition lifecycle state emitted by the browser adapter. */
  @EventName("speech-state")
  @EventOptions(data = {@EventData(key = "state", exp = "event.detail.state")})
  public static final class SpeechStateEvent extends ComponentEvent<SpeechRecognizer> {

    public SpeechStateEvent(SpeechRecognizer component, Map<String, Object> payload) {
      super(component, payload);
    }

    public State getState() {
      Object raw = getData().get("state");
      if (raw == null) {
        return State.IDLE;
      }

      try {
        return State.valueOf(raw.toString().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException exception) {
        return State.IDLE;
      }
    }
  }

  /** Actionable recognition failure emitted by the browser adapter. */
  @EventName("speech-error")
  @EventOptions(data = {
      @EventData(key = "code", exp = "event.detail.code"),
      @EventData(key = "message", exp = "event.detail.message")
  })
  public static final class SpeechErrorEvent extends ComponentEvent<SpeechRecognizer> {

    public SpeechErrorEvent(SpeechRecognizer component, Map<String, Object> payload) {
      super(component, payload);
    }

    public String getCode() {
      Object raw = getData().get("code");
      return raw == null ? "unknown" : raw.toString();
    }

    public String getMessage() {
      Object raw = getData().get("message");
      return raw == null ? "Speech recognition failed." : raw.toString();
    }
  }
}
