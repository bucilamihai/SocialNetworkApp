package ir.map.GUI;

import ir.map.domain.User;
import ir.map.domain.validators.ValidationException;
import ir.map.service.UserService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class LoginController{

    UserService service;

    User loggedUser = null;

    int loginTries = 0;

    @FXML
    Button loginIntoAccountButton;
    @FXML
    Button createAccountButton;
    @FXML
    HBox firstNameHBox;
    @FXML
    HBox lastNameHBox;
    @FXML
    HBox emailHBox;
    @FXML
    HBox passwordHBox;
    @FXML
    TextField firstNameTextField;
    @FXML
    TextField lastNameTextField;
    @FXML
    TextField emailTextField;
    @FXML
    PasswordField passwordTextField;
    @FXML
    Button loginButton;
    @FXML
    Button signUpButton;


    public void setUserService(UserService service) {
        this.service = service;
    }

    private void initUserView() throws IOException {
        FXMLLoader userLoader = new FXMLLoader();
        userLoader.setLocation(getClass().getResource("/ir/map/views/user-view.fxml"));
        StackPane userLayout = userLoader.load();
        Stage userStage = new Stage();
        userStage.setScene(new Scene(userLayout));

        UserController userController = userLoader.getController();
        userController.setLoggedUser(loggedUser);
        userController.setUserService(service);

        userStage.setWidth(1000);
        userStage.setHeight(600);
        userStage.show();
    }

    public void handleLoginBox() {
        loginIntoAccountButton.setVisible(false);
        createAccountButton.setVisible(false);

        emailHBox.setVisible(true);
        passwordHBox.setVisible(true);
        loginButton.setVisible(true);

        emailTextField.setText("");
        passwordTextField.setText("");
        loginTries = 0;
    }


    public void handleCreateAccountBox() {
        loginIntoAccountButton.setVisible(false);
        createAccountButton.setVisible(false);

        firstNameHBox.setVisible(true);
        lastNameHBox.setVisible(true);
        emailHBox.setVisible(true);
        passwordHBox.setVisible(true);
        signUpButton.setVisible(true);

        firstNameTextField.setText("");
        lastNameTextField.setText("");
        emailTextField.setText("");
        passwordTextField.setText("");
    }

    public void handleLogin() throws IOException {
        String email = emailTextField.getText();
        String password = passwordTextField.getText();
        Optional<User> optionalLoggedUser = service.findUserByEmailAndPassword(email, password);
        loggedUser = optionalLoggedUser.orElse(null);
        if(loggedUser != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Successful login!");
            alert.setContentText("Logged as " + loggedUser.getFirstName() + " " + loggedUser.getLastName());
            alert.showAndWait();
            initUserView();
        }
        else {
            loginTries++;
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Failed login!");
            alert.setContentText("You failed to login, " + (3 - loginTries) + " more tries!");
            alert.showAndWait();
            if(loginTries == 3) {
                loginIntoAccountButton.setVisible(true);
                createAccountButton.setVisible(true);

                emailHBox.setVisible(false);
                passwordHBox.setVisible(false);
                loginButton.setVisible(false);
            }
        }
    }

    public void handleSignUp() {
        String firstName = firstNameTextField.getText();
        String lastName = lastNameTextField.getText();
        String email = emailTextField.getText();
        String password = passwordTextField.getText();
        try {
            service.save(firstName, lastName, email, password);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Successful sign-up!");
            alert.setContentText("Account created!");
            alert.showAndWait();
            loginIntoAccountButton.setVisible(true);
            createAccountButton.setVisible(true);

            firstNameHBox.setVisible(false);
            lastNameHBox.setVisible(false);
            emailHBox.setVisible(false);
            passwordHBox.setVisible(false);
            signUpButton.setVisible(false);
        }
        catch(ValidationException validationException) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation exception");
            alert.setContentText(validationException.getMessage());
            alert.showAndWait();
        }
        catch(Exception exception) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Exception");
            if(exception.toString().contains("unq_users_email"))
                alert.setContentText("There is a created account that uses this email address");
            else
                alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }
}
