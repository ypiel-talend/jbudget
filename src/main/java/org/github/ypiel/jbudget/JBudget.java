package org.github.ypiel.jbudget;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class JBudget extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML file with proper error handling
            URL fxmlUrl = getClass().getResource("/org/github/ypiel/jbudget/fxml/JBudget.fxml");

            if (fxmlUrl == null) {
                System.err.println("FXML file not found in resources!");
                System.err.println("Looking for: /org/github/ypiel/jbudget/fxml/JBudget.fxml");
                System.exit(1);
            }

            System.out.println("Loading FXML from: " + fxmlUrl.toString());

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            primaryStage.setTitle("JBudget - Bank Transaction Manager");
            primaryStage.setScene(new Scene(root, 1024, 768));
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Failed to load FXML file:");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
