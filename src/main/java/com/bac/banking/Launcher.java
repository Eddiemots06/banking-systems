package com.bac.banking;

/**
 * Plain main class (not extending Application) so the app can also be started from an IDE
 * "Run" button or a classpath launch. Normally use: mvn javafx:run
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
