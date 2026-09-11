package com.example.aichat.bbj;

import com.example.aichat.component.AiChat;
import com.webforj.App;
import com.webforj.component.window.Frame;
import com.webforj.exceptions.WebforjException;

/** Chat host started by BBj Services, without a Spring context or an embedded server. */
public final class BBjChatApplication extends App {

  @Override
  public void run() throws WebforjException {
    AiChat chat = new AiChat()
        .setTitle("AI Chat")
        .setSubtitle("BBj Services demo")
        .setEmptyStateTitle("Start a conversation")
        .setEmptyStateDescription("Send a message to check the chat UI in BBj Services.")
        // BBj 26.01 reparents controls, cancelling the Markdown viewer's typewriter animation.
        .setProgressiveRender(false)
        .setSpeechLanguage("de-DE");

    // Replace this local response with the agreed CarIT/model integration on the webforJ UI thread.
    chat.onPromptSubmit(event -> {
      chat.appendResponse("The chat UI received your message.\n\n"
          + "This is a local demonstration response; CarIT and an AI model are not connected yet.");
      chat.completeResponse();
    });

    Frame frame = new Frame();
    frame.setTitle("AI Chat");
    frame.add(chat);
  }
}
