package com.voicify.util;

import javafx.scene.Scene;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    public enum Theme {
        LIGHT("light-theme.css"),
        DARK("dark-theme.css");

        private final String cssFile;

        Theme(String cssFile) {
            this.cssFile = cssFile;
        }

        public String getCssFile() {
            return cssFile;
        }
    }

    private static ThemeManager instance;
    private Theme currentTheme = Theme.LIGHT;
    private final List<Scene> managedScenes = new ArrayList<>();
    private static final String CONFIG_FILE = "theme-preference.txt";

    private ThemeManager() {
        loadThemePreference();
        System.out.println("ThemeManager initialized with theme: " + currentTheme);
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void registerScene(Scene scene) {
        if (scene != null && !managedScenes.contains(scene)) {
            managedScenes.add(scene);
            applyThemeToScene(scene, currentTheme);
            System.out.println("Scene registered with ThemeManager");
        }
    }

    public void unregisterScene(Scene scene) {
        managedScenes.remove(scene);
        System.out.println("Scene unregistered from ThemeManager");
    }

    public void setTheme(Theme theme) {
        if (theme == null || theme == currentTheme) {
            return;
        }

        Theme previousTheme = currentTheme;
        currentTheme = theme;

        for (Scene scene : managedScenes) {
            applyThemeToScene(scene, theme);
        }

        saveThemePreference();
        System.out.println("Theme switched from " + previousTheme + " to " + currentTheme);
    }

    public void toggleTheme() {
        Theme newTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        setTheme(newTheme);
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public boolean isDarkTheme() {
        return currentTheme == Theme.DARK;
    }

    private void applyThemeToScene(Scene scene, Theme theme) {
        try {
            scene.getStylesheets().clear();

            String commonCss = "/com/voicify/css/common.css";
            var commonUrl = getClass().getResource(commonCss);
            if (commonUrl != null) {
                scene.getStylesheets().add(commonUrl.toExternalForm());
                System.out.println("Applied common.css to scene");
            } else {
                System.out.println("Common CSS file not found: " + commonCss);
            }

            String themeCss = "/com/voicify/css/" + theme.getCssFile();
            var themeUrl = getClass().getResource(themeCss);
            if (themeUrl != null) {
                scene.getStylesheets().add(themeUrl.toExternalForm());
                System.out.println("Applied theme " + theme + " to scene");
            } else {
                System.out.println("Theme CSS file not found: " + themeCss);
                applyBasicTheme(scene, theme);
            }

        } catch (Exception e) {
            System.out.println("Failed to apply theme: " + e.getMessage());
            applyBasicTheme(scene, theme);
        }
    }

    private void applyBasicTheme(Scene scene, Theme theme) {
        scene.getStylesheets().clear();
        String cssFile = (theme == Theme.DARK) ? "dark-theme.css" : "light-theme.css";
        scene.getStylesheets().add(getClass().getResource(cssFile).toExternalForm());
        System.out.println("Applied " + theme + " theme via CSS file.");
    }


    public void saveThemePreference() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write(currentTheme.name());
            System.out.println("Theme preference saved: " + currentTheme);
        } catch (IOException e) {
            System.out.println("Failed to save theme preference: " + e.getMessage());
        }
    }

    public void loadThemePreference() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String themeName = reader.readLine();
            if (themeName != null) {
                currentTheme = Theme.valueOf(themeName);
                System.out.println("Theme preference loaded: " + currentTheme);
            }
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Failed to load theme preference, using default: " + e.getMessage());
            currentTheme = Theme.LIGHT;
        }
    }

    public String getThemeStyleClass() {
        return currentTheme.name().toLowerCase() + "-theme";
    }

    public void applyThemeToNode(javafx.scene.Node node, String... additionalClasses) {
        if (node != null) {
            node.getStyleClass().removeIf(styleClass ->
                    styleClass.contains("light-theme") || styleClass.contains("dark-theme"));

            node.getStyleClass().add(getThemeStyleClass());

            for (String additionalClass : additionalClasses) {
                if (additionalClass != null && !additionalClass.trim().isEmpty()) {
                    node.getStyleClass().add(additionalClass);
                }
            }
        }
    }
}
