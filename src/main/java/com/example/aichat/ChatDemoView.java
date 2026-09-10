package com.example.aichat;

import com.example.aichat.component.AiChat;
import com.webforj.component.Composite;
import com.webforj.component.icons.TablerIcon;
import com.webforj.component.layout.flexlayout.FlexAlignment;
import com.webforj.component.layout.flexlayout.FlexDirection;
import com.webforj.component.layout.flexlayout.FlexJustifyContent;
import com.webforj.component.layout.flexlayout.FlexLayout;
import com.webforj.router.annotation.Route;

@Route("/")
public final class ChatDemoView extends Composite<FlexLayout> {

  public ChatDemoView() {
    FlexLayout self = getBoundComponent();
    self.setDirection(FlexDirection.COLUMN)
        .setAlignment(FlexAlignment.CENTER)
        .setJustifyContent(FlexJustifyContent.CENTER)
        .setStyle("minHeight", "100vh")
        .setStyle("padding", "var(--dwc-space-xl)")
        .setStyle("background", "var(--dwc-surface-1)");

    AiChat chat = new AiChat()
        .setTitle("Orbit AI")
        .setSubtitle("Reusable chat UI · bring your own model")
        .setHeaderIcon(TablerIcon.create("sparkles", TablerIcon.Variate.FILLED))
        .setEmptyStateTitle("How can Orbit help?")
        .setEmptyStateDescription(
            "Ask a question below or start with one of these example prompts.")
        .setEmptyStateIcon(
            TablerIcon.create("message-chatbot", TablerIcon.Variate.FILLED))
        .setAssistantLabel("Orbit")
        .setAssistantAvatarFactory(
            () -> TablerIcon.create("sparkles", TablerIcon.Variate.FILLED))
        .setPlaceholder("Ask Orbit anything...")
        .setSuggestions(
            "What can this component do?",
            "Show me the integration API",
            "How does streaming work?");

    chat.onPromptSubmit(event -> {
      chat.appendResponse(demoResponse());
      chat.completeResponse();
    });

    self.add(chat);
  }

  private static String demoResponse() {
    return "This demo keeps the model layer deliberately separate. Listen for a prompt, send it "
        + "to your AI provider, and pass each returned markdown chunk to the component.\n\n"
        + "```java\n"
        + "chat.onPromptSubmit(event -> provider.stream(event.getPrompt())\n"
        + "    .doOnNext(chat::appendResponse)\n"
        + "    .doOnComplete(chat::completeResponse));\n"
        + "```\n\n"
        + "The UI handles message layout, progressive markdown rendering, auto-scroll, empty "
        + "state, responsive styling, and send/stop state.";
  }
}
