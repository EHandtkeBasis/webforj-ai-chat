import {
  collectFinalTranscript,
  type RecognitionResultLike,
  speechErrorMessage,
} from "./speech-recognizer-logic";

interface RecognitionEventLike extends Event {
  readonly resultIndex: number;
  readonly results: ArrayLike<RecognitionResultLike>;
}

interface RecognitionErrorEventLike extends Event {
  readonly error: string;
}

interface RecognitionLike {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  maxAlternatives: number;
  onend: ((event: Event) => void) | null;
  onerror: ((event: RecognitionErrorEventLike) => void) | null;
  onresult: ((event: RecognitionEventLike) => void) | null;
  onstart: ((event: Event) => void) | null;
  abort(): void;
  start(): void;
  stop(): void;
}

type RecognitionConstructor = new () => RecognitionLike;
type RecognitionState = "idle" | "listening" | "unsupported";

declare global {
  interface Window {
    SpeechRecognition?: RecognitionConstructor;
    webkitSpeechRecognition?: RecognitionConstructor;
  }
}

class SpeechRecognizerElement extends HTMLElement {
  private recognition: RecognitionLike | null = null;
  private listening = false;

  connectedCallback(): void {
    this.hidden = true;

    if (this.recognition) {
      return;
    }

    const Recognition =
      window.SpeechRecognition ?? window.webkitSpeechRecognition;

    if (!Recognition) {
      queueMicrotask(() => this.emitState("unsupported"));
      return;
    }

    const recognition = new Recognition();
    recognition.continuous = false;
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    recognition.onstart = () => {
      this.listening = true;
      this.emitState("listening");
    };
    recognition.onresult = (event) => {
      const result = collectFinalTranscript(event.results, event.resultIndex);
      if (result) {
        this.emit("speech-result", result);
      }
    };
    recognition.onerror = (event) => {
      if (event.error !== "aborted") {
        this.emit("speech-error", {
          code: event.error,
          message: speechErrorMessage(event.error),
        });
      }
    };
    recognition.onend = () => {
      this.listening = false;
      this.emitState("idle");
    };
    this.recognition = recognition;
    queueMicrotask(() => this.emitState("idle"));
  }

  disconnectedCallback(): void {
    const recognition = this.recognition;
    this.recognition = null;
    this.listening = false;

    if (!recognition) {
      return;
    }

    recognition.onstart = null;
    recognition.onresult = null;
    recognition.onerror = null;
    recognition.onend = null;
    try {
      recognition.abort();
    } catch {
      // The recognizer may already be inactive while the component is detached.
    }
  }

  start(): void {
    if (!this.recognition) {
      this.emitState("unsupported");
      this.emit("speech-error", {
        code: "unsupported",
        message: speechErrorMessage("unsupported"),
      });
      return;
    }

    if (this.listening) {
      return;
    }

    this.recognition.lang =
      this.getAttribute("language")?.trim() ||
      document.documentElement.lang ||
      navigator.language;

    try {
      this.listening = true;
      this.emitState("listening");
      this.recognition.start();
    } catch {
      this.listening = false;
      this.emit("speech-error", {
        code: "start-failed",
        message: speechErrorMessage("start-failed"),
      });
      this.emitState("idle");
    }
  }

  stop(): void {
    if (!this.recognition || !this.listening) {
      return;
    }

    try {
      this.recognition.stop();
    } catch {
      this.listening = false;
      this.emitState("idle");
    }
  }

  toggle(): void {
    if (this.listening) {
      this.stop();
    } else {
      this.start();
    }
  }

  private emitState(state: RecognitionState): void {
    this.emit("speech-state", { state });
  }

  private emit(name: string, detail: object): void {
    this.dispatchEvent(
      new CustomEvent(name, { detail, bubbles: true, composed: true }),
    );
  }
}

if (!customElements.get("webforj-speech-recognizer")) {
  customElements.define("webforj-speech-recognizer", SpeechRecognizerElement);
}
