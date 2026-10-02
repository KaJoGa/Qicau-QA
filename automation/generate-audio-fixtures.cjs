// One-off generator for the fake-microphone .wav fixtures VOICE-* scenarios need
// (test-plan.md §6.2: "clean sentences, silence, < 0.8 s clip, long (> 60 s) clip, noisy clip").
// These stand in for real speech at the mechanics level (recording start/stop, auto-stop timing,
// duration caps) - Selenium plays them back via Chrome's --use-file-for-fake-audio-capture flag
// (DriverFactory.java), so the *content* only needs to be non-silent (or silent, for the
// silence fixture) and the right length; actual speech-to-text accuracy is L2's job, not L4's.
//
// Run once with: node automation/generate-audio-fixtures.cjs
const fs = require('fs');
const path = require('path');

const SAMPLE_RATE = 16000;
const CHANNELS = 1;
const BITS_PER_SAMPLE = 16;

function writeWav(filePath, samples) {
  const dataSize = samples.length * 2; // 16-bit = 2 bytes/sample
  const buffer = Buffer.alloc(44 + dataSize);

  buffer.write('RIFF', 0);
  buffer.writeUInt32LE(36 + dataSize, 4);
  buffer.write('WAVE', 8);
  buffer.write('fmt ', 12);
  buffer.writeUInt32LE(16, 16); // fmt chunk size
  buffer.writeUInt16LE(1, 20); // PCM
  buffer.writeUInt16LE(CHANNELS, 22);
  buffer.writeUInt32LE(SAMPLE_RATE, 24);
  buffer.writeUInt32LE(SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8), 28); // byte rate
  buffer.writeUInt16LE(CHANNELS * (BITS_PER_SAMPLE / 8), 32); // block align
  buffer.writeUInt16LE(BITS_PER_SAMPLE, 34);
  buffer.write('data', 36);
  buffer.writeUInt32LE(dataSize, 40);

  for (let i = 0; i < samples.length; i++) {
    buffer.writeInt16LE(samples[i], 44 + i * 2);
  }
  fs.writeFileSync(filePath, buffer);
  console.log('wrote', filePath, `(${(samples.length / SAMPLE_RATE).toFixed(2)}s)`);
}

function silence(durationSec) {
  return new Int16Array(Math.round(SAMPLE_RATE * durationSec));
}

function tone(durationSec, freqHz = 440, amplitude = 8000) {
  const n = Math.round(SAMPLE_RATE * durationSec);
  const samples = new Int16Array(n);
  for (let i = 0; i < n; i++) {
    samples[i] = Math.round(amplitude * Math.sin((2 * Math.PI * freqHz * i) / SAMPLE_RATE));
  }
  return samples;
}

function whiteNoise(durationSec, amplitude = 6000) {
  const n = Math.round(SAMPLE_RATE * durationSec);
  const samples = new Int16Array(n);
  for (let i = 0; i < n; i++) {
    samples[i] = Math.round((Math.random() * 2 - 1) * amplitude);
  }
  return samples;
}

const OUT = path.join(__dirname, 'src', 'test', 'resources', 'audio');

// clean.wav - VOICE-01/02/08: a steady tone standing in for clear, unambiguous speech (~3s)
writeWav(path.join(OUT, 'clean.wav'), tone(3.0));

// silence.wav - VOICE-03: >=2s of true silence, to trigger the 2s auto-stop
writeWav(path.join(OUT, 'silence.wav'), silence(3.0));

// short-under-0.8s.wav - VOICE-05: an accidental sub-0.8s clip that should be ignored entirely
writeWav(path.join(OUT, 'short-under-0.8s.wav'), tone(0.3));

// long-over-60s.wav - VOICE-04: continuous non-silent audio past the 60s hard cap
writeWav(path.join(OUT, 'long-over-60s.wav'), tone(65.0));

// noisy.wav - low-confidence-leaning input (VOICE-09/MAN-05 style), per test-plan.md §6.2
writeWav(path.join(OUT, 'noisy.wav'), whiteNoise(3.0));
