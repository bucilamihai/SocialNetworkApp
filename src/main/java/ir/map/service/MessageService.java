package ir.map.service;

import ir.map.domain.Message;
import ir.map.domain.User;
import ir.map.repository.Repository;
import ir.map.repository.database.MessageDBRepository;
import ir.map.repository.database.UserDBPagingRepository;
import ir.map.repository.database.UserDBRepository;
import ir.map.repository.paging.Pageable;
import ir.map.utils.observer.Observable;
import ir.map.utils.observer.Observer;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

public class MessageService implements Observable {

    private final Repository<Long, User> userRepository;

    private final MessageDBRepository messageRepository;

    private final List<Observer> observers = new ArrayList<>();

    public MessageService(Repository<Long, User> userRepository, MessageDBRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    public Optional<Message> findOne(Long messageId) {
        return messageRepository.findOne((UserDBRepository) userRepository, messageId);
    }

    public Iterable<Message> findAll() {
        return messageRepository.findAll((UserDBRepository) userRepository);
    }

    public Optional<Message> save(User from, List<User> to, String message, LocalDateTime date, Message reply) {
        Optional<Message> optionalMessage = messageRepository.save(new Message(from, to, message, date, reply));
        if (optionalMessage.isPresent())
            notifyObservers();
        return optionalMessage;
    }

    public Optional<Message> findMessageByText(String messageText) {
        return messageRepository.findMessageByText((UserDBRepository) userRepository, messageText);
    }

    public Iterable<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<User> findAllUsers(Pageable pageable) {
        if(userRepository instanceof UserDBPagingRepository)
            return ((UserDBPagingRepository) userRepository).findAll(pageable).getContent().toList();
        return new ArrayList<>();
    }

    public int findNumberOfUsers() {
        if(userRepository instanceof UserDBRepository) {
            return ((UserDBRepository) userRepository).findNumberOfUsers();
        }
        Iterable<User> users = userRepository.findAll();
        List<User> userList = StreamSupport.stream(users.spliterator(), false)
                .toList();
        return userList.size();
    }

    public List<Message> findAllMessagesBetweenUsers(User sender, List<User> receivers, String time) {
        List<Message> messageList = new ArrayList<>();
        Set<Long> uniqueMessageId = new HashSet<>();
        receivers.forEach(receiver -> {
            List<Message> messagesBetweenTwoUsers = findAllMessagesBetweenTwoUsers(sender, receiver, time);
            messagesBetweenTwoUsers.forEach(message -> {
                if (uniqueMessageId.add(message.getId())) {
                    messageList.add(message);
                }
            });
        });
        messageList.sort(Comparator.comparing(Message::getDate));
        return messageList;
    }

    private List<Message> findAllMessagesBetweenTwoUsers(User sender, User receiver, String time) {
        Iterable<Message> messages = messageRepository.findAllMessagesBetweenToUsers((UserDBRepository) userRepository, sender, receiver, time);
        return StreamSupport.stream(messages.spliterator(), false).toList();
//        Iterable<Message> messages = messageRepository.findAll((UserDBRepository) userRepository, time);
//        List<Message> messageList = new ArrayList<>();
//        messages.forEach(message -> {
//            if ((Objects.equals(message.getFrom(), sender) && message.getTo().contains(receiver))
//                    ||
//                    (Objects.equals(message.getFrom(), receiver) && message.getTo().contains(sender)))
//                messageList.add(message);
//        });
//        return messageList;
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

    public Optional<Message> setReply(Message message, Message reply) {
        message.setReply(reply);
        return messageRepository.update(message);
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
        this.observers.forEach(Observer::update);
    }

}
