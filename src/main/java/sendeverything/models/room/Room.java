package sendeverything.models.room;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import sendeverything.models.User;

import java.time.LocalDateTime;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "room")
@ToString(exclude = "dbRoomFiles")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roomCode;
    private String title;
    @Lob
    @Column(name = "description", nullable = false,columnDefinition = "TEXT")
    private String description;

    private String password;
    @Lob
    private Blob image;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User owner;
    private String members;
    @Enumerated(EnumType.STRING)
    private RoomType roomType;
    @Enumerated(EnumType.STRING)
    private BoardType boardType;
    private LocalDateTime createTime;
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DBRoomFile> dbRoomFiles = new ArrayList<>();

    @Lob
    @Column(name = "room_prime", nullable = false,columnDefinition = "TEXT")
    private String roomPrime;

    @Lob
    @Column(name = "init_Vector", nullable = false,columnDefinition = "TEXT")
    private String initVector;

    public Room(String roomCode, String title, String description, String password, Blob image, RoomType roomType,BoardType boardType, LocalDateTime createTime, String roomPrime, String initVector) {
        this.roomCode = roomCode;
        this.title = title;
        this.description = description;
        this.password = password;
        this.image = image;
        this.roomType = roomType;
        this.boardType = boardType;
        this.createTime = createTime;
        this.roomPrime = roomPrime;
        this.initVector = initVector;
    }


    public Room() {

    }
}
