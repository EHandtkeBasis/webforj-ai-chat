package com.example.aichat.component;

import com.webforj.component.Component;
import com.webforj.component.html.elements.Div;
import com.webforj.component.html.elements.Span;
import com.webforj.component.icons.DwcIcon;
import com.webforj.component.markdown.MarkdownViewer;
import java.util.Map;

/** Renders one assistant message, including progressive output and terminal states. */
final class AssistantMessage {
  private final Div row = new Div();
  private final Div thinking = new Div();
  private final MarkdownViewer viewer = new MarkdownViewer();
  private boolean hasContent;
  private boolean complete;

  AssistantMessage(String label, Component avatarContent, boolean pending) {
    Span role = new Span(label);
    role.addClassName("ai-chat__role");
    role.setVisible(!label.isBlank());
    thinking.add(DwcIcon.ANIMATED_SPINNER.create(), new Span("Thinking..."));
    thinking.addClassName("ai-chat__thinking");
    thinking.setVisible(pending);
    viewer.setAutoScroll(true).setProgressiveRender(pending).setRenderSpeed(8);
    viewer.setVisible(false);
    viewer.addClassName("ai-chat__answer");

    Div bubble = new Div(thinking, viewer);
    bubble.addClassName("ai-chat__bubble");
    Div content = new Div(role, bubble);
    content.addClassName("ai-chat__message-content");
    if (avatarContent != null) {
      Div avatar = new Div(avatarContent);
      avatar.addClassName("ai-chat__avatar");
      row.add(avatar);
    }
    row.add(content);
    row.addClassName("ai-chat__message", "ai-chat__message--assistant");
  }

  Div getRow() {
    return row;
  }

  void append(String chunk) {
    if (complete) {
      throw new IllegalStateException("The assistant response is already complete");
    }
    if (chunk.isEmpty()) {
      return;
    }
    hasContent = true;
    thinking.setVisible(false);
    viewer.setVisible(true);
    viewer.append(chunk);
    reveal();
  }

  void complete(Runnable onRenderComplete) {
    if (!hasContent) {
      append("*No response was returned.*");
    }
    complete = true;
    viewer.whenRenderComplete().thenAccept(ignored -> onRenderComplete.run());
  }

  void fail(String message) {
    showFinalContent("**Something went wrong**\n\n" + escapeHtml(message));
  }

  void stop() {
    if (!hasContent) {
      showFinalContent("*Response stopped.*");
    } else {
      complete = true;
      viewer.stop();
      reveal();
    }
  }

  private void showFinalContent(String content) {
    thinking.setVisible(false);
    viewer.setProgressiveRender(false);
    viewer.setContent(content);
    viewer.setVisible(true);
    complete = true;
    reveal();
  }

  private void reveal() {
    row.getElement().callJsFunctionVoidAsync(
        "scrollIntoView", Map.of("behavior", "smooth", "block", "end"));
  }

  private static String escapeHtml(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
