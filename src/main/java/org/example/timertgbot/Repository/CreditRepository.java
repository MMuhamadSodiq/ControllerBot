package org.example.timertgbot.Repository;

import org.example.timertgbot.Entity.Credit;
import org.example.timertgbot.TG.CreditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CreditRepository extends JpaRepository<Credit, UUID> {
    List<Credit> findAllByCreditStatusOrderByEndDate(CreditStatus creditStatus);
    @Query(value = """
             SELECT * from credit
             where EXTRACT(MONTH FROM credit.created_date)=EXTRACT(MONTH FROM CURRENT_TIMESTAMP)  order by end_date 
            """, nativeQuery = true)
    List<Credit> findAllByLastMonth();

    List<Credit> findAllByCreditStatus(CreditStatus creditStatus);
    @Query(value = """
  SELECT * from credit
             where EXTRACT(MONTH FROM credit.created_date)=EXTRACT(MONTH FROM CURRENT_TIMESTAMP)  and staff_id=:staff order by end_date
""",nativeQuery = true)
    List<Credit> findAllByStaff(UUID staff);
    List<Credit> findAllByCreditStatusAndStaffId(CreditStatus creditStatus, UUID staffId);
    @Query(value = """
SELECT * FROM credit WHERE end_date <= :now
""", nativeQuery = true)
    List<Credit> findExpiredCredit(LocalDateTime now);
}
