package sendeverything.models.room;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sendeverything.models.User;

import java.time.LocalDateTime;
@Data
@Entity
public class UserRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    private LocalDateTime joinedAt; // 加入时间

    @Lob
    @Column(name = "user_public_key", columnDefinition = "TEXT")
    private String userPublicKey; // 用户公钥

    @Lob
    @Column(name = "user_private_key", columnDefinition = "TEXT")
    private String userPrivateKey; // 用户私钥

    @Lob
    @Column(name = "user_shared_key", columnDefinition = "TEXT")
    private String userSharedKey; // 用户共享密钥

    private Integer userCount; // 用户数量

    public UserRoom() {

    }

    // 构造函数、getter和setter
}
