export interface RecognitionAlternativeLike {
  readonly transcript: string;
  readonly confidence?: number;
}

export interface RecognitionResultLike {
  readonly isFinal: boolean;
  readonly length: number;
  readonly [index: number]: RecognitionAlternativeLike;
}

export interface TranscriptResult {
  readonly transcript: string;
  readonly confidence: number;
}

export function collectFinalTranscript(
  results: ArrayLike<RecognitionResultLike>,
  resultIndex: number,
): TranscriptResult | null {
  const parts: string[] = [];
  let confidence = 0;

  for (let index = resultIndex; index < results.length; index += 1) {
    const result = results[index];
    const alternative = result?.[0];
    const transcript = alternative?.transcript?.trim();

    if (!result?.isFinal || !transcript) {
      continue;
    }

    parts.push(transcript);
    confidence = Math.max(confidence, alternative.confidence ?? 0);
  }

  return parts.length === 0
    ? null
    : { transcript: parts.join(" "), confidence };
}

export function speechErrorMessage(code: string): string {
  switch (code) {
    case "not-allowed":
    case "service-not-allowed":
      return "Microphone access was denied.";
    case "audio-capture":
      return "No microphone is available.";
    case "network":
      return "Speech recognition could not reach its service.";
    case "no-speech":
      return "No speech was detected. Try again.";
    case "unsupported":
      return "Voice input is not supported in this browser.";
    default:
      return "Speech recognition failed. Try again.";
  }
}
