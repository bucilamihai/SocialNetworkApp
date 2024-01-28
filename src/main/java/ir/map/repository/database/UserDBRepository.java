package ir.map.repository.database;

import ir.map.domain.Message;
import ir.map.domain.User;
import ir.map.domain.validators.Validator;
import ir.map.repository.Repository;

import java.sql.*;
import java.util.*;

public class UserDBRepository implements Repository<Long, User> {
    private final Validator<User> validator;
    protected final String url;
    protected final String username;
    protected final String password;

    public UserDBRepository(Validator<User> validator, String url, String username, String password) {
        this.validator = validator;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public Optional<User> findOne(Long id) {
        if(id == null) {
            throw new IllegalArgumentException("id must be not null");
        }
        try(Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement("select * from users where id = ?")
        ) {
            statement.setLong(1, id);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                String first_name = resultSet.getString(2);
                String last_name = resultSet.getString(3);
                String email = resultSet.getString(4);
                String password = resultSet.getString(5);
                User user = new User(first_name, last_name, email, password);
                user.setId(id);
                return Optional.of(user);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterable<User> findAll() {
        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement("select * from users");
            ResultSet resultSet = statement.executeQuery();
        ) {
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
            return userSet;
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Optional<User> save(User entity) {
        if(entity == null)
            throw new IllegalArgumentException("entity must be not null");
        validator.validate(entity);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "insert into users (first_name, last_name, email, password)" +
                     "values (?, ?, ?, crypt(?, gen_salt('bf')))")
        ) {
            statement.setString(1, entity.getFirstName());
            statement.setString(2, entity.getLastName());
            statement.setString(3, entity.getEmail());
            statement.setString(4, entity.getPassword());
            int result = statement.executeUpdate();
            return result == 0 ? Optional.of(entity) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<User> delete(Long id) {
        if(id == null)
            throw new IllegalArgumentException("id must be not null");
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                    "delete from users where id = ?")
        ) {
           statement.setLong(1, id);
           Optional<User> user = this.findOne(id);
           int result = statement.executeUpdate();
           return result == 0 ? Optional.empty() : user;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<User> update(User entity) {
        if(entity == null)
            throw new IllegalArgumentException("entity must be not null");
        validator.validate(entity);
        try (Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement("update users " +
                    "set first_name = ?, last_name = ? " +
                    "where id = ?")) {
            statement.setString(1, entity.getFirstName());
            statement.setString(2, entity.getLastName());
            statement.setLong(3, entity.getId());
            int result = statement.executeUpdate();
            return result == 0 ? Optional.of(entity) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<User> findAllUsersNotFriendWithUser(Long id) {
        // we define that a user is not friend with other user when
        //      that user doesn't have a friendship with other user or doesn't have a friend request
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM users " +
                             "WHERE id NOT IN (SELECT userid2 FROM friendships WHERE userid1 = ? AND userid2 = id)" +
                             "AND id NOT IN (SELECT userid1 FROM friendships WHERE userid1 = id AND userid2 = ?)" +
                             "AND id NOT IN (SELECT userid2 FROM friendship_requests WHERE userid1 = ? AND userid2 = id)" +
                             "AND id NOT IN (SELECT userid1 FROM friendship_requests WHERE userid1 = id AND userid2 = ?)" +
                             "AND id != ?"
             );
        ) {
            statement.setLong(1, id);
            statement.setLong(2, id);
            statement.setLong(3, id);
            statement.setLong(4, id);
            statement.setLong(5, id);
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
            return userSet;
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int findNumberOfUsers() {
        int count = 0;
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement("select count(*) from users");
             ResultSet resultSet = statement.executeQuery()
        ) {
            if(resultSet.next())
                count = resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return count;
    }

    public Optional<User> findUserByName(String firstName, String lastName) {
        try(Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement("select * from users " +
                    "where first_name = ? and last_name = ?")
        ) {
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                Long id = resultSet.getLong(1);
                String email = resultSet.getString(4);
                String password = resultSet.getString(5);
                User user = new User(firstName, lastName, email, password);
                user.setId(id);
                return Optional.of(user);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<User> findUserByEmailAndPassword(String user_email, String user_password) {
        try(Connection connection = DriverManager.getConnection(url, username, password);
            PreparedStatement statement = connection.prepareStatement("select * from users " +
                    "where email = ? and password = crypt(?, password)")
        ) {
            statement.setString(1, user_email);
            statement.setString(2, user_password);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                Long id = resultSet.getLong(1);
                String first_name = resultSet.getString(2);
                String last_name = resultSet.getString(3);
                User user = new User(first_name, last_name, user_email, user_password);
                user.setId(id);
                return Optional.of(user);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
