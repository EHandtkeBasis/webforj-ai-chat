package com.example.aichat.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatConversationTest {
  private final ChatMessageList messages = mock(ChatMessageList.class);
  private final ChatComposer composer = mock(ChatComposer.class);
  private final AssistantMessage firstResponse = mock(AssistantMessage.class);
  private final AssistantMessage secondResponse = mock(AssistantMessage.class);
  private final ChatConversation conversation = new ChatConversation(messages, composer);

  @Test
  void composerStaysBusyUntilProgressiveRenderingCompletes() {
    when(messages.addAssistantMessage(true)).thenReturn(firstResponse);
    conversation.submit("first", prompt -> {});

    Runnable renderFinished = completeFirstResponse();

    assertTrue(conversation.isBusy());
    verify(composer, never()).setBusy(false);
    renderFinished.run();
    assertFalse(conversation.isBusy());
    verify(composer).setBusy(false);
    verify(composer).focusInput();
  }

  @Test
  void clearingConversationInvalidatesDelayedRenderCompletion() {
    when(messages.addAssistantMessage(true)).thenReturn(firstResponse, secondResponse);
    conversation.submit("first", prompt -> {});
    Runnable renderFinished = completeFirstResponse();

    conversation.clear();
    assertFalse(conversation.isBusy());
    verify(firstResponse).stop();
    verify(messages).clear();
    verify(composer).reset();

    conversation.submit("second", prompt -> {});
    clearInvocations(composer);
    renderFinished.run();

    assertTrue(conversation.isBusy());
    verify(composer, never()).setBusy(false);
    verify(composer, never()).focusInput();
  }

  @Test
  void cancelledRenderCannotFinishResponseStartedByStopListener() {
    when(messages.addAssistantMessage(true)).thenReturn(firstResponse, secondResponse);
    conversation.submit("first", prompt -> {});
    Runnable renderFinished = completeFirstResponse();

    conversation.cancel(prompt -> conversation.submit("second", submitted -> {}));
    clearInvocations(composer);
    renderFinished.run();

    assertTrue(conversation.isBusy());
    conversation.append("next chunk");
    verify(secondResponse).append("next chunk");
    verify(composer, never()).setBusy(false);
  }

  private Runnable completeFirstResponse() {
    conversation.complete();
    ArgumentCaptor<Runnable> completion = ArgumentCaptor.forClass(Runnable.class);
    verify(firstResponse).complete(completion.capture());
    return completion.getValue();
  }
}
