package kerneln64.ui;
import kerneln64.Main;


import javax.swing.*;

public final class HostMessages {
    public static void error(String title, Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage() == null ? cause.toString() : cause.getMessage();
        System.err.println(title + ": " + message);
        if (!java.awt.GraphicsEnvironment.isHeadless()) SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(Main.hMainWindow, message, title, JOptionPane.ERROR_MESSAGE));
    }
}
