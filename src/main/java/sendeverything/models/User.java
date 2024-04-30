package sendeverything.models;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import sendeverything.models.room.UserRoom;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        })

public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Size(max = 20)
  private String username;

  @NotBlank
  @Size(max = 50)
  @Email
  private String email;


  @Size(max = 120)
  private String password;



  @ManyToMany(fetch = FetchType.LAZY)// FetchType.LAZY: 延遲加載
  @JoinTable(name = "user_roles",
          joinColumns = @JoinColumn(name = "user_id"),
          inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  @Enumerated(EnumType.STRING)
  private Provider provider;
  @Lob
  private Blob profileImage;
  private String imgUrl;



  public User(String username, String email, String password,Provider provider,Blob profileImage) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.provider = provider;
    this.profileImage = profileImage;
  }



}
