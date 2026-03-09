package com.moreno.electrodomesticos;

import com.moreno.electrodomesticos.config.SpringFXMLLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Punto de entrada principal: integra Spring Boot con el ciclo de vida de JavaFX.
 * Muestra un splash screen inmediato mientras Spring arranca en segundo plano.
 */
public class ElectrodomesticosApp extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void start(Stage primaryStage) {
        // Mostrar splash screen de inmediato (el usuario ve algo en <1 s)
        Stage splash = buildSplash();
        splash.show();

        // Arrancar Spring Boot en un hilo de fondo para no bloquear el UI
        Thread springThread = new Thread(() -> {
            springContext = SpringApplication.run(
                    SpringBootConfig.class,
                    getParameters().getRaw().toArray(new String[0]));

            // Cuando Spring termina, construir la ventana principal en el hilo JavaFX
            Platform.runLater(() -> {
                try {
                    SpringFXMLLoader loader = springContext.getBean(SpringFXMLLoader.class);
                    Parent root = loader.load("/fxml/main.fxml");

                    Scene scene = new Scene(root, 1200, 720);
                    scene.getStylesheets().add(
                            getClass().getResource("/css/styles.css").toExternalForm());

                    primaryStage.setTitle("Moreno Electrodomésticos — Gestión de Inventario");
                    primaryStage.setScene(scene);
                    primaryStage.setMinWidth(900);
                    primaryStage.setMinHeight(600);
                    primaryStage.show();
                    splash.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.exit();
                }
            });
        }, "spring-init");
        springThread.setDaemon(true);
        springThread.start();
    }

    /** Crea una ventana pequeña de carga sin decoración de sistema operativo. */
    private Stage buildSplash() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);

        Label label = new Label("Cargando Moreno Electrodomésticos…");
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #444;");

        VBox box = new VBox(16, spinner, label);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #ffffff; -fx-padding: 40;");

        Stage splash = new Stage(StageStyle.UNDECORATED);
        splash.setScene(new Scene(box, 320, 160));
        splash.centerOnScreen();
        return splash;
    }

    @Override
    public void stop() {
        if (springContext != null) springContext.close();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
