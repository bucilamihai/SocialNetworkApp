package ir.map.service;

import ir.map.domain.User;
import ir.map.repository.Repository;
import ir.map.repository.database.UserDBRepository;
import ir.map.repository.paging.Page;
import ir.map.repository.paging.Pageable;
import ir.map.repository.paging.PagingRepository;
import ir.map.utils.Graph;
import ir.map.utils.observer.Observable;
import ir.map.utils.observer.Observer;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

public class UserService implements Observable {

    private final Repository<Long, User> repository;

    private final List<Observer> observers = new ArrayList<>();

    public UserService(Repository<Long, User> repository) {
        this.repository = repository;
    }

    public Optional<User> findOne(Long id) {
        return repository.findOne(id);
    }

    public Iterable<User> findAll() {
        return repository.findAll();
    }

    public Page<User> findAll(Pageable pageable) throws ClassCastException{
        return ((PagingRepository<Long, User>) repository).findAll(pageable);
    }

    public int findNumberOfUsers() {
        if(repository instanceof UserDBRepository) {
            return ((UserDBRepository) repository).findNumberOfUsers();
        }
        Iterable<User> users = this.findAll();
        List<User> userList = StreamSupport.stream(users.spliterator(), false)
                .toList();
        return userList.size();
    }

    public Optional<User> save(String firstName, String lastName, String email, String password) {
        Optional<User> user = repository.save(new User(firstName, lastName, email, password));
        if(user.isEmpty())
            notifyObservers();
        return user;
    }

    public Optional<User> delete(Long id) {
        Optional<User> user = repository.delete(id);
        if(user.isPresent())
            notifyObservers();
        return user;
    }

    public Optional<User> update(User user) {
        Optional<User> updatedUser = repository.update(user);
        if(updatedUser.isEmpty())
            notifyObservers();
        return updatedUser;
    }

    private Graph createGraph() {
        Graph community = new Graph();
        repository.findAll().forEach(user -> user.getFriends().forEach(friend -> community.addEdge(user.getId(), friend.getId())));
        return community;
    }

    public int numberOfCommunities() {
        Graph community = createGraph();
        return community.numberOfConnectedComponents();
    }

    public List<User> findLargestCommunity() {
        Graph community = createGraph();
        List<Long> usersIdCommunity = community.largestCommunity();
        List<User> largestCommunity = new ArrayList<>();
        usersIdCommunity.forEach(userId -> {
            Optional<User> user = repository.findOne(userId);
            user.ifPresent(largestCommunity::add);
        });
        return largestCommunity;
    }

    public User findUserByName(String firstName, String lastName) {
        Iterable<User> users = this.findAll();
        AtomicReference<User> foundUser = new AtomicReference<>();
        users.forEach(user -> {
            if(Objects.equals(user.getFirstName(), firstName) && Objects.equals(user.getLastName(), lastName)) {
                foundUser.set(user);
            }
        });
        return foundUser.get();
    }

    public Optional<User> findUserByEmailAndPassword(String email, String password) {
        if(repository instanceof UserDBRepository)
            return ((UserDBRepository) repository).findUserByEmailAndPassword(email, password);
        AtomicReference<User> foundUser = new AtomicReference<>();
        Iterable<User> userList = this.findAll();
        userList.forEach(user -> {
            if(Objects.equals(user.getEmail(), email) && Objects.equals(user.getPassword(), password))
                foundUser.set(user);
        });
        return Optional.ofNullable(foundUser.get());
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        observers.forEach(Observer::update);
    }
}
