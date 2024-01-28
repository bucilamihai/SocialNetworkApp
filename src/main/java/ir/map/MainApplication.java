package ir.map;

import ir.map.GUI.LoginController;
import ir.map.domain.User;
import ir.map.domain.validators.UserValidator;
import ir.map.repository.database.UserDBPagingRepository;
import ir.map.repository.paging.PagingRepository;
import ir.map.service.UserService;

import ir.map.utils.Constants;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    private UserService userService;


    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        initializeService();
        initView(stage);
        stage.setWidth(1000);
        stage.setHeight(600);
        stage.setTitle("Social Network");
        stage.show();
    }

    private void initializeService() {
        PagingRepository<Long, User> userDBPagingRepository = new UserDBPagingRepository(
                new UserValidator(), Constants.url, Constants.username, Constants.password);
        userService = new UserService(userDBPagingRepository);
    }

    private void initView(Stage primaryStage) throws IOException {
        FXMLLoader loginLoader = new FXMLLoader();
        loginLoader.setLocation(getClass().getResource("views/login-view.fxml"));
        StackPane loginLayout = loginLoader.load();
        primaryStage.setScene(new Scene(loginLayout));
        // TODO - multiple scenes and commute to them when needed?

        LoginController loginController = loginLoader.getController();
        loginController.setUserService(userService);
    }
}