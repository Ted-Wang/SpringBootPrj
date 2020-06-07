package psn.ted.demo.springboot.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import psn.ted.demo.springboot.entity.EquityPosition;
import psn.ted.demo.springboot.entity.TradeStatus;

@Repository
public interface TradeStatusRepository extends JpaRepository<TradeStatus, Long> {

	@Query("select t.securityCode as equity, sum(t.quantity) as position from TradeStatus t group by t.securityCode")
	List<EquityPosition> listEquityPosition();
}
