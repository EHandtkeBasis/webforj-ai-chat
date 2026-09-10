package com.example.aichat.component;

import com.webforj.component.Component;
import com.webforj.component.Composite;
import com.webforj.component.html.elements.Div;
import com.webforj.component.html.elements.H2;
import com.webforj.component.html.elements.Span;
import java.util.Objects;

/** Owns the optional identity content and conversation status. */
final class ChatHeader extends Composite<Div> {
  private final Div icon = new Div();
  private final H2 title = new H2("");
  private final Span subtitle = new Span("");
  private final Span status = new Span("Ready");
  private final Div copy = new Div(title, subtitle);

  ChatHeader() {
    icon.addClassName("ai-chat__brand-icon");
    icon.setVisible(false);
    title.addClassName("ai-chat__title");
    title.setVisible(false);
    subtitle.addClassName("ai-chat__subtitle");
    subtitle.setVisible(false);
    copy.addClassName("ai-chat__brand-copy");
    copy.setVisible(false);

    Span dot = new Span();
    dot.addClassName("ai-chat__status-dot");
    Div statusWrap = new Div(dot, status);
    statusWrap.addClassName("ai-chat__status");

    getBoundComponent().addClassName("ai-chat__header");
    getBoundComponent().add(icon, copy, statusWrap);
  }

  void setTitle(String value) {
    title.setText(Objects.requireNonNull(value, "value"));
    title.setVisible(!value.isBlank());
    updateCopyVisibility();
  }

  String getTitle() {
    return title.getText();
  }

  void setSubtitle(String value) {
    subtitle.setText(Objects.requireNonNull(value, "value"));
    subtitle.setVisible(!value.isBlank());
    updateCopyVisibility();
  }

  String getSubtitle() {
    return subtitle.getText();
  }

  void setIcon(Component content) {
    icon.removeAll();
    icon.setVisible(content != null);
    if (content != null) {
      icon.add(content);
    }
  }

  void updateStatus(boolean busy, boolean listening) {
    status.setText(busy ? "Responding" : listening ? "Listening" : "Ready");
  }

  private void updateCopyVisibility() {
    copy.setVisible(!title.getText().isBlank() || !subtitle.getText().isBlank());
  }
}
