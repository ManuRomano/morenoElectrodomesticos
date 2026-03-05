package com.moreno.electrodomesticos.config;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

/**
 * FXMLLoader que delega la creación de controladores al contexto de Spring,
 * permitiendo inyección de dependencias en los controllers de JavaFX.
 */
@Component
public class SpringFXMLLoader {

    private final ApplicationContext context;

    public SpringFXMLLoader(ApplicationContext context) {
        this.context = context;
    }

    public Parent load(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(context::getBean);
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new IOException("FXML no encontrado: " + fxmlPath);
        }
        loader.setLocation(resource);
        return loader.load();
    }

    public <T> T loadWithController(String fxmlPath, Class<T> controllerClass) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(context::getBean);
        loader.setLocation(getClass().getResource(fxmlPath));
        loader.load();
        return loader.getController();
    }
}
