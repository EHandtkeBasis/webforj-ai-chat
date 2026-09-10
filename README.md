# webforJ AI Chat Component

A standalone, provider-agnostic chat UI built with webforJ 26.02. It takes the useful interaction
patterns from [ghost:ai](https://github.com/webforj/built-with-webforj/tree/main/webforj-ghostai)
and packages them as one reusable `AiChat` composite instead of coupling the view to Spring AI or a
specific model. The root project builds a normal library JAR with no Spring dependencies,
annotations, dependency injection, or application configuration. Spring Boot is used only by the
optional, separate `demo/` application.

## Included

- Progressive streaming markdown with smart auto-scroll
- Typed, removable prompt and stop events
- Send, thinking, streaming, error, and stopped states
- Restorable conversation history
- Optional quick-start prompts and replaceable identity, empty-state, and avatar content
- Responsive light/dark-theme-aware styling
- Keyboard support: Enter sends and Shift+Enter adds a line
- Browser speech-to-text with locale selection, permission/error feedback, and an unsupported-browser fallback
- A runnable provider-free demo and unit tests

## Build and use the library

Requirements: Java 21 and Maven 3.9+.

```shell
mvn clean install
```

This runs the Java and frontend tests and installs the component in your local Maven repository.
Add it to a plain webforJ 26.02 application (Spring is not required):

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>webforj-ai-chat-component</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Keep the webforJ Maven bundler enabled in the consuming application's build:

```xml
<plugin>
  <groupId>com.webforj</groupId>
  <artifactId>webforj-maven-plugin</artifactId>
  <version>26.02</version>
  <extensions>true</extensions>
</plugin>
```

Ensure the application has a `src/main/frontend` directory; a tracked `.gitkeep` is sufficient.
The library ships its CSS and speech-recognition sources under `META-INF/webforj/frontend`.
The application's bundler extracts and compiles them automatically; no source copying or Node
installation is needed. Run `mvn package` in the application to build its frontend.

The library uses only core webforJ modules, with JUnit and Mockito for tests. A Maven Enforcer
rule rejects direct and transitive Spring dependencies so this boundary stays enforced.

## Run the optional demo

After installing the library as above:

```shell
mvn -f demo/pom.xml package
mvn -f demo/pom.xml spring-boot:run
```

Open `http://localhost:8080`. The demo returns a local markdown response, so it needs no API key.
Spring dependencies and application settings live entirely in `demo/` and are not included in
the library JAR. After editing the component, run `mvn install` and rebuild/restart the demo.

## Customize the component

`AiChat` starts without a title, subtitle, artwork, empty-state copy, or example prompts. The demo
adds its own identity outside the reusable component:

```java
AiChat chat = new AiChat()
    .setTitle("Support")
    .setSubtitle("Answers from your knowledge base")
    .setHeaderIcon(TablerIcon.create("sparkles"))
    .setEmptyStateTitle("What can we help with?")
    .setEmptyStateDescription("Write a question or choose a starting point.")
    .setEmptyStateIcon(TablerIcon.create("message-chatbot"))
    .setUserLabel("Customer")
    .setAssistantLabel("Support")
    .setAssistantAvatarFactory(() -> TablerIcon.create("sparkles"))
    .setPlaceholder("Write your question...")
    .setSuggestions("Account access", "Billing question");
```

The avatar uses a factory because webforJ components can only be attached in one place and each
assistant message needs its own instance. Pass `null` to either icon setter or the avatar factory
to omit that visual entirely. Pass an empty string to title, subtitle, empty-state, or role-label
setters to hide that copy.

## Connect an AI provider

Create and configure the component, then bridge its two events to your model client:

```java
AiChat chat = new AiChat()
    .setTitle("Support Copilot")
    .setSubtitle("Answers grounded in your documentation")
    .setSuggestions("Summarize my account", "How do I reset access?");

chat.onPromptSubmit(event -> {
  provider.stream(event.getPrompt())
      .doOnNext(chunk -> Environment.runLater(() -> chat.appendResponse(chunk)))
      .doOnError(error -> Environment.runLater(() -> chat.failResponse(error.getMessage())))
      .doOnComplete(() -> Environment.runLater(chat::completeResponse))
      .subscribe();
});

chat.onStop(event -> provider.cancel());
```

The provider is intentionally not part of this project. The same UI works with Spring AI,
LangChain4j, an HTTP/SSE client, a WebSocket, or an in-process agent.

All component mutations must happen on the webforJ UI thread. Use
`com.webforj.Environment.runLater` when a provider calls back from a worker or network thread;
this is a webforJ API, not Spring's `Environment`.

## Speech to text

The microphone button transcribes one utterance into the editable draft. It doesn't submit by
default, which gives the user a chance to correct the transcript. To reproduce the reference
car-search flow that submits immediately:

```java
AiChat chat = new AiChat()
    .setSpeechLanguage("de-DE")
    .setAutoSubmitSpeech(true);

chat.onSpeechTranscription(event ->
    logger.info("Transcript confidence: {}", event.getConfidence()));
chat.onSpeechError(event ->
    logger.warn("Voice input failed ({}): {}", event.getCode(), event.getMessage()));
```

Pass an empty language to follow the page or browser locale. Speech recognition is a
[limited-availability browser API](https://developer.mozilla.org/en-US/docs/Web/API/SpeechRecognition),
so the component disables the microphone when the API is absent and remains fully usable as a
text chat. The browser requests microphone permission when dictation starts. Depending on the
browser, audio may be sent to a remote recognition service rather than processed offline.

## Public API

| Method | Purpose |
| --- | --- |
| `onPromptSubmit(listener)` | Receive the normalized user prompt |
| `appendResponse(chunk)` | Append a streamed markdown chunk |
| `completeResponse()` | Finish after progressive rendering drains |
| `failResponse(message)` | Show a user-facing provider error |
| `cancelResponse()` | Stop rendering and emit a stop event |
| `addMessage(role, content)` | Restore completed conversation history |
| `clear()` | Reset the transcript and welcome state |
| `setSuggestions(...)` | Replace empty-state quick prompts |
| `setTitle(value)` / `setSubtitle(value)` | Configure or hide header copy |
| `setHeaderIcon(component)` | Replace or remove header artwork |
| `setEmptyStateTitle(value)` / `setEmptyStateDescription(value)` | Configure empty-state copy |
| `setEmptyStateIcon(component)` | Replace or remove empty-state artwork |
| `setUserLabel(value)` / `setAssistantLabel(value)` | Configure message role labels |
| `setAssistantAvatarFactory(factory)` | Create optional per-message assistant avatars |
| `setDraft(value)` | Replace the editable composer draft |
| `setSpeechEnabled(enabled)` | Show or hide voice input |
| `setSpeechLanguage(tag)` | Set a BCP 47 recognition language |
| `setAutoSubmitSpeech(enabled)` | Optionally send final transcripts immediately |
| `startVoiceInput()` / `stopVoiceInput()` | Control dictation programmatically |
| `onSpeechTranscription(listener)` | Observe final transcripts and confidence |
| `onSpeechError(listener)` | Observe microphone, network, and support errors |

## Source structure

`AiChat` is the public facade: it connects the internal views and keeps the fluent API and nested
event types stable. The implementation is split into package-private collaborators:

| Class | Responsibility |
| --- | --- |
| `ChatHeader` | Optional identity content and status |
| `ChatMessageList` | Message rows, welcome state, suggestions, and presentation settings |
| `AssistantMessage` | Progressive markdown, completion, errors, and stopped output |
| `ChatComposer` | Editable draft, send/stop actions, and speech controls |
| `ChatConversation` | Active response lifetime, history restoration, and cancellation |

`SpeechRecognizer` remains the reusable browser adapter. A response stays busy until progressive
rendering finishes; delayed completion callbacks from cleared or cancelled turns cannot finish a
newer turn. Labels and the avatar factory apply when a message is created.

The root `src/main` contains only the reusable component and its frontend. The optional demo's
entry point, route, and application settings live under `demo/src/main` and consume the library
as a Maven dependency. The owning component classes load their frontend resources through
`@BundleEntry`.
