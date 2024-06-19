package project.dao;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.domain.Token;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TokenDao extends JpaRepository<Token, Long> {


    @Query(value = "SELECT COUNT(*) FROM token_tb WHERE token_code = :tokenCode", nativeQuery = true)
    Integer isValidToken(String tokenCode);

    Token findTokenByTokenCode(String tokenCode);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE token_tb SET
    related_user_id = :#{#token.relatedUserId}
    WHERE token_code = :#{#token.tokenCode}
    """, nativeQuery = true)
    void updateToken(Token token);

    @Query(value = "SELECT COUNT(*) FROM token_tb WHERE related_user_id = :userId",nativeQuery = true)
    int isSubscriber(long userId);

    @Query(value = "SELECT COUNT(*) FROM token_tb WHERE related_user_id IS NOT NULL",nativeQuery = true)
    Integer countSubscribers();

    @Query(value = "SELECT related_user_id FROM token_tb;", nativeQuery = true)
    List<Long> allSubscribers();

    @Query(value = "SELECT related_user_id FROM token_tb;",nativeQuery = true)
    List<Long> allUserIdList();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM token_tb WHERE generation_date < :pastMonth", nativeQuery = true )
    void deleteOldTokens(@Param("pastMonth") LocalDate pastMonth);
}