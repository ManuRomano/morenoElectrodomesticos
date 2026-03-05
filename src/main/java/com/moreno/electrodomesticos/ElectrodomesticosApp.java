package com.moreno.electrodomesticos;

import com.moreno.electrodomesticos.config.SpringFXMLLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Punto de entrada principal: integra Spring Boot con el ciclo de vida de JavaFX.
 */
public class ElectrodomesticosApp extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Arrancar Spring Boot antes de que JavaFX muestre ventana alguna
        springContext = SpringApplication.run(SpringBootConfig.class, getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SpringFXMLLoader loader = springContext.getBean(SpringFXMLLoader.class);
        Parent root = loader.load("/fxml/main.fxml");

        Scene scene = new Scene(root, 1200, 720);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        primaryStage.setTitle("Moreno Electrodomésticos — Gestión de Inventario");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
