import { describe, expect, test } from "bun:test";
import {
  collectFinalTranscript,
  speechErrorMessage,
} from "./speech-recognizer-logic";

describe("speech recognizer logic", () => {
  test("collects only new final transcript segments", () => {
    const previous = Object.assign(
      [{ transcript: "already handled", confidence: 0.7 }],
      { isFinal: true },
    );
    const interim = Object.assign(
      [{ transcript: "still listening", confidence: 0.6 }],
      { isFinal: false },
    );
    const final = Object.assign(
      [{ transcript: "hello world", confidence: 0.92 }],
      { isFinal: true },
    );

    expect(collectFinalTranscript([previous, interim, final], 1)).toEqual({
      transcript: "hello world",
      confidence: 0.92,
    });
  });

  test("returns null when no final speech is available", () => {
    const interim = Object.assign([{ transcript: "hello" }], {
      isFinal: false,
    });

    expect(collectFinalTranscript([interim], 0)).toBeNull();
  });

  test("provides actionable browser error messages", () => {
    expect(speechErrorMessage("not-allowed")).toBe(
      "Microphone access was denied.",
    );
    expect(speechErrorMessage("audio-capture")).toBe(
      "No microphone is available.",
    );
  });
});
