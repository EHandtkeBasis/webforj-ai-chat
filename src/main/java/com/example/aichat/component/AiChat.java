package com.example.aichat.component;

import com.webforj.bundle.annotation.BundleEntry;
import com.webforj.component.Component;
import com.webforj.component.Composite;
import com.webforj.component.html.elements.Div;
import com.webforj.concern.HasClassName;
import com.webforj.concern.HasStyle;
import com.webforj.dispatcher.EventDispatcher;
import com.webforj.dispatcher.EventListener;
import com.webforj.dispatcher.ListenerRegistration;
import java.util.EventObject;
import java.util.function.Supplier;

/**
 * A reusable, provider-agnostic AI chat interface.
 *
 * <p>Applications receive {@link PromptSubmitEvent}s and stream text back with
 * {@link #appendResponse(String)}. Internal views own rendering and input; this facade retains
 * the fluent configuration API and public events. All mutations must run on the webforJ UI thread.</p>
 */
@BundleEntry("chat/ai-chat.css")
public final class AiChat extends Composite<Div>
    implements HasStyle<AiChat>, HasClassName<AiChat> {

  /** The role used when restoring an existing conversation. */
  public enum Role {
    USER,
    ASSISTANT
  }

  private final Div self = getBoundComponent();
  private final EventDispatcher dispatcher = new EventDispatcher();
  private final ChatHeader header = new ChatHeader();
  private final ChatMessageList messages = new ChatMessageList(this::submitPrompt);
  private final ChatComposer composer = new ChatComposer(new SpeechRecognizer(),
      this::submitPrompt, this::cancelResponse, this::refreshStatus,
      (transcript, confidence) ->
          dispatcher.dispatchEvent(new SpeechTranscriptionEvent(this, transcript, confidence)),
      (code, message) -> dispatcher.dispatchEvent(new SpeechErrorEvent(this, code, message)));
  private final ChatConversation conversation = new ChatConversation(messages, composer);

  public AiChat() {
    self.addClassName("ai-chat");
    self.add(header, messages, composer);
  }

  /**
   * Submits a prompt as if it came from the composer.
   *
   * @param prompt prompt to submit; blank prompts are ignored
   * @return this component
   */
  public AiChat submitPrompt(String prompt) {
    conversation.submit(prompt,
        normalized -> dispatcher.dispatchEvent(new PromptSubmitEvent(this, normalized)));
    return this;
  }

  /** Adds a completed message without dispatching a prompt event. */
  public AiChat addMessage(Role role, String content) {
    conversation.addMessage(role, content);
    return this;
  }

  /** Appends one streamed markdown chunk to the active assistant response. */
  public AiChat appendResponse(String chunk) {
    conversation.append(chunk);
    return this;
  }

  /**
   * Marks the provider response complete. The composer unlocks after progressive rendering ends.
   */
  public AiChat completeResponse() {
    conversation.complete();
    return this;
  }

  /** Shows a user-facing error in the active response and returns the composer to idle. */
  public AiChat failResponse(String message) {
    conversation.fail(message);
    return this;
  }

  /** Stops the current response and emits a {@link StopEvent}. */
  public AiChat cancelResponse() {
    conversation.cancel(prompt -> dispatcher.dispatchEvent(new StopEvent(this, prompt)));
    return this;
  }

  /** Clears all visible messages and restores the welcome state. */
  public AiChat clear() {
    conversation.clear();
    return this;
  }

  /** Replaces the quick-start prompts displayed in the empty state. */
  public AiChat setSuggestions(String... prompts) {
    messages.setSuggestions(prompts);
    return this;
  }

  /**
   * Enables typewriter animation for future responses (enabled by default).
   * Disabling it displays each appended chunk immediately; provider streaming is unchanged.
   */
  public AiChat setProgressiveRender(boolean enabled) {
    messages.setProgressiveRender(enabled);
    return this;
  }

  public boolean isProgressiveRender() {
    return messages.isProgressiveRender();
  }

  public AiChat setTitle(String value) {
    header.setTitle(value);
    return this;
  }

  public String getTitle() {
    return header.getTitle();
  }

  public AiChat setSubtitle(String value) {
    header.setSubtitle(value);
    return this;
  }

  public String getSubtitle() {
    return header.getSubtitle();
  }

  /** Replaces the optional icon shown at the start of the header. Pass null to remove it. */
  public AiChat setHeaderIcon(Component icon) {
    header.setIcon(icon);
    return this;
  }

  public AiChat setEmptyStateTitle(String value) {
    messages.setEmptyStateTitle(value);
    return this;
  }

  public String getEmptyStateTitle() {
    return messages.getEmptyStateTitle();
  }

  public AiChat setEmptyStateDescription(String value) {
    messages.setEmptyStateDescription(value);
    return this;
  }

  public String getEmptyStateDescription() {
    return messages.getEmptyStateDescription();
  }

  /** Replaces the optional artwork shown above the empty-state copy. Pass null to remove it. */
  public AiChat setEmptyStateIcon(Component icon) {
    messages.setEmptyStateIcon(icon);
    return this;
  }

  public AiChat setUserLabel(String value) {
    messages.setUserLabel(value);
    return this;
  }

  public String getUserLabel() {
    return messages.getUserLabel();
  }

  public AiChat setAssistantLabel(String value) {
    messages.setAssistantLabel(value);
    return this;
  }

  public String getAssistantLabel() {
    return messages.getAssistantLabel();
  }

  /**
   * Sets a factory for optional assistant avatars. The factory is called once for each assistant
   * turn because a component instance can only be attached in one place. Pass null for no avatar.
   */
  public AiChat setAssistantAvatarFactory(Supplier<? extends Component> factory) {
    messages.setAssistantAvatarFactory(factory);
    return this;
  }

  public AiChat setPlaceholder(String value) {
    composer.setPlaceholder(value);
    return this;
  }

  public String getPlaceholder() {
    return composer.getPlaceholder();
  }

  /** Replaces the editable composer draft without submitting it. */
  public AiChat setDraft(String value) {
    composer.setDraft(value);
    return this;
  }

  public String getDraft() {
    return composer.getDraft();
  }

  /** Shows or hides voice input. It is enabled by default. */
  public AiChat setSpeechEnabled(boolean enabled) {
    composer.setSpeechEnabled(enabled);
    return this;
  }

  public boolean isSpeechEnabled() {
    return composer.isSpeechEnabled();
  }

  /**
   * Sets a BCP 47 recognition language, for example {@code en-US} or {@code de-DE}.
   * An empty value follows the document or browser language.
   */
  public AiChat setSpeechLanguage(String languageTag) {
    composer.setSpeechLanguage(languageTag);
    return this;
  }

  public String getSpeechLanguage() {
    return composer.getSpeechLanguage();
  }

  /** Controls whether a final voice transcript is submitted immediately. Defaults to false. */
  public AiChat setAutoSubmitSpeech(boolean autoSubmit) {
    composer.setAutoSubmitSpeech(autoSubmit);
    return this;
  }

  public boolean isAutoSubmitSpeech() {
    return composer.isAutoSubmitSpeech();
  }

  public boolean isSpeechListening() {
    return composer.isSpeechListening();
  }

  public AiChat startVoiceInput() {
    composer.startVoiceInput();
    return this;
  }

  public AiChat stopVoiceInput() {
    composer.stopVoiceInput();
    return this;
  }

  public int getMessageCount() {
    return messages.getMessageCount();
  }

  public boolean isBusy() {
    return conversation.isBusy();
  }

  public AiChat focusInput() {
    composer.focusInput();
    return this;
  }

  public ListenerRegistration<PromptSubmitEvent> onPromptSubmit(
      EventListener<PromptSubmitEvent> listener) {
    return dispatcher.addListener(PromptSubmitEvent.class, listener);
  }

  public ListenerRegistration<StopEvent> onStop(EventListener<StopEvent> listener) {
    return dispatcher.addListener(StopEvent.class, listener);
  }

  public ListenerRegistration<SpeechTranscriptionEvent> onSpeechTranscription(
      EventListener<SpeechTranscriptionEvent> listener) {
    return dispatcher.addListener(SpeechTranscriptionEvent.class, listener);
  }

  public ListenerRegistration<SpeechErrorEvent> onSpeechError(
      EventListener<SpeechErrorEvent> listener) {
    return dispatcher.addListener(SpeechErrorEvent.class, listener);
  }

  private void refreshStatus() {
    if (composer.isBusy()) {
      self.addClassName("ai-chat--busy");
    } else {
      self.removeClassName("ai-chat--busy");
    }
    if (composer.isSpeechListening()) {
      self.addClassName("ai-chat--listening");
    } else {
      self.removeClassName("ai-chat--listening");
    }
    header.updateStatus(composer.isBusy(), composer.isSpeechListening());
  }

  /** Event emitted after the component adds the user's prompt and opens a response turn. */
  public static final class PromptSubmitEvent extends EventObject {
    private final String prompt;

    private PromptSubmitEvent(AiChat source, String prompt) {
      super(source);
      this.prompt = prompt;
    }

    public AiChat getChat() {
      return (AiChat) getSource();
    }

    public String getPrompt() {
      return prompt;
    }
  }

  /** Event emitted when the user stops an active response. */
  public static final class StopEvent extends EventObject {
    private final String prompt;

    private StopEvent(AiChat source, String prompt) {
      super(source);
      this.prompt = prompt;
    }

    public AiChat getChat() {
      return (AiChat) getSource();
    }

    public String getPrompt() {
      return prompt;
    }
  }

  /** Event emitted after a final voice transcript is inserted into the draft. */
  public static final class SpeechTranscriptionEvent extends EventObject {
    private final String transcript;
    private final double confidence;

    private SpeechTranscriptionEvent(AiChat source, String transcript, double confidence) {
      super(source);
      this.transcript = transcript;
      this.confidence = confidence;
    }

    public AiChat getChat() {
      return (AiChat) getSource();
    }

    public String getTranscript() {
      return transcript;
    }

    public double getConfidence() {
      return confidence;
    }
  }

  /** Event emitted when browser speech recognition cannot complete. */
  public static final class SpeechErrorEvent extends EventObject {
    private final String code;
    private final String message;

    private SpeechErrorEvent(AiChat source, String code, String message) {
      super(source);
      this.code = code;
      this.message = message;
    }

    public AiChat getChat() {
      return (AiChat) getSource();
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }
  }
}
