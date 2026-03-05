package com.moreno.electrodomesticos;

/**
 * Clase de entrada real para evitar problemas de classpath con JavaFX.
 * El manifest del JAR apunta aquí, no a la clase Application de JavaFX.
 */
public class MainLauncher {
    public static void main(String[] args) {
        ElectrodomesticosApp.main(args);
    }
}
