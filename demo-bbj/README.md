# Deploy the chat application into BBj Services

This is the host for running the chat inside BBj Services alongside a BBj-based system such as
CarIT. It does not start Spring, Tomcat, or a separate webforJ engine. The reusable library in the
repository root stays independent of this deployment choice.

## Build without deploying

From the repository root, using Java 21+ and Maven 3.9+:

```shell
mvn clean install
mvn -f demo-bbj/pom.xml clean package
```

The deployable application is `demo-bbj/target/webforj-ai-chat-bbj-1.0-SNAPSHOT.jar`.
It is a BBj-hosted application, not an executable `java -jar` or Spring Boot application.
Normal `package` and `install` builds do not contact a BBj server.

The build incorporates the chat library's classes into the application JAR and removes that local
snapshot from its embedded deployment POM. The BBj server therefore does not need your developer
machine's Maven repository or a separately published chat library. The remaining webforJ
dependencies stay external and must be resolvable by the target installation.

The webforJ bundler extracts the library's frontend sources and builds an eager bundle. BBj
Services loads that bundle inline from the application classpath, including both the stylesheet
and the speech-recognition adapter. Do not replace the generated frontend index with the
library's per-component index. The build intentionally copies only the library's Java classes
into the application JAR.

The dependency checks reject Spring and `webforj-engine`. BBj Services supplies the engine, as
required by the [official BBj deployment configuration](https://docs.webforj.com/docs/configuration/bbj-installation/configuration).

The host uses a frame title instead of `@AppProfile`, whose default `icons://` resource is not
supported by BBj Services. It also calls `setProgressiveRender(false)`: the Markdown viewer shipped
with local BBj 26.01 cancels its typewriter animation when a control is reparented. Chunks still
render immediately, and responses can complete normally. Other hosts keep animation enabled by
default; the reusable component contains no BBj-specific runtime checks.

## Prepare the target server

Have the CarIT/BBj administrator confirm:

- The server's BBj Services and webforJ/DWCJ plugin versions support this project's webforJ 26.02
  and Java 21 bytecode. Do not change or upgrade a shared CarIT server without coordination.
- The webforJ/DWCJ plugin is installed and its **Enable Maven Remote Install** option is enabled.
- The intended deployment URL is reachable from the machine performing the deployment, typically
  `http://BBJ_HOST:8888/webforj-install` (use the server's actual scheme and port).
- The published application name `ai-chat` is available, or is the application you intend
  to update. Change `<publishname>` in this project's POM before deploying under another name.

See the [BBj installation guide](https://docs.webforj.com/docs/configuration/bbj-installation/local)
for the server-side plugin setup. Keep the remote-install endpoint restricted to trusted
administrators and use the authentication and transport configuration approved for that server.
Do not commit server credentials to this repository.

## Deploy explicitly

After confirming the target, replace `BBJ_HOST` and the port/scheme below:

```shell
mvn -f demo-bbj/pom.xml -Pbbj-deploy install "-Dbbj.deployUrl=http://BBJ_HOST:8888/webforj-install"
```

This command publishes or updates `ai-chat` on that server. The profile requires an
explicit deployment URL; it never silently defaults to a server. The entry point is
`com.example.aichat.bbj.BBjChatApplication`, which BBj Services starts through `App.run()`.

Inspect the installer response, then open the target server's `/webapp/ai-chat` URL.
A Maven success message alone is not a runtime smoke test. Check that the chat is styled, a sent
message receives the local demo reply, and the browser console has no missing frontend resources.
For microphone input, use HTTPS (or localhost), a supporting browser, and microphone permission.

### Local deployment

With BBj Services running locally and remote installation enabled:

```shell
mvn clean install
mvn -f demo-bbj/pom.xml -Pbbj-deploy clean install "-Dbbj.deployUrl=http://localhost:8888/webforj-install"
```

Open [AI Chat](http://localhost:8888/webapp/ai-chat).

Verified locally on 2026-09-11 with the existing BBj Services 26.01 installation, without restarting
or upgrading it. The clean build passes 20 Java tests and 3 frontend tests. Browser smoke checks
passed for asset loading, typed prompts and replies, Enter/Shift+Enter, a 390px viewport, simulated
German speech transcription, and the unsupported-speech fallback, with no console or resource errors.
Real microphone recognition and CarIT connectivity were not tested.

During local testing, IDE background compilation interfered with Maven's `target` output. A clean
build of the same sources outside the IDE workspace produced the verified deployment. If resources
disappear during a build, pause IDE auto-build or use a clean checkout outside its watched workspace.
The library JAR must include `META-INF/webforj/frontend/` and the host JAR must include
`META-INF/webforj/frontend-entries.json` and the generated `static/frontend/` bundle.

BBj can retain closed browser sessions temporarily. If a rapid series of tests exhausts session
licenses, allow those sessions to expire or ask the administrator to release the identified test
sessions; do not restart a shared server or terminate unrelated sessions.

## CarIT integration is a separate step

Hosting in BBj Services satisfies the requested deployment arrangement; it does not itself
establish a CarIT connection or share a user's CarIT session. The demo currently returns a local
response and does not read or write any CarIT data.

Agree on the CarIT-side interface and session/authentication requirements before replacing the
`onPromptSubmit` handler in `BBjChatApplication`. Forward model output through `appendResponse`,
`completeResponse`, and `failResponse`, and connect `onStop` to cancellation. Keep those adapters
in the host application, not in the reusable chat component, and perform component updates on
the webforJ UI thread.
