package ir.map.domain;

public class FriendshipRequest extends Entity<Tuple<Long, Long>>{

    private Long userId1;

    private Long userId2;

    private Long initiatedById;

    private String status;

    public FriendshipRequest(Long userId1, Long userId2, Long initiatedById, String status) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.initiatedById = initiatedById;
        this.status = status;
    }

    public Long getInitiatedById() {
        return initiatedById;
    }

    public String getStatus() {
        return status;
    }
}
