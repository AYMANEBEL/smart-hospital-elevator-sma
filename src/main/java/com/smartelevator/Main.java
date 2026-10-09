package com.smartelevator;

import com.smartelevator.core.AppState;
import com.smartelevator.core.JadePlatform;
import com.smartelevator.gui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            JadePlatform.start();
        } catch (Exception e) {
            AppState.log("FATAL", "Impossible de démarrer JADE : " + e.getMessage());
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}
