package mutantes;

import javax.swing.SwingUtilities;
import mutantes.ui.VentanaBatalla;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(VentanaBatalla::new);
    }
}