package project.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import project.domain.User;
import java.time.LocalDateTime;

@Repository
public interface UserDao extends JpaRepository<User, Long> {

    @Query(value = "SELECT COUNT(*) FROM user_tb WHERE user_id = :userId",nativeQuery = true)
    int isRegistered(long userId);

    @Query(value = "SELECT registration_date FROM user_tb WHERE user_id = :userId ",nativeQuery = true)
    LocalDateTime getUserRegistrationDate(long userId);
}
