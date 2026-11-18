package powerband.storage;

import static powerband.util.IConstants.ACTION_CODE_NOTE_OFF;
import static powerband.util.IConstants.ACTION_CODE_NOTE_ON;
import static powerband.util.IConstants.ACTION_CODE_TRIGGER;
import static powerband.util.IConstants.COMMAND_PADDING;
import static powerband.util.IConstants.ERR_INVALID_NAME;
import static powerband.util.IConstants.ERR_STORAGE_READ;
import static powerband.util.IConstants.ERR_STORAGE_WRITE;
import static powerband.util.IConstants.ERR_UNEXPECTED_RECORD_TYPE;
import static powerband.util.IConstants.ERR_UNKNOWN_ACTION_CODE;
import static powerband.util.IConstants.ERR_UNKNOWN_SYNTH_CODE;
import static powerband.util.IConstants.FILE_MODE_READ;
import static powerband.util.IConstants.FILE_MODE_READ_WRITE;
import static powerband.util.IConstants.FILE_START;
import static powerband.util.IConstants.FIRST_PARTIAL;
import static powerband.util.IConstants.HEADER_PADDING;
import static powerband.util.IConstants.LIST_FILE;
import static powerband.util.IConstants.LIST_RECORD_SIZE;
import static powerband.util.IConstants.MUSIC_FILE;
import static powerband.util.IConstants.NAME_LENGTH;
import static powerband.util.IConstants.NAME_MAX_CHAR;
import static powerband.util.IConstants.NAME_MIN_CHAR;
import static powerband.util.IConstants.NAME_PAD_CHAR;
import static powerband.util.IConstants.ORGAN_PARTIALS;
import static powerband.util.IConstants.PADDING_BYTE;
import static powerband.util.IConstants.RECORD_SIZE;
import static powerband.util.IConstants.RECORD_TYPE_COMMAND;
import static powerband.util.IConstants.RECORD_TYPE_HEADER;
import static powerband.util.IConstants.SYNTH_CODE_BASS;
import static powerband.util.IConstants.SYNTH_CODE_ORGAN;
import static powerband.util.IConstants.SYNTH_CODE_PIANO;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import powerband.exception.StorageException;
import powerband.model.ListEntry;
import powerband.model.RecordingHeader;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.model.SynthType;

/**
 * Única clase que lee y escribe los registros binarios de recordings.music
 * (encabezados y comandos, todos de {@code RECORD_SIZE} bytes) y de
 * recordings.list ({@code LIST_RECORD_SIZE} bytes).
 *
 * <p>Cada operación abre el archivo, trabaja y lo cierra, así cada comando queda
 * en disco de inmediato. Los métodos son {@code synchronized} porque el grabador
 * recibe eventos de varios hilos.</p>
 */
public class RecordingStorage {

    private final File musicFile;
    private final File listFile;

    public RecordingStorage() {
        this(new File(MUSIC_FILE), new File(LIST_FILE));
    }

    public RecordingStorage(File pMusicFile, File pListFile) {
        musicFile = pMusicFile;
        listFile = pListFile;
    }

    // ------------------------------------------------------------ recordings.music

    public synchronized long musicLength() throws StorageException {
        return musicFile.length();
    }

    /** Agrega un encabezado al final del archivo y devuelve la posición donde quedó. */
    public synchronized long appendHeader(RecordingHeader pHeader) throws StorageException {
        long position = musicFile.length();
        writeAt(musicFile, position, encodeHeader(pHeader));
        return position;
    }

    /** Reescribe completo el encabezado que está en esa posición. */
    public synchronized void writeHeader(long pPosition, RecordingHeader pHeader)
            throws StorageException {
        writeAt(musicFile, pPosition, encodeHeader(pHeader));
    }

    public synchronized RecordingHeader readHeader(long pPosition) throws StorageException {
        try (DataInputStream in = openRecord(pPosition, RECORD_TYPE_HEADER)) {
            byte[] name = new byte[NAME_LENGTH];
            in.readFully(name);
            return new RecordingHeader(decodeName(name), in.readLong(), in.readLong());
        } catch (IOException e) {
            throw new StorageException(ERR_STORAGE_READ + musicFile.getPath(), e);
        }
    }

    /** Agrega un comando al final del archivo. El {@code timeMs} del evento ya es relativo. */
    public synchronized void appendEvent(SynthEvent pEvent) throws StorageException {
        writeAt(musicFile, musicFile.length(), encodeEvent(pEvent));
    }

    public synchronized SynthEvent readEvent(long pPosition) throws StorageException {
        try (DataInputStream in = openRecord(pPosition, RECORD_TYPE_COMMAND)) {
            long timeMs = in.readLong();
            int synthCode = in.readByte();
            int actionCode = in.readByte();
            int note = in.readInt();
            int durationMs = in.readInt();
            int third = in.readInt();
            float amp = in.readFloat();
            float[] ratios = readPartials(in);
            float[] amps = readPartials(in);
            return buildEvent(synthCode, actionCode, note, durationMs, third, amp, ratios, amps)
                    .withTimeMs(timeMs);
        } catch (IOException e) {
            throw new StorageException(ERR_STORAGE_READ + musicFile.getPath(), e);
        }
    }

    /** Recorta el archivo; se usa para descartar una grabación sin nombre. */
    public synchronized void truncateMusic(long pLength) throws StorageException {
        try (RandomAccessFile file = new RandomAccessFile(musicFile, FILE_MODE_READ_WRITE)) {
            file.setLength(pLength);
        } catch (IOException e) {
            throw new StorageException(ERR_STORAGE_WRITE + musicFile.getPath(), e);
        }
    }

    // ------------------------------------------------------------ recordings.list

    public synchronized void appendListEntry(ListEntry pEntry) throws StorageException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(LIST_RECORD_SIZE);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(encodeName(pEntry.getName()));
            out.writeLong(pEntry.getCreatedAtMs());
        } catch (IOException e) {
            throw new StorageException(ERR_STORAGE_WRITE + listFile.getPath(), e);
        }
        writeAt(listFile, listFile.length(), bytes.toByteArray());
    }

    /**
     * Busca el nombre leyendo recordings.list en disco, un registro a la vez.
     * Distingue mayúsculas de minúsculas.
     */
    public synchronized boolean listContainsName(String pName) throws StorageException {
        if (!listFile.exists()) {
            return false;
        }
        long count = listCount();
        try (DataInputStream in = openList()) {
            byte[] name = new byte[NAME_LENGTH];
            for (long i = FILE_START; i < count; i++) {
                in.readFully(name);
                in.readLong();
                if (decodeName(name).equals(pName)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new StorageException(ERR_STORAGE_READ + listFile.getPath(), e);
        }
    }

    public synchronized long listCount() throws StorageException {
        return listFile.length() / LIST_RECORD_SIZE;
    }

    /** Lee la entrada número {@code pIndex} (desde 0) en orden cronológico. */
    public synchronized ListEntry readListEntry(long pIndex) throws StorageException {
        try (DataInputStream in = openList()) {
            in.skipNBytes(pIndex * LIST_RECORD_SIZE);
            byte[] name = new byte[NAME_LENGTH];
            in.readFully(name);
            return new ListEntry(decodeName(name), in.readLong());
        } catch (IOException e) {
            throw new StorageException(ERR_STORAGE_READ + listFile.getPath(), e);
        }
    }

    // ------------------------------------------------------------ nombres

    /**
     * Un nombre es válido si mide de 1 a {@code NAME_LENGTH}, solo tiene ASCII
     * imprimible y no empieza ni termina con espacio (se confundiría con el relleno).
     */
    public static boolean isValidName(String pName) {
        if (pName == null || pName.isEmpty() || pName.length() > NAME_LENGTH) {
            return false;
        }
        for (char c : pName.toCharArray()) {
            if (c < NAME_MIN_CHAR || c > NAME_MAX_CHAR) {
                return false;
            }
        }
        return pName.strip().length() == pName.length();
    }

    /** Nombre en ASCII, rellenado con espacios hasta {@code NAME_LENGTH} bytes. */
    public static byte[] encodeName(String pName) {
        if (pName == null || pName.length() > NAME_LENGTH) {
            throw new IllegalArgumentException(ERR_INVALID_NAME + pName);
        }
        String padding = String.valueOf(NAME_PAD_CHAR).repeat(NAME_LENGTH - pName.length());
        return (pName + padding).getBytes(StandardCharsets.US_ASCII);
    }

    /** Nombre sin el relleno del final. */
    public static String decodeName(byte[] pBytes) {
        return new String(pBytes, StandardCharsets.US_ASCII).stripTrailing();
    }

    // ------------------------------------------------------------ auxiliares

    private static byte[] encodeHeader(RecordingHeader pHeader) throws StorageException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(RECORD_SIZE);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(RECORD_TYPE_HEADER);
            out.write(encodeName(pHeader.getName()));
            out.writeLong(pHeader.getStartPosition());
            out.writeLong(pHeader.getEndPosition());
            out.write(padding(HEADER_PADDING));
        } catch (IOException e) {
            throw new StorageException(ERR_STORAGE_WRITE + RECORD_TYPE_HEADER, e);
        }
        return bytes.toByteArray();
    }

    private static byte[] encodeEvent(SynthEvent pEvent) throws StorageException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(RECORD_SIZE);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(RECORD_TYPE_COMMAND);
            out.writeLong(pEvent.getTimeMs());
            out.writeByte(synthCode(pEvent.getSynth()));
            out.writeByte(actionCode(pEvent.getAction()));
            out.writeInt(pEvent.getNote());
            out.writeInt(pEvent.getDurationMs());
            out.writeInt(pEvent.getThird());
            out.writeFloat(pEvent.getAmp());
            out.writeFloat(pEvent.getR1());
            out.writeFloat(pEvent.getR2());
            out.writeFloat(pEvent.getR3());
            out.writeFloat(pEvent.getA1());
            out.writeFloat(pEvent.getA2());
            out.writeFloat(pEvent.getA3());
            out.write(padding(COMMAND_PADDING));
        } catch (IOException e) {
            throw new StorageException(ERR_STORAGE_WRITE + RECORD_TYPE_COMMAND, e);
        }
        return bytes.toByteArray();
    }

    private static byte[] padding(int pCount) {
        byte[] padding = new byte[pCount];
        Arrays.fill(padding, (byte) PADDING_BYTE);
        return padding;
    }

    private static int synthCode(SynthType pSynth) {
        switch (pSynth) {
            case PIANO:
                return SYNTH_CODE_PIANO;
            case BASS:
                return SYNTH_CODE_BASS;
            default:
                return SYNTH_CODE_ORGAN;
        }
    }

    private static int actionCode(SynthAction pAction) {
        switch (pAction) {
            case TRIGGER:
                return ACTION_CODE_TRIGGER;
            case NOTE_ON:
                return ACTION_CODE_NOTE_ON;
            default:
                return ACTION_CODE_NOTE_OFF;
        }
    }

    private static SynthEvent buildEvent(int pSynthCode, int pActionCode, int pNote,
            int pDurationMs, int pThird, float pAmp, float[] pRatios, float[] pAmps)
            throws StorageException {
        switch (pSynthCode) {
            case SYNTH_CODE_PIANO:
                return SynthEvent.piano(pNote, pDurationMs, pThird, pAmp);
            case SYNTH_CODE_BASS:
                return SynthEvent.bass(pNote, pDurationMs, pAmp);
            case SYNTH_CODE_ORGAN:
                if (pActionCode == ACTION_CODE_NOTE_ON) {
                    return SynthEvent.organOn(pNote, pAmp, pRatios, pAmps);
                }
                if (pActionCode == ACTION_CODE_NOTE_OFF) {
                    return SynthEvent.organOff(pNote);
                }
                throw new StorageException(ERR_UNKNOWN_ACTION_CODE + pActionCode);
            default:
                throw new StorageException(ERR_UNKNOWN_SYNTH_CODE + pSynthCode);
        }
    }

    private static float[] readPartials(DataInputStream pIn) throws IOException {
        float[] partials = new float[ORGAN_PARTIALS];
        for (int i = FIRST_PARTIAL; i < ORGAN_PARTIALS; i++) {
            partials[i] = pIn.readFloat();
        }
        return partials;
    }

    /** Escribe el registro completo de una vez en esa posición; crea la carpeta si falta. */
    private static void writeAt(File pFile, long pPosition, byte[] pRecord)
            throws StorageException {
        File parent = pFile.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs(); // si no se pudo crear, falla el open de abajo
        }
        try (RandomAccessFile file = new RandomAccessFile(pFile, FILE_MODE_READ_WRITE)) {
            file.seek(pPosition);
            file.write(pRecord);
        } catch (IOException e) {
            throw new StorageException(ERR_STORAGE_WRITE + pFile.getPath(), e);
        }
    }

    /** Lee un registro de recordings.music y verifica su primer byte. */
    private DataInputStream openRecord(long pPosition, int pExpectedType)
            throws IOException, StorageException {
        byte[] record = new byte[RECORD_SIZE];
        try (RandomAccessFile file = new RandomAccessFile(musicFile, FILE_MODE_READ)) {
            file.seek(pPosition);
            file.readFully(record);
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(record));
        int type = in.readByte();
        if (type != pExpectedType) {
            throw new StorageException(ERR_UNEXPECTED_RECORD_TYPE + type);
        }
        return in;
    }

    private DataInputStream openList() throws IOException {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(listFile)));
    }
}
