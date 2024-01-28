package ir.map.repository.database;

import ir.map.domain.FriendshipRequest;
import ir.map.domain.Tuple;
import ir.map.repository.paging.Page;
import ir.map.repository.paging.PageImplementation;
import ir.map.repository.paging.Pageable;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class FriendshipRequestDBPagingRepository extends FriendshipRequestDBRepository {
    public FriendshipRequestDBPagingRepository(String url, String username, String password) {
        super(url, username, password);
    }

    public Page<FriendshipRequest> findAllPendingRequestsForUser(Long id, Pageable pageable) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select * from friendship_requests " +
                     "where status = ? and (userid1 = ? or userid2 = ?) limit ? offset ?");
        ) {
            statement.setString(1, "pending");
            statement.setLong(2, id);
            statement.setLong(3, id);
            statement.setInt(4, pageable.getPageSize());
            statement.setInt(5, pageable.getPageSize() * (pageable.getPageNumber() - 1));
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
            return new PageImplementation<>(pageable, friendshipRequestsSet.stream());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
