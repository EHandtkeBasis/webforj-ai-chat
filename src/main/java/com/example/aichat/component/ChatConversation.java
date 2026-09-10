package com.example.aichat.component;

import java.util.Objects;
import java.util.function.Consumer;

/** Coordinates response lifetimes; rendering and input behavior belong to the child views. */
final class ChatConversation {
  private final ChatMessageList messages;
  private final ChatComposer composer;
  private ActiveTurn activeTurn;

  ChatConversation(ChatMessageList messages, ChatComposer composer) {
    this.messages = messages;
    this.composer = composer;
  }

  void submit(String prompt, Consumer<String> onSubmit) {
    String normalized = Objects.requireNonNull(prompt, "prompt").trim();
    if (normalized.isEmpty() || isBusy()) {
      return;
    }
    composer.setDraft("");
    messages.addUserMessage(normalized);
    activeTurn = new ActiveTurn(normalized, messages.addAssistantMessage(true));
    composer.setBusy(true);
    onSubmit.accept(normalized);
  }

  void addMessage(AiChat.Role role, String content) {
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(content, "content");
    if (isBusy()) {
      throw new IllegalStateException("Cannot restore messages while a response is active");
    }
    switch (role) {
      case USER -> messages.addUserMessage(content);
      case ASSISTANT -> messages.addAssistantMessage(false).append(content);
    }
  }

  void append(String chunk) {
    requireActiveTurn().message().append(Objects.requireNonNull(chunk, "chunk"));
  }

  void complete() {
    ActiveTurn turn = requireActiveTurn();
    turn.message().complete(() -> finish(turn));
  }

  void fail(String message) {
    ActiveTurn turn = requireActiveTurn();
    turn.message().fail(Objects.requireNonNull(message, "message"));
    finish(turn);
  }

  void cancel(Consumer<String> onStop) {
    if (activeTurn == null) {
      return;
    }
    ActiveTurn turn = activeTurn;
    turn.message().stop();
    finish(turn);
    onStop.accept(turn.prompt());
  }

  void clear() {
    ActiveTurn previous = activeTurn;
    activeTurn = null;
    if (previous != null) {
      previous.message().stop();
    }
    messages.clear();
    composer.reset();
  }

  boolean isBusy() {
    return activeTurn != null;
  }

  private ActiveTurn requireActiveTurn() {
    if (activeTurn == null) {
      throw new IllegalStateException("No assistant response is active");
    }
    return activeTurn;
  }

  private void finish(ActiveTurn turn) {
    // A delayed render callback must never unlock a newer response.
    if (activeTurn != turn) {
      return;
    }
    activeTurn = null;
    composer.setBusy(false);
    composer.focusInput();
  }

  private record ActiveTurn(String prompt, AssistantMessage message) {}
}
