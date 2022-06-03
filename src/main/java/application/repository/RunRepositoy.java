package application.repository;

import application.model.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface RunRepositoy extends JpaRepository<Run,Long> {
    List<Run> findAllByPaceAndDate(double pace, LocalDate date);
    @Query(value = "SELECT * FROM run r INNER JOIN user u ON r.athlete_id = u.athlete_id", nativeQuery = true)
    List<Run> fetchAll();

    @Query(value = "SELECT * FROM run r WHERE r.date between ?1 and ?2 ", nativeQuery = true)
    List<Run> statistic(LocalDate fromDate, LocalDate toDate);
}
