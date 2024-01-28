package ir.map.repository.database;

import ir.map.domain.Friendship;
import ir.map.domain.Tuple;
import ir.map.domain.validators.FriendshipValidator;
import ir.map.domain.validators.Validator;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class FriendshipDBRepository {
    private final Validator<Friendship> validator;
    protected final String url;
    protected final String username;
    protected final String password;

    public FriendshipDBRepository(FriendshipValidator friendshipValidator, String url, String username, String password) {
        this.validator = friendshipValidator;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Optional<Friendship> createFriendship(Friendship friendship) {
        validator.validate(friendship);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "insert into friendships (userid1, userid2, friendsfrom)" +
                             "values (?, ?, ?)")
        ) {
            statement.setLong(1, friendship.getId().getLeft());
            statement.setLong(2, friendship.getId().getRight());
            statement.setTimestamp(3, Timestamp.valueOf(friendship.getDate()));
            int result = statement.executeUpdate();
            return result == 0 ? Optional.of(friendship) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Friendship> breakFriendship(Long userId1, Long userId2) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement(
                    "delete from friendships " +
                            "where userId1 = ? and userId2 = ?")
        ) {
            statement.setLong(1, userId1);
            statement.setLong(2, userId2);
            Optional<Friendship> friendship = this.findOne(userId1, userId2);
            int result = statement.executeUpdate();
            return result == 0 ? Optional.empty() : friendship;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Friendship> findOne(Long userId1, Long userId2) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement(
                    "select * from friendships " +
                            "where (userId1 = ? and userId2 = ?) or (userId1 = ? and userId2 = ?)")
        ) {
            statement.setLong(1, userId1);
            statement.setLong(2, userId2);
            statement.setLong(3, userId2);
            statement.setLong(4, userId1);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                Timestamp date = resultSet.getTimestamp(3);
                Friendship friendship = new Friendship(userId1, userId2, date.toLocalDateTime());
                friendship.setId(new Tuple<>(userId1, userId2));
                return Optional.of(friendship);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Friendship> findAll() {
        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement("select * from friendships");
            ResultSet resultSet = statement.executeQuery();
        ) {
            Set<Friendship> friendshipSet = new HashSet<>();
            while(resultSet.next()) {
                Long userId1 = resultSet.getLong(1);
                Long userId2 = resultSet.getLong(2);
                LocalDateTime friendsFrom = resultSet.getTimestamp(3).toLocalDateTime();
                Friendship friendship = new Friendship(userId1, userId2, friendsFrom);
                friendship.setId(new Tuple<>(userId1, userId2));
                friendshipSet.add(friendship);
            }
            return friendshipSet;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Friendship> findAllFriendshipsForUser(Long userId) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select * from friendships " +
                     "where userid1 = ? or userid2 = ?");
        ) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            ResultSet resultSet = statement.executeQuery();
            Set<Friendship> friendshipSet = new HashSet<>();
            while(resultSet.next()) {
                Long userId1 = resultSet.getLong(1);
                Long userId2 = resultSet.getLong(2);
                LocalDateTime friendsFrom = resultSet.getTimestamp(3).toLocalDateTime();
                Friendship friendship = new Friendship(userId1, userId2, friendsFrom);
                friendship.setId(new Tuple<>(userId1, userId2));
                friendshipSet.add(friendship);
            }
            return friendshipSet;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
