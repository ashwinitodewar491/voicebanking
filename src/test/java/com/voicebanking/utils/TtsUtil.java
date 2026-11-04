package com.voicebanking.utils;

import com.voicebanking.utils.tts.EdgeTtsEngine;
import com.voicebanking.utils.tts.TtsFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TtsUtil {

    /** Generates a WAV for {@code text} with 3s of trailing silence so the VAD triggers STT before
     * release. */
    public static String generateWav(String text) throws Exception {
        String path = TtsFactory.create().generate(text);
        appendSilenceToWav(path, 3000);
        return path;
    }

    /** Same as {@link #generateWav(String)} but with an explicit edge-tts voice, for locales
     * other than the default English (e.g. {@link EdgeTtsEngine#VOICE_HINDI}). */
    public static String generateWav(String text, String voice) throws Exception {
        String path = new EdgeTtsEngine().generate(text, voice);
        appendSilenceToWav(path, 3000);
        return path;
    }

    /** Same as {@link #generateWav(String, String)} with a rate/pitch offset. */
    public static String generateWavWithVariation(String text, String voice, String rate, String pitch)
            throws Exception {
        String path = new EdgeTtsEngine().generate(text, voice, rate, pitch);
        appendSilenceToWav(path, 3000);
        return path;
    }

    /** WAV duration in ms, found by scanning the RIFF chunks. */
    public static long getWavDurationMs(String wavPath) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(wavPath, "r")) {
            raf.seek(12);
            int sampleRate = 0, channels = 0, bitsPerSample = 0;
            long dataSize = 0;

            while (raf.getFilePointer() < raf.length() - 8) {
                byte[] id = new byte[4];
                raf.read(id);
                String chunkId = new String(id);
                byte[] sz = new byte[4];
                raf.read(sz);
                int chunkSize = ByteBuffer.wrap(sz).order(ByteOrder.LITTLE_ENDIAN).getInt();

                if ("fmt ".equals(chunkId)) {
                    byte[] fmt = new byte[chunkSize];
                    raf.read(fmt);
                    ByteBuffer b = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN);
                    channels      = b.getShort(2) & 0xFFFF;
                    sampleRate    = b.getInt(4);
                    bitsPerSample = b.getShort(14) & 0xFFFF;
                } else if ("data".equals(chunkId)) {
                    dataSize = Integer.toUnsignedLong(chunkSize);
                    break;
                } else {
                    raf.skipBytes(chunkSize);
                }
            }

            if (sampleRate == 0 || dataSize == 0) return 4000;
            return (dataSize * 1000L) / ((long) sampleRate * channels * (bitsPerSample / 8));
        }
    }

    /** Rewrites the WAV as a minimal RIFF file with {@code silenceMs} of silence appended. */
    private static void appendSilenceToWav(String wavPath, int silenceMs) throws Exception {
        int sampleRate = 0, channels = 0, bitsPerSample = 0;
        byte[] pcmData = null;

        try (RandomAccessFile raf = new RandomAccessFile(wavPath, "r")) {
            raf.seek(12);
            while (raf.getFilePointer() < raf.length() - 8) {
                byte[] id = new byte[4];
                raf.read(id);
                String chunkId = new String(id);
                byte[] sz = new byte[4];
                raf.read(sz);
                int chunkSize = ByteBuffer.wrap(sz).order(ByteOrder.LITTLE_ENDIAN).getInt();

                if ("fmt ".equals(chunkId)) {
                    byte[] fmt = new byte[chunkSize];
                    raf.read(fmt);
                    ByteBuffer b = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN);
                    channels      = b.getShort(2) & 0xFFFF;
                    sampleRate    = b.getInt(4);
                    bitsPerSample = b.getShort(14) & 0xFFFF;
                } else if ("data".equals(chunkId)) {
                    pcmData = new byte[(int) Integer.toUnsignedLong(chunkSize)];
                    raf.readFully(pcmData);
                    break;
                } else {
                    raf.skipBytes(chunkSize);
                }
            }
        }

        if (sampleRate == 0 || pcmData == null) return;

        int silenceBytes    = (int)(silenceMs / 1000.0 * sampleRate * channels * (bitsPerSample / 8));
        int totalDataBytes  = pcmData.length + silenceBytes;
        int riffContentSize = 4 + 8 + 16 + 8 + totalDataBytes;

        try (RandomAccessFile raf = new RandomAccessFile(wavPath, "rw")) {
            raf.setLength(0);

            raf.write(new byte[]{'R','I','F','F'});
            raf.write(le4(riffContentSize));
            raf.write(new byte[]{'W','A','V','E'});

            raf.write(new byte[]{'f','m','t',' '});
            raf.write(le4(16));
            ByteBuffer fmt = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
            fmt.putShort((short) 1);
            fmt.putShort((short) channels);
            fmt.putInt(sampleRate);
            fmt.putInt(sampleRate * channels * (bitsPerSample / 8));
            fmt.putShort((short)(channels * (bitsPerSample / 8)));
            fmt.putShort((short) bitsPerSample);
            raf.write(fmt.array());

            raf.write(new byte[]{'d','a','t','a'});
            raf.write(le4(totalDataBytes));
            raf.write(pcmData);
            raf.write(new byte[silenceBytes]);
        }
    }

    private static byte[] le4(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    /** With {@code -DkeepAudio=true}, copies the WAV to {@code target/kept-audio/<label>.wav} for
     * debugging. */
    public static void keepCopy(String wavPath, String label) {
        if (!Boolean.getBoolean("keepAudio") || wavPath == null) return;
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("target", "kept-audio");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path copy = dir.resolve(label.replaceAll("[^\\w.+-]", "_") + ".wav");
            java.nio.file.Files.copy(java.nio.file.Path.of(wavPath), copy,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[KeepAudio] Saved " + copy.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("[KeepAudio] Could not save " + label + ": " + e.getMessage());
        }
    }

    public static void deleteWav(String wavPath) {
        if (wavPath != null) new File(wavPath).delete();
    }
}
