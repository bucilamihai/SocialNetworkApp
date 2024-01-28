package ir.map.service;

import ir.map.domain.Friendship;
import ir.map.domain.FriendshipRequest;
import ir.map.domain.Tuple;
import ir.map.domain.User;
import ir.map.repository.Repository;
import ir.map.repository.database.*;
import ir.map.repository.paging.Page;
import ir.map.repository.paging.Pageable;
import ir.map.repository.paging.PagingRepository;
import ir.map.utils.observer.Observable;
import ir.map.utils.observer.Observer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

public class FriendshipService implements Observable{
    private final Repository<Long, User> userRepository;
    private final FriendshipDBRepository friendshipDBRepository;

    private final FriendshipRequestDBRepository friendshipRequestDBRepository;

    private final List<Observer> observers = new ArrayList<>();

    public FriendshipService(Repository<Long, User> userRepository, FriendshipDBRepository friendshipDBRepository, FriendshipRequestDBRepository friendshipRequestDBRepository) {
        this.userRepository = userRepository;
        this.friendshipDBRepository = friendshipDBRepository;
        this.friendshipRequestDBRepository = friendshipRequestDBRepository;
    }

    public Optional<FriendshipRequest> createFriendshipRequest(Long userId1, Long userId2, Long initiatedById) {
        Optional<User> user1 = userRepository.findOne(userId1);
        Optional<User> user2 = userRepository.findOne(userId2);
        if(user1.isPresent() && user2.isPresent()) {
            if(userId1 > userId2) { // respect constraint from DB
                Long aux = userId1;
                userId1 = userId2;
                userId2 = aux;
            }
            FriendshipRequest friendshipRequest = new FriendshipRequest(userId1, userId2, initiatedById, "pending");
            friendshipRequest.setId(new Tuple<>(userId1, userId2));
            Optional<FriendshipRequest> optionalFriendshipRequest = friendshipRequestDBRepository.createFriendshipRequest(friendshipRequest);
            if (optionalFriendshipRequest.isEmpty())
                notifyObservers();
            return optionalFriendshipRequest;
        }
        return Optional.empty();
    }

    public Optional<Friendship> approveFriendshipRequest(Long userId1, Long userId2) {
        Optional<User> user1 = userRepository.findOne(userId1);
        Optional<User> user2 = userRepository.findOne(userId1);
        if(user1.isPresent() && user2.isPresent()) {
            if(userId1 > userId2) { // respect constraint from DB
                Long aux = userId1;
                userId1 = userId2;
                userId2 = aux;
            }
            Friendship friendship = new Friendship(userId1, userId2, LocalDateTime.now());
            friendship.setId(new Tuple<>(userId1, userId2));
            // create friendship
            Optional<Friendship> optionalFriendship = friendshipDBRepository.createFriendship(friendship);
            // approve request
            friendshipRequestDBRepository.approveFriendshipRequest(userId1, userId2);
            if(optionalFriendship.isEmpty())
                notifyObservers();
            return optionalFriendship;
        }
        return Optional.empty();
    }

    public Optional<FriendshipRequest> rejectFriendshipRequest(Long userId1, Long userId2) {
        Optional<User> user1 = userRepository.findOne(userId1);
        Optional<User> user2 = userRepository.findOne(userId2);
        if(user1.isPresent() && user2.isPresent()) {
            if(userId1 > userId2) { // respect constraint from DB
                Long aux = userId1;
                userId1 = userId2;
                userId2 = aux;
            }
            Optional<FriendshipRequest> friendshipRequest = friendshipRequestDBRepository.rejectFriendshipRequest(userId1, userId2);
            if(friendshipRequest.isEmpty())
                notifyObservers();
            return friendshipRequest;
        }
        return Optional.empty();
    }

    public Optional<Friendship> breakFriendship(Long userId1, Long userId2) {
        Optional<User> user1 = userRepository.findOne(userId1);
        Optional<User> user2 = userRepository.findOne(userId2);
        if(user1.isPresent() && user2.isPresent()) {
            if(userId1 > userId2) { // respect constraint from DB
                Long aux = userId1;
                userId1 = userId2;
                userId2 = aux;
            }
            Optional<Friendship> friendship = friendshipDBRepository.breakFriendship(userId1, userId2);
            if(friendship.isPresent()) {
                friendshipRequestDBRepository.deleteFriendshipRequest(userId1, userId2);
                notifyObservers();
            }
            return friendship;
        }
        return Optional.empty();
    }

    public List<User> findAllFriendsForUser(Long userId) {
        Iterable<Friendship> userFriendships = friendshipDBRepository.findAllFriendshipsForUser(userId);
        List<User> userFriends = new ArrayList<>();
        userFriendships.forEach(friendship -> {
            Optional<User> friend;
            if(Objects.equals(friendship.getId().getLeft(), userId)) {
                friend = userRepository.findOne(friendship.getId().getRight());
            } else {
                friend = userRepository.findOne(friendship.getId().getLeft());
            }
            friend.ifPresent(userFriends::add);
        });
        return userFriends;
    }

    public List<User> findAllFriendsForUser(Long userId, Pageable pageable) throws ClassCastException {
        Page<Friendship> userFriendships = ((FriendshipDBPagingRepository)friendshipDBRepository).findAllFriendshipsForUser(userId, pageable);
        List<User> userFriends = new ArrayList<>();
        userFriendships.getContent().forEach(friendship -> {
            Optional<User> friend;
            if(Objects.equals(friendship.getId().getLeft(), userId)) {
                friend = userRepository.findOne(friendship.getId().getRight());
            } else {
                friend = userRepository.findOne(friendship.getId().getLeft());
            }
            friend.ifPresent(userFriends::add);
        });
        return userFriends;
    }

    public List<User> findAllUsersNotFriendWithUser(Long id) {
        if(userRepository instanceof UserDBRepository)
            return StreamSupport.stream(((UserDBRepository) userRepository).findAllUsersNotFriendWithUser(id).spliterator(), false).toList();
        return new ArrayList<>();
    }

    public List<User> findAllUsersNotFriendWithUser(Long id, Pageable pageable) {
        if(userRepository instanceof UserDBPagingRepository)
            return ((UserDBPagingRepository) userRepository).findAllUsersNotFriendWithUser(id, pageable).getContent().toList();
        return new ArrayList<>();
    }

    public List<User> findAllUsersFromPendingRequestsForUser(Long id) {
        Iterable<FriendshipRequest> pendingRequests = friendshipRequestDBRepository.findAllPendingRequestsForUser(id);
        List<User> usersWithPendingRequests = new ArrayList<>();
        pendingRequests.forEach(friendship -> {
            if(!Objects.equals(friendship.getInitiatedById(), id)) {
                Optional<User> friend;
                if(Objects.equals(friendship.getId().getLeft(), id))
                    friend = userRepository.findOne(friendship.getId().getRight());
                else
                    friend = userRepository.findOne(friendship.getId().getLeft());
                friend.ifPresent(usersWithPendingRequests::add);
            }
        });
        return usersWithPendingRequests;
    }

    public List<User> findAllUsersFromPendingRequestsForUser(Long id, Pageable pageable) {
        Page<FriendshipRequest> pendingRequests = ((FriendshipRequestDBPagingRepository)friendshipRequestDBRepository).findAllPendingRequestsForUser(id, pageable);
        List<User> usersWithPendingRequests = new ArrayList<>();
        pendingRequests.getContent().forEach(friendship -> {
            if(!Objects.equals(friendship.getInitiatedById(), id)) {
                Optional<User> friend;
                if(Objects.equals(friendship.getId().getLeft(), id))
                    friend = userRepository.findOne(friendship.getId().getRight());
                else
                    friend = userRepository.findOne(friendship.getId().getLeft());
                friend.ifPresent(usersWithPendingRequests::add);
            }
        });
        return usersWithPendingRequests;
    }

    public Optional<User> findUserByName(String firstName, String lastName) {
        if(userRepository instanceof UserDBRepository)
            return ((UserDBRepository) userRepository).findUserByName(firstName, lastName);
        Iterable<User> users = userRepository.findAll();
        AtomicReference<User> foundUser = new AtomicReference<>();
        users.forEach(user -> {
            if(Objects.equals(user.getFirstName(), firstName) && Objects.equals(user.getLastName(), lastName)) {
                foundUser.set(user);
            }
        });
        return Optional.ofNullable(foundUser.get());
    }

    @Override
    public void addObserver(Observer observer) {
        this.observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        this.observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        observers.forEach(Observer::update);
    }
}
