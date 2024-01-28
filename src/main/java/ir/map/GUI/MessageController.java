package ir.map.GUI;

import ir.map.domain.Message;
import ir.map.domain.User;
import ir.map.domain.validators.ValidationException;
import ir.map.repository.paging.PageableImplementation;
import ir.map.service.MessageService;
import ir.map.utils.observer.Observer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class MessageController implements Observer {

    MessageService service;

    User loggedUser;

    List<User> receivers = new ArrayList<>();

    ObservableList<String> messageFriendsModel = FXCollections.observableArrayList();

    ObservableList<String> messageModel = FXCollections.observableArrayList();

    private static int pageSizeUsers = 1;

    private static int pageNumberUsers = 1;

    @FXML
    ListView<String> messageUsersListView;
    @FXML
    ListView<String> messageListView;
    @FXML
    VBox messageVBox;
    @FXML
    Label messageHeaderLabel;
    @FXML
    TextField messageTextField;
    @FXML
    Button sendMessage;
    @FXML
    RadioButton groupConversationOption;
    @FXML
    Pagination usersPagination;
    @FXML
    ComboBox<Integer> pageSizeUsersComboBox;
    @FXML
    ComboBox<String> messagesComboBox;


    public void setMessageService(MessageService service) {
        this.service = service;
        this.service.addObserver(this);
        setPagination();
        initMessageUsersModel();
    }

    public void setLoggedUser(User user) {
        loggedUser = user;
    }

    @FXML
    public void initialize() {
        messageUsersListView.setItems(messageFriendsModel);
        messageListView.setItems(messageModel);


        // show tooltip when hover
        messageListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                Optional<Message> message = service.findMessageByText(item);
                if (empty || item == null || message.isEmpty()) {
                    setTooltip(null);
                    setText(null);
                } else {
                    Message reply = message.get().getReply();
                    if (reply != null) {
                        setText(item);
                        Tooltip tooltip = new Tooltip(reply.getMessage());
                        setTooltip(tooltip);
                        setOnMouseEntered(event -> tooltip.show(getListView(), event.getScreenX() + 10, event.getScreenY() + 10));
                        setOnMouseExited(event -> tooltip.hide());
                    }
                }
            }
        });

        ObservableList<String> comboBoxValues = FXCollections.observableArrayList("Today", "This week", "This month", "All");
        messagesComboBox.setItems(comboBoxValues);
        messagesComboBox.setValue("Today");
        messagesComboBox.setOnAction(event -> initMessageModel());
    }

    private void setPagination() {
        setNumberOfPages();
        usersPagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            pageNumberUsers = newValue.intValue() + 1;
            initMessageUsersModel();
        });

        ObservableList<Integer> numberList = FXCollections.observableArrayList(IntStream.rangeClosed(1, service.findNumberOfUsers()).boxed().toList());
        pageSizeUsersComboBox.setItems(numberList);
        pageSizeUsersComboBox.setValue(1);
        pageSizeUsersComboBox.setOnAction(event -> {
            pageSizeUsers = pageSizeUsersComboBox.getValue();
            pageNumberUsers = 1;
            initMessageUsersModel();
        });
    }

    private void setNumberOfPages() {
        int numberOfPages;
        int numberOfUsers = service.findNumberOfUsers();
        if (numberOfUsers % pageSizeUsers == 0) {
            numberOfPages = numberOfUsers / pageSizeUsers;
        }
        else
            numberOfPages = numberOfUsers / pageSizeUsers + 1;
        usersPagination.setPageCount(numberOfPages);
    }

    private void initMessageUsersModel() {
        Iterable<User> users = service.findAllUsers(new PageableImplementation(pageNumberUsers, pageSizeUsers));
        List<String> usersData = new ArrayList<>();
        users.forEach(user -> usersData.add(user.getFirstName() + " " + user.getLastName()));
        messageFriendsModel.setAll(usersData);

        messageUsersListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            String[] fullName = newValue.split(" ");
            if(!groupConversationOption.isSelected()) {
                receivers = new ArrayList<>();
            }
            // TODO - check duplicate users to group conversation
            Optional<User> optionalUser = service.findUserByName(fullName[0], fullName[1]);
            optionalUser.ifPresent(user -> receivers.add(user));
            messagesComboBox.setValue("Today");
            initMessageModel();
        });
        setNumberOfPages();
    }

    private void initMessageModel() {
        // newValue is the name of selected user
        messageVBox.setVisible(true);
        AtomicReference<String> headerLabelText = new AtomicReference<>("Conversation with ");
        receivers.forEach(user -> headerLabelText.set(headerLabelText.get() + user.getFirstName() + " " + user.getLastName() + ", "));
        headerLabelText.set(headerLabelText.get().substring(0, headerLabelText.get().length() - 2)); // cut last comma
        messageHeaderLabel.setText(headerLabelText.get());
        messageTextField.setText("");

        // load messages
        List<Message> messageList = service.findAllMessagesBetweenUsers(loggedUser, receivers, messagesComboBox.getValue());
        List<String> messageListData = new ArrayList<>();
        // TODO - print id (and date) only when hover over or select the message
        messageList.forEach(message -> messageListData.add(message.getMessage()));
        messageModel.setAll(messageListData);

        // set alignment
        messageListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setTooltip(null);
                        } else {
                            setText(item);
                            Optional<Message> message = service.findMessageByText(item);
                            if(message.isPresent()) {
                                if(Objects.equals(message.get().getFrom().getId(), loggedUser.getId())) {
                                    setAlignment(Pos.CENTER_RIGHT);
                                }
                                else {
                                    setAlignment(Pos.CENTER_LEFT);
                                }
                                Message reply = message.get().getReply();
                                if(reply == null) {
                                    setTooltip(null);
                                }
                                else {
                                    Tooltip tooltip = new Tooltip(reply.getMessage());
                                    setTooltip(tooltip);
                                }
                            }
                        }
                    }
                };
            }
        });
        messageListView.setItems(messageModel);
    }

    public void handleSendMessage() {
        String messageText = messageTextField.getText();
        String selectedMessage = messageListView.getSelectionModel().getSelectedItem();
        if(selectedMessage == null) {
            try {
                service.save(loggedUser, receivers, messageText, LocalDateTime.now(), null);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Successful!");
                alert.setContentText("Message sent!");
                alert.showAndWait();
            }
            catch (ValidationException validationException) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Empty message!");
                alert.setContentText("Please send a valid message.");
                alert.showAndWait();
            }
        }
        else {
            Optional<Message> message = service.findMessageByText(selectedMessage);
            message.ifPresent(messageValue -> {
                try {
                    if(service.save(loggedUser, receivers, messageText, LocalDateTime.now(), null).isPresent()) { // reply is saved into database
                        Optional<Message> reply = service.findMessageByText(messageText);
                        reply.ifPresent(replyValue -> service.setReply(messageValue, replyValue)); // set reply field of the selected message
                    }
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Successful!");
                    alert.setContentText("Reply sent!");
                    alert.showAndWait();
                }
                catch (ValidationException validationException) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Empty message!");
                    alert.setContentText("Please send a valid message.");
                    alert.showAndWait();
                }
            });
        }
    }

    @Override
    public void update() {
        initMessageModel();
    }
}
