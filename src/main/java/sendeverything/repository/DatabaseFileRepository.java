package sendeverything.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sendeverything.models.DatabaseFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sendeverything.models.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseFileRepository extends JpaRepository<DatabaseFile, String> {





    @Query("SELECT df FROM DatabaseFile df WHERE df.timestamp < :cutoff")
    List<DatabaseFile> findAllOlderThanTwoDays(@Param("cutoff") LocalDateTime cutoff);




    Optional<DatabaseFile> findByFileName(String fileName);

    Optional<DatabaseFile> findByFileId(String fileId);

    List<DatabaseFile>findAllByUserOrderByTimestampDesc(User user);



    Optional<DatabaseFile> findByVerificationCode(String verificationCode);
    void deleteById(String id);





    List<DatabaseFile> findByUserIdIsNullAndTimestampBefore(Instant time);

}