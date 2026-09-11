package com.example.aichat.component;

import com.webforj.component.Component;
import com.webforj.component.Composite;
import com.webforj.component.button.Button;
import com.webforj.component.html.elements.Div;
import com.webforj.component.html.elements.H2;
import com.webforj.component.html.elements.Paragraph;
import com.webforj.component.html.elements.Span;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Owns conversation rows, their presentation settings, and the welcome state. */
final class ChatMessageList extends Composite<Div> {
  private final Div emptyState = new Div();
  private final Div emptyIcon = new Div();
  private final H2 emptyTitle = new H2("");
  private final Paragraph emptyDescription = new Paragraph("");
  private final Div suggestions = new Div();
  private final List<Div> rows = new ArrayList<>();
  private final Consumer<String> submitPrompt;
  private String userLabel = "You";
  private String assistantLabel = "Assistant";
  private boolean progressiveRender = true;
  private Supplier<? extends Component> assistantAvatarFactory;

  ChatMessageList(Consumer<String> submitPrompt) {
    this.submitPrompt = submitPrompt;
    emptyIcon.addClassName("ai-chat__empty-icon");
    emptyIcon.setVisible(false);
    emptyTitle.addClassName("ai-chat__empty-title");
    emptyTitle.setVisible(false);
    emptyDescription.addClassName("ai-chat__empty-description");
    emptyDescription.setVisible(false);
    suggestions.addClassName("ai-chat__suggestions");
    emptyState.addClassName("ai-chat__empty");
    emptyState.add(emptyIcon, emptyTitle, emptyDescription, suggestions);
    getBoundComponent().addClassName("ai-chat__messages");
    getBoundComponent().add(emptyState);
  }

  void setSuggestions(String... prompts) {
    suggestions.removeAll();
    if (prompts == null) {
      return;
    }
    for (String prompt : prompts) {
      if (prompt == null || prompt.isBlank()) {
        continue;
      }
      String normalized = prompt.trim();
      Button suggestion = new Button(normalized);
      suggestion.addClassName("ai-chat__suggestion");
      suggestion.onClick(event -> submitPrompt.accept(normalized));
      suggestions.add(suggestion);
    }
  }

  void setEmptyStateTitle(String value) {
    emptyTitle.setText(Objects.requireNonNull(value, "value"));
    emptyTitle.setVisible(!value.isBlank());
  }

  String getEmptyStateTitle() {
    return emptyTitle.getText();
  }

  void setEmptyStateDescription(String value) {
    emptyDescription.setText(Objects.requireNonNull(value, "value"));
    emptyDescription.setVisible(!value.isBlank());
  }

  String getEmptyStateDescription() {
    return emptyDescription.getText();
  }

  void setEmptyStateIcon(Component content) {
    emptyIcon.removeAll();
    emptyIcon.setVisible(content != null);
    if (content != null) {
      emptyIcon.add(content);
    }
  }

  void setUserLabel(String value) {
    userLabel = Objects.requireNonNull(value, "value");
  }

  String getUserLabel() {
    return userLabel;
  }

  void setAssistantLabel(String value) {
    assistantLabel = Objects.requireNonNull(value, "value");
  }

  String getAssistantLabel() {
    return assistantLabel;
  }

  void setAssistantAvatarFactory(Supplier<? extends Component> factory) {
    assistantAvatarFactory = factory;
  }

  void setProgressiveRender(boolean enabled) {
    progressiveRender = enabled;
  }

  boolean isProgressiveRender() {
    return progressiveRender;
  }

  void addUserMessage(String text) {
    Span role = new Span(userLabel);
    role.addClassName("ai-chat__role");
    role.setVisible(!userLabel.isBlank());
    Div bubble = new Div(text);
    bubble.addClassName("ai-chat__bubble");
    Div content = new Div(role, bubble);
    content.addClassName("ai-chat__message-content");
    Div row = new Div(content);
    row.addClassName("ai-chat__message", "ai-chat__message--user");
    addRow(row);
  }

  AssistantMessage addAssistantMessage(boolean pending) {
    Component avatar = assistantAvatarFactory == null ? null : assistantAvatarFactory.get();
    AssistantMessage message = new AssistantMessage(assistantLabel, avatar, pending, progressiveRender);
    addRow(message.getRow());
    return message;
  }

  int getMessageCount() {
    return rows.size();
  }

  void clear() {
    getBoundComponent().remove(rows.toArray(Div[]::new));
    rows.clear();
    emptyState.setVisible(true);
  }

  private void addRow(Div row) {
    emptyState.setVisible(false);
    getBoundComponent().add(row);
    rows.add(row);
    reveal(row);
  }

  private static void reveal(Div row) {
    row.getElement().callJsFunctionVoidAsync(
        "scrollIntoView", Map.of("behavior", "smooth", "block", "end"));
  }
}
