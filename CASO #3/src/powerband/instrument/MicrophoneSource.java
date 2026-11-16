package powerband.instrument;

import static powerband.util.IConstants.AUDIO_BIG_ENDIAN;
import static powerband.util.IConstants.AUDIO_BYTES_PER_SAMPLE;
import static powerband.util.IConstants.AUDIO_CHANNELS;
import static powerband.util.IConstants.AUDIO_SAMPLE_BITS;
import static powerband.util.IConstants.AUDIO_SAMPLE_RATE;
import static powerband.util.IConstants.AUDIO_SAMPLE_SCALE;
import static powerband.util.IConstants.AUDIO_SIGNED;
import static powerband.util.IConstants.ERR_NO_MICROPHONE;
import static powerband.util.IConstants.FIRST_INDEX;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import powerband.exception.DeviceException;

/**
 * Micrófono por defecto del sistema: 44100 Hz, mono, 16 bits con signo, little-endian.
 *
 * <p>En Windows, si el permiso de micrófono está apagado la línea abre igual
 * pero solo entrega silencio.</p>
 */
public class MicrophoneSource implements IAudioSource {

    private volatile TargetDataLine line;

    @Override
    public void open() throws DeviceException {
        AudioFormat format = new AudioFormat(AUDIO_SAMPLE_RATE, AUDIO_SAMPLE_BITS,
                AUDIO_CHANNELS, AUDIO_SIGNED, AUDIO_BIG_ENDIAN);
        try {
            TargetDataLine opened = AudioSystem.getTargetDataLine(format);
            opened.open(format);
            opened.start();
            line = opened;
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            throw new DeviceException(ERR_NO_MICROPHONE, e);
        }
    }

    @Override
    public boolean read(float[] pBlock) {
        TargetDataLine current = line;
        if (current == null) {
            return false;
        }
        byte[] bytes = new byte[pBlock.length * AUDIO_BYTES_PER_SAMPLE];
        if (current.read(bytes, FIRST_INDEX, bytes.length) < bytes.length) {
            return false; // la línea se cerró o dejó de entregar audio
        }
        ShortBuffer samples = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        for (int i = FIRST_INDEX; i < pBlock.length; i++) {
            pBlock[i] = samples.get(i) / AUDIO_SAMPLE_SCALE;
        }
        return true;
    }

    @Override
    public void close() {
        TargetDataLine current = line;
        if (current != null) {
            line = null;
            current.stop();
            current.close();
        }
    }
}
