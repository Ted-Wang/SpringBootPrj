package psn.ted.demo.springboot.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import psn.ted.demo.springboot.entity.EquityPosition;

@Repository
public interface EquityPositionRepository extends JpaRepository<EquityPosition, Long> {
	Optional<EquityPosition> findByEquity(String equity);
}
