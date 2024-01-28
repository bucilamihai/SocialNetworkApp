package ir.map.domain;

import java.time.LocalDateTime;
import java.util.List;

public class Message {
    private Long id;
    private final User from;
    private final List<User> to;
    private final String message;
    private final LocalDateTime date;
    private Message reply;

    public Message(User from, List<User> to, String message, LocalDateTime date, Message reply) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.date = date;
        this.reply = reply;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getFrom() {
        return from;
    }

    public List<User> getTo() {
        return to;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Message getReply() {
        return reply;
    }

    public void setReply(Message reply) {
        this.reply = reply;
    }
}
