package ir.map.repository.database;

import ir.map.domain.Friendship;
import ir.map.domain.FriendshipRequest;
import ir.map.domain.Tuple;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class FriendshipRequestDBRepository {

    protected final String url;
    protected final String username;
    protected final String password;

    public FriendshipRequestDBRepository(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Optional<FriendshipRequest> createFriendshipRequest(FriendshipRequest friendshipRequest) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                    "insert into friendship_requests (userid1, userid2, initiatedbyid, status) " +
                            "values (?, ?, ?, ?) ")
            ) {
            statement.setLong(1, friendshipRequest.getId().getLeft());
            statement.setLong(2, friendshipRequest.getId().getRight());
            statement.setLong(3, friendshipRequest.getInitiatedById());
            statement.setString(4, friendshipRequest.getStatus());
            int result = statement.executeUpdate();
            return result == 0 ? Optional.of(friendshipRequest) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<FriendshipRequest> approveFriendshipRequest(Long userId1, Long userId2) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement(
                    "update friendship_requests " +
                            "set status = 'approved' " +
                            "where userid1 = ? and userid2 = ?"
            )) {
            statement.setLong(1, userId1);
            statement.setLong(2, userId2);
            Optional<FriendshipRequest> friendshipRequest = this.findOne(userId1, userId2);
            int result = statement.executeUpdate();
            return result == 0 ? friendshipRequest : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<FriendshipRequest> rejectFriendshipRequest(Long userId1, Long userId2) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "update friendship_requests " +
                             "set status = 'rejected' " +
                             "where userid1 = ? and userid2 = ?"
             )) {
            statement.setLong(1, userId1);
            statement.setLong(2, userId2);
            Optional<FriendshipRequest> friendshipRequest = this.findOne(userId1, userId2);
            int result = statement.executeUpdate();
            return result == 0 ? friendshipRequest : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<FriendshipRequest> deleteFriendshipRequest(Long userId1, Long userId2) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "delete from friendship_requests " +
                             "where userId1 = ? and userId2 = ?")
        ) {
            statement.setLong(1, userId1);
            statement.setLong(2, userId2);
            Optional<FriendshipRequest> friendshipRequest = this.findOne(userId1, userId2);
            int result = statement.executeUpdate();
            return result == 0 ? Optional.empty() : friendshipRequest;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<FriendshipRequest> findOne(Long userId1, Long userId2) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "select * from friendship_requests " +
                             "where (userId1 = ? and userId2 = ?) or (userId1 = ? and userId2 = ?)")
        ) {
            statement.setLong(1, userId1);
            statement.setLong(2, userId2);
            statement.setLong(3, userId2);
            statement.setLong(4, userId1);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                Long initiatedById = resultSet.getLong(3);
                String status = resultSet.getString(4);
                FriendshipRequest friendshipRequest = new FriendshipRequest(userId1, userId2, initiatedById, status);
                friendshipRequest.setId(new Tuple<>(userId1, userId2));
                return Optional.of(friendshipRequest);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<FriendshipRequest> findAll() {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select * from friendship_requests");
             ResultSet resultSet = statement.executeQuery();
        ) {
            Set<FriendshipRequest> friendshipRequestsSet = new HashSet<>();
            while(resultSet.next()) {
                Long userId1 = resultSet.getLong(1);
                Long userId2 = resultSet.getLong(2);
                Long initiatedById = resultSet.getLong(3);
                String status = resultSet.getString(4);
                FriendshipRequest friendshipRequest = new FriendshipRequest(userId1, userId2, initiatedById, status);
                friendshipRequest.setId(new Tuple<>(userId1, userId2));
                friendshipRequestsSet.add(friendshipRequest);
            }
            return friendshipRequestsSet;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<FriendshipRequest> findAllPendingRequestsForUser(Long id) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select * from friendship_requests " +
                     "where status = ? and (userid1 = ? or userid2 = ?)");
        ) {
            statement.setString(1, "pending");
            statement.setLong(2, id);
            statement.setLong(3, id);
            ResultSet resultSet = statement.executeQuery();
            Set<FriendshipRequest> friendshipRequestsSet = new HashSet<>();
            while(resultSet.next()) {
                Long userId1 = resultSet.getLong(1);
                Long userId2 = resultSet.getLong(2);
                Long initiatedById = resultSet.getLong(3);
                String status = resultSet.getString(4);
                FriendshipRequest friendshipRequest = new FriendshipRequest(userId1, userId2, initiatedById, status);
                friendshipRequest.setId(new Tuple<>(userId1, userId2));
                friendshipRequestsSet.add(friendshipRequest);
            }
            return friendshipRequestsSet;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
