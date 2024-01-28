package ir.map.GUI;

import ir.map.domain.User;
import ir.map.domain.validators.FriendshipValidator;
import ir.map.domain.validators.MessageValidator;
import ir.map.domain.validators.UserValidator;
import ir.map.repository.database.*;
import ir.map.repository.paging.Page;
import ir.map.repository.paging.PageableImplementation;
import ir.map.service.FriendshipService;
import ir.map.service.MessageService;
import ir.map.service.UserService;
import ir.map.utils.Constants;
import ir.map.utils.observer.Observer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class UserController implements Observer {

    UserService service;

    User loggedUser;

    ObservableList<User> model = FXCollections.observableArrayList();

    private static int pageSize = 1;

    private static int pageNumber = 1;

    @FXML
    TableView<User> tableView;
    @FXML
    TableColumn<User, String> tableColumnUserId;
    @FXML
    TableColumn<User, String> tableColumnFirstName;
    @FXML
    TableColumn<User, String> tableColumnLastName;
    @FXML
    TableColumn<User, String> tableColumnEmail;
    @FXML
    Pagination pagination;
    @FXML
    Button toFriendshipsWindow;
    @FXML
    Button toMessagesWindow;
    @FXML
    ComboBox<Integer> pageSizeComboBox;


    public void setUserService(UserService service) {
        this.service = service;
        this.service.addObserver(this);
        setPagination();
        initModel();
    }

    public void setLoggedUser(User user) {
        loggedUser = user;
    }

    private void setPagination() {
        setNumberOfPages();
        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            pageNumber = newValue.intValue() + 1;
            initModel();
        });

        ObservableList<Integer> numberList = FXCollections.observableArrayList(IntStream.rangeClosed(1, service.findNumberOfUsers()).boxed().toList());
        pageSizeComboBox.setItems(numberList);
        pageSizeComboBox.setValue(1);
        pageSizeComboBox.setOnAction(event -> {
            pageSize = pageSizeComboBox.getValue();
            pageNumber = 1;
            initModel();
        });
    }

    private void setNumberOfPages() {
        int numberOfPages;
        int numberOfUsers = service.findNumberOfUsers();
        if (numberOfUsers % pageSize == 0) {
            numberOfPages = numberOfUsers / pageSize;
        }
        else
            numberOfPages = numberOfUsers / pageSize + 1;
        pagination.setPageCount(numberOfPages);
    }

    @FXML
    public void initialize() {
        tableColumnUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        tableColumnFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        tableColumnLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        tableColumnEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        tableView.setItems(model);
    }

    private void initModel() {
        Page<User> users = service.findAll(new PageableImplementation(pageNumber, pageSize));
        List<User> userList = new ArrayList<>();
        users.getContent().forEach(userList::add);
        model.setAll(userList);
        setNumberOfPages();
    }

    @Override
    public void update() {
        initModel();
    }

    private void initFriendshipView() throws IOException {
        FXMLLoader friendshipLoader = new FXMLLoader();
        friendshipLoader.setLocation(getClass().getResource("/ir/map/views/friendship-view.fxml"));
        StackPane friendshipLayout = friendshipLoader.load();
        Stage friendshipStage = new Stage();
        friendshipStage.setScene(new Scene(friendshipLayout));

        FriendshipController friendshipController = friendshipLoader.getController();
        friendshipController.setLoggedUser(loggedUser);
        friendshipController.setFriendshipService(
                new FriendshipService(
                        new UserDBPagingRepository(new UserValidator(), Constants.url, Constants.username, Constants.password),
                        new FriendshipDBPagingRepository(new FriendshipValidator(), Constants.url, Constants.username, Constants.password),
                        new FriendshipRequestDBPagingRepository(Constants.url, Constants.username, Constants.password)
                )
        );
        friendshipStage.setWidth(1000);
        friendshipStage.setHeight(600);
        friendshipStage.show();
    }

    public void initMessageView() throws IOException {
        FXMLLoader messageLoader = new FXMLLoader() ;
        messageLoader.setLocation(getClass().getResource("/ir/map/views/message-view.fxml"));
        StackPane messageLayout = messageLoader.load();
        Stage messageStage = new Stage();
        messageStage.setScene(new Scene(messageLayout));

        MessageController messageController = messageLoader.getController();
        messageController.setLoggedUser(loggedUser);
        messageController.setMessageService(
                new MessageService(
                        new UserDBPagingRepository(new UserValidator(), Constants.url, Constants.username, Constants.password),
                        new MessageDBRepository(new MessageValidator(), Constants.url, Constants.username, Constants.password)
                ));

        messageStage.setWidth(1000);
        messageStage.setHeight(600);
        messageStage.show();
    }

    public void handleToFriendshipsWindow() throws IOException {
        initFriendshipView();
    }

    public void handleToMessagesWindow() throws IOException {
        initMessageView();
    }
}