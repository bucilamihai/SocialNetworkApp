package ir.map.repository.database;

import ir.map.domain.Message;
import ir.map.domain.User;
import ir.map.domain.validators.MessageValidator;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

public class MessageDBRepository {

    private MessageValidator messageValidator;
    private String url;
    private String username;
    private String password;

    public MessageDBRepository(MessageValidator messageValidator, String url, String username, String password) {
        this.messageValidator = messageValidator;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Optional<Message> findOne(UserDBRepository userDBRepository, Long messageId) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "select * from messages "
                             + "where id = ?");
        ) {
            statement.setLong(1, messageId);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                Long id = resultSet.getLong(1);
                Optional<User> userFrom = userDBRepository.findOne(resultSet.getLong(2));
                String message_text = resultSet.getString(3);
                LocalDateTime date = resultSet.getTimestamp(4).toLocalDateTime();
                long replyId = resultSet.getLong(5);
                // get list of receivers
                List<User> usersTo = findAllReceiversForMessage(userDBRepository, messageId);
                if(userFrom.isPresent() && !usersTo.isEmpty()) {
                    Message message = null;
                    if(replyId == 0) {
                        message = new Message(userFrom.get(), usersTo, message_text, date, null);
                    }
                    else {
                        Optional<Message> reply = this.findOne(userDBRepository, replyId);
                        if(reply.isPresent())
                            message = new Message(userFrom.get(), usersTo, message_text, date, reply.get());
                    }
                    Objects.requireNonNull(message).setId(id);
                    return Optional.of(message);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Message> findAll(UserDBRepository userDBRepository) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select * from messages");
             ResultSet resultSet = statement.executeQuery();
        ) {
            Set<Message> messageSet = new HashSet<>();
            while(resultSet.next()) {
                Message message = null;
                Long id = resultSet.getLong(1);
                Optional<User> userFrom = userDBRepository.findOne(resultSet.getLong(2));
                String message_text = resultSet.getString(3);
                LocalDateTime date = resultSet.getTimestamp(4).toLocalDateTime();
                long replyId = resultSet.getLong(5);
                List<User> usersTo = this.findAllReceiversForMessage(userDBRepository, id);
                if(userFrom.isPresent() && !usersTo.isEmpty()) {
                    if(replyId == 0) {
                        message = new Message(userFrom.get(), usersTo, message_text, date, null);
                    }
                    else {
                        Optional<Message> reply = this.findOne(userDBRepository, replyId);
                        if(reply.isPresent())
                            message = new Message(userFrom.get(), usersTo, message_text, date, reply.get());
                    }
                    Objects.requireNonNull(message).setId(id);
                }
                if(message != null)
                    messageSet.add(message);
            }
            return messageSet;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<User> findAllReceiversForMessage(UserDBRepository userDBRepository, Long messageId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement receiversStatement = connection.prepareStatement(
                     "select * from message_receivers "
                             + "where messageid = ?");
        ) {
            receiversStatement.setLong(1, messageId);
            ResultSet receiversResultSet = receiversStatement.executeQuery();
            List<User> usersTo = new ArrayList<>();
            while (receiversResultSet.next()) {
                Long receiverId = receiversResultSet.getLong(3);
                Optional<User> receiver = userDBRepository.findOne(receiverId);
                receiver.ifPresent(usersTo::add);
            }
            return usersTo;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Message> findAll(UserDBRepository userDBRepository, String time) {
        LocalDateTime dateTime = getDateFromTime(time);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select * from messages " +
                     "where datesend > ?");
        ) {
            statement.setTimestamp(1, Timestamp.valueOf(dateTime));
            ResultSet resultSet = statement.executeQuery();
            Set<Message> messageSet = new HashSet<>();
            while(resultSet.next()) {
                Message message = null;
                Long id = resultSet.getLong(1);
                Optional<User> userFrom = userDBRepository.findOne(resultSet.getLong(2));
                String message_text = resultSet.getString(3);
                LocalDateTime date = resultSet.getTimestamp(4).toLocalDateTime();
                long replyId = resultSet.getLong(5);
                List<User> usersTo = this.findAllReceiversForMessage(userDBRepository, id);
                if(userFrom.isPresent() && !usersTo.isEmpty()) {
                    if(replyId == 0) {
                        message = new Message(userFrom.get(), usersTo, message_text, date, null);
                    }
                    else {
                        Optional<Message> reply = this.findOne(userDBRepository, replyId);
                        if(reply.isPresent())
                            message = new Message(userFrom.get(), usersTo, message_text, date, reply.get());
                    }
                    Objects.requireNonNull(message).setId(id);
                }
                if(message != null)
                    messageSet.add(message);
            }
            return messageSet;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Message> findAllMessagesBetweenToUsers(UserDBRepository userDBRepository, User sender, User receiver, String time) {
        LocalDateTime dateTime = getDateFromTime(time);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select * from messages " +
                     "inner join message_receivers on message_receivers.messageid = messages.id " +
                     "where ((fromid = ? and receiverid = ?) or (fromid = ? and receiverid = ?)) and datesend > ?");
        ) {
            statement.setLong(1, sender.getId());
            statement.setLong(2, receiver.getId());
            statement.setLong(3, receiver.getId());
            statement.setLong(4, sender.getId());
            statement.setTimestamp(5, Timestamp.valueOf(dateTime));
            ResultSet resultSet = statement.executeQuery();
            Set<Message> messageSet = new HashSet<>();
            while(resultSet.next()) {
                Message message = null;
                Long id = resultSet.getLong(1);
                String message_text = resultSet.getString(3);
                LocalDateTime date = resultSet.getTimestamp(4).toLocalDateTime();
                long replyId = resultSet.getLong(5);
                List<User> usersTo = this.findAllReceiversForMessage(userDBRepository, id);
                if(!usersTo.isEmpty()) {
                    if(replyId == 0) {
                        message = new Message(sender, usersTo, message_text, date, null);
                    }
                    else {
                        Optional<Message> reply = this.findOne(userDBRepository, replyId);
                        if(reply.isPresent())
                            message = new Message(sender, usersTo, message_text, date, reply.get());
                    }
                    Objects.requireNonNull(message).setId(id);
                }
                if(message != null)
                    messageSet.add(message);
            }
            return messageSet;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Message> findMessageByText(UserDBRepository userDBRepository, String messageText) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "select * from messages "
                             + "where messagetext = ?");
        ) {
            statement.setString(1, messageText);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                Long id = resultSet.getLong(1);
                Optional<User> userFrom = userDBRepository.findOne(resultSet.getLong(2));
                String message_text = resultSet.getString(3);
                LocalDateTime date = resultSet.getTimestamp(4).toLocalDateTime();
                long replyId = resultSet.getLong(5);
                // get list of receivers
                List<User> usersTo = findAllReceiversForMessage(userDBRepository, id);
                if(userFrom.isPresent() && !usersTo.isEmpty()) {
                    Message message = null;
                    if(replyId == 0) {
                        message = new Message(userFrom.get(), usersTo, message_text, date, null);
                    }
                    else {
                        Optional<Message> reply = this.findOne(userDBRepository, replyId);
                        if(reply.isPresent())
                            message = new Message(userFrom.get(), usersTo, message_text, date, reply.get());
                    }
                    Objects.requireNonNull(message).setId(id);
                    return Optional.of(message);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Message> save(Message message) {
        messageValidator.validate(message);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "insert into messages (fromid, messagetext, datesend, replyId)" +
                             "values (?, ?, ?, ?) returning id");
        ) {
            statement.setLong(1, message.getFrom().getId());
            statement.setString(2, message.getMessage());
            statement.setTimestamp(3, Timestamp.valueOf(message.getDate()));
            if(message.getReply() != null)
                statement.setLong(4, message.getReply().getId());
            else
                statement.setNull(4, Types.NULL);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                long generatedId = resultSet.getLong(1);
                message.setId(generatedId);
                saveReceivers(message);
            }
            return Optional.of(message);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveReceivers(Message message) {
        messageValidator.validate(message);
        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement(
                    "insert into message_receivers (messageid, receiverid)" +
                        "values (?, ?)")
        ) {
            statement.setLong(1, message.getId());
            message.getTo().forEach(user -> {
                try {
                    statement.setLong(2, user.getId());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public Optional<Message> update(Message message) {
        // at the moment, this function is only used to update reply
        // TODO - update receivers table
        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement(
                    "update messages " +
                            "set fromid = ?, messagetext = ?, datesend = ?, replyid = ? " +
                            "where id = ?")
        ) {
            statement.setLong(1, message.getFrom().getId());
            statement.setString(2, message.getMessage());
            statement.setTimestamp(3, Timestamp.valueOf(message.getDate()));
            statement.setLong(4, message.getReply().getId());
            statement.setLong(5, message.getId());
            int result = statement.executeUpdate();
            return result == 0 ? Optional.of(message) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDateTime getDateFromTime(String time) {
        return switch (time) {
            case "Today" -> LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            case "This week" -> LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            case "This month" -> LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth())
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            default -> LocalDateTime.ofEpochSecond(0, 0, java.time.ZoneOffset.UTC);
        };
    }
}
