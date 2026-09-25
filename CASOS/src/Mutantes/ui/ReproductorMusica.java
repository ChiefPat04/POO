package mutantes.ui;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class ReproductorMusica {

    private Clip clip;

    public void reproducirEnBucle(String rutaRecurso) {
        try {
            URL url = getClass().getResource(rutaRecurso);
            if (url == null) {
                System.err.println("No se encontro el archivo de musica: " + rutaRecurso);
                return;
            }

            AudioInputStream audio = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(audio);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("No se pudo reproducir la musica: " + e.getMessage());
        }
    }

    public void detener() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }
} 