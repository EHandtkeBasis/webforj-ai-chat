package com.example.aichat.component;

import com.webforj.component.Composite;
import com.webforj.component.button.Button;
import com.webforj.component.button.ButtonTheme;
import com.webforj.component.event.KeypressEvent;
import com.webforj.component.field.TextArea;
import com.webforj.component.html.elements.Div;
import com.webforj.component.html.elements.Span;
import com.webforj.component.icons.DwcIcon;
import com.webforj.component.icons.FeatherIcon;
import com.webforj.component.icons.TablerIcon;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Owns draft editing, send/stop actions, and speech-recognition feedback. */
final class ChatComposer extends Composite<Div> {
  private final TextArea input = new TextArea();
  private final Button speechAction = new Button("Speak");
  private final Button action = new Button("Send");
  private final Span speechHint = new Span();
  private final SpeechRecognizer speechRecognizer;
  private final Consumer<String> submitPrompt;
  private final Runnable stateChanged;
  private final BiConsumer<String, Double> transcriptionReceived;
  private SpeechRecognizer.State speechState = SpeechRecognizer.State.IDLE;
  private String speechFeedback = "";
  private boolean busy;
  private boolean speechEnabled = true;
  private boolean autoSubmitSpeech;

  ChatComposer(SpeechRecognizer speechRecognizer, Consumer<String> submitPrompt,
      Runnable stopResponse, Runnable stateChanged,
      BiConsumer<String, Double> transcriptionReceived,
      BiConsumer<String, String> speechErrorReceived) {
    this.speechRecognizer = speechRecognizer;
    this.submitPrompt = submitPrompt;
    this.stateChanged = stateChanged;
    this.transcriptionReceived = transcriptionReceived;
    input.setPlaceholder("Write a message...");
    input.setLineWrap(true);
    input.setAttribute("aria-label", "Write a message");
    input.addClassName("ai-chat__input");
    input.onKeypress(event -> {
      if (event.getKeyCode().equals(KeypressEvent.Key.ENTER) && !event.isShiftKey() && !busy) {
        submitInput();
      }
    });

    speechAction.setTheme(ButtonTheme.GRAY);
    speechAction.setPrefixComponent(TablerIcon.create("microphone"));
    speechAction.setAttribute("aria-label", "Start voice input");
    speechAction.addClassName("ai-chat__speech-action");
    speechAction.onClick(event -> {
      if (isSpeechAvailable()) {
        speechRecognizer.toggle();
      }
    });
    speechRecognizer.onResult(event ->
        acceptSpeechResult(event.getTranscript(), event.getConfidence()));
    speechRecognizer.onStateChange(event -> updateSpeechState(event.getState()));
    speechRecognizer.onError(event -> {
      speechFeedback = event.getMessage();
      updateSpeechControls();
      speechErrorReceived.accept(event.getCode(), event.getMessage());
    });

    action.setTheme(ButtonTheme.PRIMARY);
    action.setPrefixComponent(FeatherIcon.ARROW_UP.create());
    action.setAttribute("aria-label", "Send message");
    action.addClassName("ai-chat__action");
    action.onClick(event -> {
      if (busy) {
        stopResponse.run();
      } else {
        submitInput();
      }
    });

    Div row = new Div(input, speechRecognizer, speechAction, action);
    row.addClassName("ai-chat__composer-row");
    Span keyboardHint = new Span("Enter to send · Shift+Enter for a new line");
    speechHint.addClassName("ai-chat__speech-hint");
    Div hint = new Div(keyboardHint, speechHint);
    hint.addClassName("ai-chat__hint");
    getBoundComponent().addClassName("ai-chat__composer");
    getBoundComponent().add(row, hint);
  }

  void setPlaceholder(String value) {
    input.setPlaceholder(Objects.requireNonNull(value, "value"));
    input.setAttribute("aria-label", value);
  }

  String getPlaceholder() {
    return input.getPlaceholder();
  }

  void setDraft(String value) {
    input.setValue(Objects.requireNonNull(value, "value"));
  }

  String getDraft() {
    return input.getValue();
  }

  void focusInput() {
    input.focus();
  }

  void setSpeechEnabled(boolean enabled) {
    speechEnabled = enabled;
    if (!enabled) {
      stopVoiceInput();
      speechFeedback = "";
    }
    updateSpeechControls();
  }

  boolean isSpeechEnabled() {
    return speechEnabled;
  }

  void setSpeechLanguage(String languageTag) {
    speechRecognizer.setLanguage(languageTag);
  }

  String getSpeechLanguage() {
    return speechRecognizer.getLanguage();
  }

  void setAutoSubmitSpeech(boolean autoSubmit) {
    autoSubmitSpeech = autoSubmit;
  }

  boolean isAutoSubmitSpeech() {
    return autoSubmitSpeech;
  }

  boolean isSpeechListening() {
    return speechState == SpeechRecognizer.State.LISTENING;
  }

  void startVoiceInput() {
    if (isSpeechAvailable()) {
      speechRecognizer.start();
    }
  }

  void stopVoiceInput() {
    speechRecognizer.stop();
  }

  void setBusy(boolean busy) {
    this.busy = busy;
    input.setEnabled(!busy);
    if (busy) {
      stopVoiceInput();
    }
    action.setText(busy ? "Stop" : "Send");
    action.setTheme(busy ? ButtonTheme.DANGER : ButtonTheme.PRIMARY);
    action.setPrefixComponent(busy ? DwcIcon.STOP.create() : FeatherIcon.ARROW_UP.create());
    action.setAttribute("aria-label", busy ? "Stop response" : "Send message");
    updateSpeechControls();
    stateChanged.run();
  }

  boolean isBusy() {
    return busy;
  }

  void reset() {
    stopVoiceInput();
    speechFeedback = "";
    setBusy(false);
    speechHint.setText("");
  }

  private void submitInput() {
    String draft = getDraft();
    if (draft != null) {
      submitPrompt.accept(draft);
    }
  }

  private void acceptSpeechResult(String rawTranscript, double confidence) {
    String transcript = rawTranscript == null ? "" : rawTranscript.trim();
    if (transcript.isEmpty()) {
      return;
    }
    String draft = getDraft();
    setDraft(draft == null || draft.isBlank()
        ? transcript : draft.stripTrailing() + " " + transcript);
    speechFeedback = "Voice added to draft";
    speechHint.setText(speechFeedback);
    transcriptionReceived.accept(transcript, confidence);
    if (!busy) {
      if (autoSubmitSpeech) {
        submitInput();
      } else {
        focusInput();
      }
    }
  }

  private void updateSpeechState(SpeechRecognizer.State state) {
    speechState = state;
    if (state == SpeechRecognizer.State.LISTENING) {
      speechFeedback = "";
    } else if (state == SpeechRecognizer.State.UNSUPPORTED) {
      speechFeedback = "Voice input isn’t supported in this browser";
    }
    updateSpeechControls();
    stateChanged.run();
  }

  private boolean isSpeechAvailable() {
    return speechEnabled && speechState != SpeechRecognizer.State.UNSUPPORTED && !busy;
  }

  private void updateSpeechControls() {
    boolean listening = isSpeechListening();
    speechAction.setVisible(speechEnabled);
    speechAction.setEnabled(isSpeechAvailable());
    speechAction.setText(listening ? "Stop" : "Speak");
    speechAction.setTheme(listening ? ButtonTheme.DANGER : ButtonTheme.GRAY);
    String label = speechState == SpeechRecognizer.State.UNSUPPORTED
        ? "Voice input is not supported" : listening ? "Stop voice input" : "Start voice input";
    speechAction.setAttribute("aria-label", label);
    speechHint.setText(!speechEnabled ? ""
        : listening && speechFeedback.isEmpty() ? "Listening… Speak now" : speechFeedback);
  }
}
