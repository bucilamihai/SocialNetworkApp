package ir.map.repository.database;

import ir.map.domain.User;
import ir.map.domain.validators.Validator;
import ir.map.repository.database.UserDBRepository;
import ir.map.repository.paging.Page;
import ir.map.repository.paging.PageImplementation;
import ir.map.repository.paging.Pageable;
import ir.map.repository.paging.PagingRepository;

import java.sql.*;
import java.util.*;

public class UserDBPagingRepository extends UserDBRepository implements PagingRepository<Long, User> {

    public UserDBPagingRepository(Validator<User> validator, String url, String username, String password) {
        super(validator, url, username, password);
    }

    //    @Override
//    public Page<User> findAll(Pageable pageable) {
//        Stream<User> result = StreamSupport.stream(this.findAll().spliterator(), false)
//                .skip((pageable.getPageNumber() - 1) * pageable.getPageSize())
//                .limit(pageable.getPageSize());
//        return new PageImplementation<>(pageable, result);
//    }
    @Override
    public Page<User> findAll(Pageable pageable) {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select * from users limit ? offset ?");
        ) {
            statement.setInt(1, pageable.getPageSize());
            statement.setInt(2, pageable.getPageSize() * (pageable.getPageNumber() - 1));
            ResultSet resultSet = statement.executeQuery();
            Set<User> userSet = new HashSet<>();
            while (resultSet.next()) {
                String first_name = resultSet.getString(2);
                String last_name = resultSet.getString(3);
                String email = resultSet.getString(4);
                String password = resultSet.getString(5);
                User user = new User(first_name, last_name, email, password);
                user.setId(resultSet.getLong(1));
                userSet.add(user);
            }
            return new PageImplementation<>(pageable, userSet.stream());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Page<User> findAllUsersNotFriendWithUser(Long id, Pageable pageable) {
        // we define that a user is not friend with other user when
        //      that user doesn't have a friendship with other user or doesn't have a friend request
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM users " +
                             "WHERE id NOT IN (SELECT userid2 FROM friendships WHERE userid1 = ? AND userid2 = id)" +
                             "AND id NOT IN (SELECT userid1 FROM friendships WHERE userid1 = id AND userid2 = ?)" +
                             "AND id NOT IN (SELECT userid2 FROM friendship_requests WHERE userid1 = ? AND userid2 = id)" +
                             "AND id NOT IN (SELECT userid1 FROM friendship_requests WHERE userid1 = id AND userid2 = ?)" +
                             "AND id != ? LIMIT ? OFFSET ?"
             );
        ) {
            statement.setLong(1, id);
            statement.setLong(2, id);
            statement.setLong(3, id);
            statement.setLong(4, id);
            statement.setLong(5, id);
            statement.setInt(6, pageable.getPageSize());
            statement.setInt(7, pageable.getPageSize() * (pageable.getPageNumber() - 1));
            ResultSet resultSet = statement.executeQuery();
            Set<User> userSet = new HashSet<>();
            while (resultSet.next()) {
                String first_name = resultSet.getString(2);
                String last_name = resultSet.getString(3);
                String email = resultSet.getString(4);
                String password = resultSet.getString(5);
                User user = new User(first_name, last_name, email, password);
                user.setId(resultSet.getLong(1));
                userSet.add(user);
            }
            return new PageImplementation<>(pageable, userSet.stream());
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
