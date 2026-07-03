package com.voicebanking.utils;

import com.voicebanking.utils.tts.TtsFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TtsUtil {

    /**
     * Generates a WAV file for the given text and appends 3 seconds of PCM silence.
     * The trailing silence gives the Pipecat VAD enough quiet audio after speech to
     * reliably trigger STT before the button is released.
     */
    public static String generateWav(String text) throws Exception {
        String path = TtsFactory.create().generate(text);
        appendSilenceToWav(path, 3000);
        return path;
    }

    /**
     * Returns the duration of the WAV file in milliseconds by scanning its RIFF chunks.
     * A chunk scanner is used because SAPI inserts extra chunks (JUNK, LIST, fact) before
     * the data chunk, making a fixed byte offset unreliable.
     */
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

    /**
     * Rewrites the WAV as a clean minimal file (RIFF → fmt → data) with {@code silenceMs}
     * of zero-valued PCM appended. A full rewrite is used because SAPI writes extra chunks
     * after the data chunk; patching in place would overwrite them and corrupt the file.
     */
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

    /** Deletes the temporary WAV file. */
    public static void deleteWav(String wavPath) {
        if (wavPath != null) new File(wavPath).delete();
    }
}
