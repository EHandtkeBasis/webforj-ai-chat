package com.example.aichat.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aichat.component.AiChat.PromptSubmitEvent;
import com.webforj.dispatcher.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiChatTest {

  @Test
  void configurationIsFluent() {
    AiChat chat = new AiChat();

    assertSame(chat, chat.setTitle("Support Copilot"));
    assertSame(chat, chat.setSubtitle("Answers from the help center"));
    assertSame(chat, chat.setHeaderIcon(null));
    assertSame(chat, chat.setEmptyStateTitle("Start here"));
    assertSame(chat, chat.setEmptyStateDescription("Choose a prompt or write your own."));
    assertSame(chat, chat.setEmptyStateIcon(null));
    assertSame(chat, chat.setUserLabel("Customer"));
    assertSame(chat, chat.setAssistantLabel("Support"));
    assertSame(chat, chat.setAssistantAvatarFactory(() -> null));
    assertSame(chat, chat.setPlaceholder("Ask a question"));
    assertSame(chat, chat.setDraft("A draft"));
    assertSame(chat, chat.setSpeechLanguage("de-DE"));
    assertSame(chat, chat.setAutoSubmitSpeech(true));

    assertEquals("Support Copilot", chat.getTitle());
    assertEquals("Answers from the help center", chat.getSubtitle());
    assertEquals("Start here", chat.getEmptyStateTitle());
    assertEquals("Choose a prompt or write your own.", chat.getEmptyStateDescription());
    assertEquals("Customer", chat.getUserLabel());
    assertEquals("Support", chat.getAssistantLabel());
    assertEquals("Ask a question", chat.getPlaceholder());
    assertEquals("A draft", chat.getDraft());
    assertEquals("de-DE", chat.getSpeechLanguage());
    assertTrue(chat.isAutoSubmitSpeech());
    assertTrue(chat.isSpeechEnabled());
  }

  @Test
  void componentStartsWithoutBrandingOrExampleCopy() {
    AiChat chat = new AiChat();

    assertEquals("", chat.getTitle());
    assertEquals("", chat.getSubtitle());
    assertEquals("", chat.getEmptyStateTitle());
    assertEquals("", chat.getEmptyStateDescription());
    assertTrue(chat.isProgressiveRender());
  }

  @Test
  void disablingAnimationCompletesResponsesWithoutWaitingForBrowserAnimation() {
    AiChat chat = new AiChat();
    assertSame(chat, chat.setProgressiveRender(false));
    assertFalse(chat.isProgressiveRender());

    chat.submitPrompt("first").appendResponse("one ").appendResponse("two").completeResponse();
    assertFalse(chat.isBusy());
    assertEquals(2, chat.getMessageCount());

    chat.clear();
    assertFalse(chat.isProgressiveRender());
    chat.submitPrompt("empty response").completeResponse();
    assertFalse(chat.isBusy());

    chat.setProgressiveRender(true).submitPrompt("animated").appendResponse("reply").completeResponse();
    assertTrue(chat.isBusy());
    chat.cancelResponse();
  }

  @Test
  void invalidSpeechLanguageIsRejected() {
    AiChat chat = new AiChat();

    assertThrows(IllegalArgumentException.class, () -> chat.setSpeechLanguage("not_a_tag"));
  }

  @Test
  void submitDispatchesTypedRemovableEvent() {
    AiChat chat = new AiChat();
    List<String> prompts = new ArrayList<>();
    ListenerRegistration<PromptSubmitEvent> registration =
        chat.onPromptSubmit(event -> prompts.add(event.getPrompt()));

    chat.submitPrompt("  hello  ");

    assertEquals(List.of("hello"), prompts);
    assertTrue(chat.isBusy());
    assertEquals(2, chat.getMessageCount());

    chat.cancelResponse();
    registration.remove();
    chat.submitPrompt("second");

    assertEquals(List.of("hello"), prompts);
  }

  @Test
  void stopEventCarriesTheActivePrompt() {
    AiChat chat = new AiChat();
    List<String> stopped = new ArrayList<>();
    chat.onStop(event -> stopped.add(event.getPrompt()));

    chat.submitPrompt("cancel me").appendResponse("partial");
    chat.cancelResponse();

    assertEquals(List.of("cancel me"), stopped);
    assertFalse(chat.isBusy());
  }

  @Test
  void blankPromptIsIgnored() {
    AiChat chat = new AiChat();

    chat.submitPrompt("   ");

    assertFalse(chat.isBusy());
    assertEquals(0, chat.getMessageCount());
  }

  @Test
  void responseRequiresAnActiveTurn() {
    AiChat chat = new AiChat();

    assertThrows(IllegalStateException.class, () -> chat.appendResponse("orphan"));
    assertThrows(IllegalStateException.class, chat::completeResponse);
  }

  @Test
  void clearRestoresIdleState() {
    AiChat chat = new AiChat();
    chat.submitPrompt("hello").appendResponse("world");

    assertSame(chat, chat.clear());

    assertFalse(chat.isBusy());
    assertEquals(0, chat.getMessageCount());
  }

  @Test
  void activeResponseBlocksSubmissionsAndHistoryRestorationUntilFailure() {
    AiChat chat = new AiChat();
    List<String> prompts = new ArrayList<>();
    chat.onPromptSubmit(event -> {
      assertSame(chat, event.getChat());
      assertTrue(chat.isBusy());
      prompts.add(event.getPrompt());
    });

    chat.submitPrompt("first");
    chat.submitPrompt("ignored");
    assertThrows(IllegalStateException.class, () -> chat.addMessage(AiChat.Role.USER, "history"));
    assertEquals(2, chat.getMessageCount());

    chat.failResponse("Provider unavailable");
    assertFalse(chat.isBusy());
    chat.submitPrompt("second");
    assertEquals(List.of("first", "second"), prompts);
    assertEquals(4, chat.getMessageCount());
  }

  @Test
  void clearPreservesConfigurationAndCreatesAvatarsForNewAssistantMessagesOnly() {
    AiChat chat = new AiChat().setTitle("Custom title").setAssistantLabel("Custom assistant");
    List<String> avatars = new ArrayList<>();
    List<String> prompts = new ArrayList<>();
    chat.setAssistantAvatarFactory(() -> {
      avatars.add("created");
      return null;
    });
    chat.onPromptSubmit(event -> prompts.add(event.getPrompt()));

    chat.addMessage(AiChat.Role.USER, "history");
    chat.addMessage(AiChat.Role.ASSISTANT, "answer");
    assertTrue(prompts.isEmpty());
    assertEquals(1, avatars.size());
    chat.clear().submitPrompt("new prompt");

    assertEquals("Custom title", chat.getTitle());
    assertEquals("Custom assistant", chat.getAssistantLabel());
    assertEquals(2, avatars.size());
    assertEquals(2, chat.getMessageCount());
    assertEquals(List.of("new prompt"), prompts);
  }
}
