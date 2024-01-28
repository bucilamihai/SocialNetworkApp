package ir.map.repository.database;

import ir.map.domain.Friendship;
import ir.map.domain.Tuple;
import ir.map.domain.validators.FriendshipValidator;
import ir.map.repository.paging.Page;
import ir.map.repository.paging.PageImplementation;
import ir.map.repository.paging.Pageable;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class FriendshipDBPagingRepository extends FriendshipDBRepository {

    public FriendshipDBPagingRepository(FriendshipValidator friendshipValidator, String url, String username, String password) {
        super(friendshipValidator, url, username, password);
    }

    public Page<Friendship> findAllFriendshipsForUser(Long userId, Pageable pageable) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM friendships WHERE userid1 = ? OR userid2 = ? LIMIT ? OFFSET ?");
        ) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            statement.setInt(3, pageable.getPageSize());
            statement.setInt(4, pageable.getPageSize() * (pageable.getPageNumber() - 1));

            ResultSet resultSet = statement.executeQuery();
            Set<Friendship> friendshipSet = new HashSet<>();
            while (resultSet.next()) {
                Long userId1 = resultSet.getLong(1);
                Long userId2 = resultSet.getLong(2);
                LocalDateTime friendsFrom = resultSet.getTimestamp(3).toLocalDateTime();
                Friendship friendship = new Friendship(userId1, userId2, friendsFrom);
                friendship.setId(new Tuple<>(userId1, userId2));
                friendshipSet.add(friendship);
            }
            return new PageImplementation<>(pageable, friendshipSet.stream());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}