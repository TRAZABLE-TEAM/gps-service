package mx.trazable.gps.repository;


import mx.trazable.gps.core.entity.BemGpsTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface BemGpsTrackingRepository extends JpaRepository<BemGpsTracking, Long> {
}
