package ir.map.domain;

import java.time.LocalDateTime;

public class Friendship extends Entity<Tuple<Long, Long>>{

    private Long userId1;
    private Long userId2;
    private LocalDateTime friendsFrom;

    public Friendship(Long userId1, Long userId2, LocalDateTime friendsFrom) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.friendsFrom = friendsFrom;
    }

    /**
     *
     * @return the date when the friendship was created
     */
    public LocalDateTime getDate() {
        return friendsFrom;
    }
}
