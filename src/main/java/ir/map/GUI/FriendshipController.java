package ir.map.GUI;

import ir.map.domain.User;
import ir.map.repository.paging.Page;
import ir.map.repository.paging.PageableImplementation;
import ir.map.service.FriendshipService;
import ir.map.utils.observer.Observer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class FriendshipController implements Observer {
    FriendshipService service;

    User loggedUser;

    ObservableList<String> friendsModel = FXCollections.observableArrayList();

    ObservableList<String> usersNotFriendWithUserModel = FXCollections.observableArrayList();

    ObservableList<String> pendingRequestsModel = FXCollections.observableArrayList();

    @FXML
    ListView<String> friendsListView;
    @FXML
    ListView<String> usersNotFriendWithUserListView;
    @FXML
    ListView<String> pendingRequestsListView;
    @FXML
    Button breakFriendship;
    @FXML
    Button sendRequest;
    @FXML
    Button approveRequest;
    @FXML
    Button rejectRequest;
    @FXML
    Pagination friendsPagination;
    @FXML
    ComboBox<Integer> pageSizeFriendsComboBox;
    private static int pageSizeFriends = 1;
    private static int pageNumberFriends = 1;
    @FXML
    Pagination othersPagination;
    @FXML
    ComboBox<Integer> pageSizeOthersComboBox;
    private static int pageSizeOthers = 1;
    private static int pageNumberOthers = 1;
    @FXML
    Pagination requestsPagination;
    @FXML
    ComboBox<Integer> pageSizeRequestsComboBox;
    private static int pageSizeRequests = 1;
    private static int pageNumberRequests = 1;


    public void setFriendshipService(FriendshipService service) {
        this.service = service;
        this.service.addObserver(this);
        setPagination();
        initFriendsModel();
        initUsersNotFriendWithUserModel();
        initPendingRequests();
    }

    public void setLoggedUser(User user) {
        loggedUser = user;
    }


    private void setPagination() {
        setPaginationForFriends();
        setPaginationForOthers();
        setPaginationForRequests();
    }

    private void setPaginationForFriends() {
        setNumberOfPagesForFriends();
        friendsPagination.currentPageIndexProperty().addListener(((observable, oldValue, newValue) -> {
            pageNumberFriends = newValue.intValue() + 1;
            initFriendsModel();
        }));
        ObservableList<Integer> numberOfFriendsList = FXCollections.observableArrayList(IntStream.rangeClosed(1, service.findAllFriendsForUser(loggedUser.getId()).size()).boxed().toList());
        pageSizeFriendsComboBox.setItems(numberOfFriendsList);
        pageSizeFriendsComboBox.setValue(1);
        pageSizeFriendsComboBox.setOnAction(event -> {
            pageSizeFriends = pageSizeFriendsComboBox.getValue();
            pageNumberFriends = 1;
            initFriendsModel();
        });
    }

    private void setPaginationForOthers() {
        setNumberOfPageForOthers();
        othersPagination.currentPageIndexProperty().addListener(((observable, oldValue, newValue) -> {
            pageNumberOthers = newValue.intValue() + 1;
            initUsersNotFriendWithUserModel();
        }));
        ObservableList<Integer> numberOfOthersList = FXCollections.observableArrayList(IntStream.rangeClosed(1, service.findAllUsersNotFriendWithUser(loggedUser.getId()).size()).boxed().toList());
        pageSizeOthersComboBox.setItems(numberOfOthersList);
        pageSizeOthersComboBox.setValue(1);
        pageSizeOthersComboBox.setOnAction(event -> {
            pageSizeOthers = pageSizeOthersComboBox.getValue();
            pageNumberOthers = 1;
            initUsersNotFriendWithUserModel();
        });
    }

    private void setPaginationForRequests() {
        setNumberOfPageForRequests();
        requestsPagination.currentPageIndexProperty().addListener(((observable, oldValue, newValue) -> {
            pageNumberRequests = newValue.intValue() + 1;
            initPendingRequests();
        }));
        ObservableList<Integer> numberOfRequestsList = FXCollections.observableArrayList(IntStream.rangeClosed(1, service.findAllUsersFromPendingRequestsForUser(loggedUser.getId()).size()).boxed().toList());
        pageSizeRequestsComboBox.setItems(numberOfRequestsList);
        pageSizeRequestsComboBox.setValue(1);
        pageSizeRequestsComboBox.setOnAction(event -> {
            pageSizeRequests = pageSizeRequestsComboBox.getValue();
            pageNumberRequests = 1;
            initPendingRequests();
        });
    }

    private void setNumberOfPagesForFriends() {
        int numberOfPagesFriends;
        int numberOfFriends = service.findAllFriendsForUser(loggedUser.getId()).size();
        if (numberOfFriends % pageSizeFriends == 0) {
            numberOfPagesFriends = numberOfFriends / pageSizeFriends;
        } else
            numberOfPagesFriends = numberOfFriends / pageSizeFriends + 1;
        friendsPagination.setPageCount(numberOfPagesFriends);
    }

    private void setNumberOfPageForOthers() {
        int numberOfPagesOthers;
        int numberOfOthers = service.findAllUsersNotFriendWithUser(loggedUser.getId()).size();
        if (numberOfOthers % pageSizeOthers == 0) {
            numberOfPagesOthers = numberOfOthers / pageSizeOthers;
        } else
            numberOfPagesOthers = numberOfOthers / pageSizeOthers + 1;
        othersPagination.setPageCount(numberOfPagesOthers);
    }
    private void setNumberOfPageForRequests() {
        int numberOfPagesRequests;
        int numberOfRequests = service.findAllUsersFromPendingRequestsForUser(loggedUser.getId()).size();
        if (numberOfRequests % pageSizeRequests == 0) {
            numberOfPagesRequests = numberOfRequests / pageSizeRequests;
        }
        else
            numberOfPagesRequests = numberOfRequests / pageSizeRequests + 1;
        requestsPagination.setPageCount(numberOfPagesRequests);
    }

    @FXML
    public void initialize() {
        friendsListView.setItems(friendsModel);
        usersNotFriendWithUserListView.setItems(usersNotFriendWithUserModel);
        pendingRequestsListView.setItems(pendingRequestsModel);
    }

    private void initFriendsModel() {
        List<User> userFriends = service.findAllFriendsForUser(
                loggedUser.getId(), new PageableImplementation(pageNumberFriends, pageSizeFriends)
        );
        List<String> userFriendsData = new ArrayList<>();
        userFriends.forEach(user -> {
            userFriendsData.add(user.getFirstName() + " " + user.getLastName());
        });
        friendsModel.setAll(userFriendsData);
        setNumberOfPagesForFriends();
    }

    private void initUsersNotFriendWithUserModel() {
        List<User> usersNotFriendWithUser = service.findAllUsersNotFriendWithUser(
                loggedUser.getId(), new PageableImplementation(pageNumberOthers, pageSizeOthers)
        );
        List<String> usersNotFriendWithUserData = new ArrayList<>();
        usersNotFriendWithUser.forEach(user -> usersNotFriendWithUserData.add(user.getFirstName() + " " + user.getLastName()));
        usersNotFriendWithUserModel.setAll(usersNotFriendWithUserData);
        setNumberOfPageForOthers();
    }

    private void initPendingRequests() {
        List<User> usersFromPendingRequestsForUser = service.findAllUsersFromPendingRequestsForUser(
                loggedUser.getId(), new PageableImplementation(pageNumberRequests, pageSizeRequests)
                );
        List<String> usersFromPendingRequestsForUserData = new ArrayList<>();
        usersFromPendingRequestsForUser.forEach(user -> usersFromPendingRequestsForUserData.add(user.getFirstName() + " " + user.getLastName()));
        pendingRequestsModel.setAll(usersFromPendingRequestsForUserData);
        setNumberOfPageForRequests();
    }

    public void handleBreakFriendship() {
        if(!friendsListView.getSelectionModel().isEmpty()) {
            String userName = friendsListView.getSelectionModel().getSelectedItem();
            String[] name = userName.split(" ");
            Optional<User> foundUser = service.findUserByName(name[0], name[1]);
            if(foundUser.isPresent()) {
                service.breakFriendship(loggedUser.getId(), foundUser.get().getId());
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Successful!");
                alert.setContentText("Friendship broken!");
                alert.showAndWait();
            }
        }
        else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("User selection!");
            alert.setContentText("Please select an user from list!");
            alert.showAndWait();
        }
    }

    public void handleSendRequest() {
        if(!usersNotFriendWithUserListView.getSelectionModel().isEmpty()) {
            String userName = usersNotFriendWithUserListView.getSelectionModel().getSelectedItem();
            String[] name = userName.split(" ");
            Optional<User> foundUser = service.findUserByName(name[0], name[1]);
            if(foundUser.isPresent()) {
                service.createFriendshipRequest(loggedUser.getId(), foundUser.get().getId(), loggedUser.getId());
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Successful!");
                alert.setContentText("Friend request sent!");
                alert.showAndWait();
            }
        }
        else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("User selection!");
            alert.setContentText("Please select an user from list!");
            alert.showAndWait();
        }
    }

    public void handleApproveRequest() {
        if(!pendingRequestsListView.getSelectionModel().isEmpty()) {
            String userName = pendingRequestsListView.getSelectionModel().getSelectedItem();
            String[] name = userName.split(" ");
            Optional<User> foundUser = service.findUserByName(name[0], name[1]);
            if(foundUser.isPresent()) {
                service.approveFriendshipRequest(loggedUser.getId(), foundUser.get().getId());
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Successful!");
                alert.setContentText("Request approved!");
                alert.showAndWait();
            }
        }
        else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("User selection!");
            alert.setContentText("Please select an user from list!");
            alert.showAndWait();
        }
    }

    public void handleRejectRequest() {
        if(!pendingRequestsListView.getSelectionModel().isEmpty()) {
            String userName = pendingRequestsListView.getSelectionModel().getSelectedItem();
            String[] name = userName.split(" ");
            Optional<User> foundUser = service.findUserByName(name[0], name[1]);
            if(foundUser.isPresent()) {
                service.rejectFriendshipRequest(loggedUser.getId(), foundUser.get().getId());
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Successful!");
                alert.setContentText("Request rejected!");
                alert.showAndWait();
            }
        }
        else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("User selection!");
            alert.setContentText("Please select an user from list!");
            alert.showAndWait();
        }
    }

    @Override
    public void update() {
        initFriendsModel();
        initUsersNotFriendWithUserModel();
        initPendingRequests();
    }
}
