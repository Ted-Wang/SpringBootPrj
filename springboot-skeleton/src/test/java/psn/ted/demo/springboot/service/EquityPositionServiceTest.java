package psn.ted.demo.springboot.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import psn.ted.demo.springboot.entity.EquityPosition;
import psn.ted.demo.springboot.entity.Transaction;
import psn.ted.demo.springboot.exception.UnexpectedTransaction;
import psn.ted.demo.springboot.service.EquityPositionService;
import psn.ted.demo.springboot.service.EquityPositionServiceImpl;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EquityPositionServiceTest {
	@Autowired
	EquityPositionService service;
	@PersistenceContext
	private EntityManager em;

	@Test
	public void testSequentialTransaction() throws UnexpectedTransaction {
		cleanup();

		Transaction trans1 = new Transaction(1L, 1L, 1L, "REL", 50, "INSERT", "Buy");
		Transaction trans2 = new Transaction(2L, 2L, 1L, "ITC", 40, "INSERT", "Sell");
		service.acceptTransaction(trans1);
		service.acceptTransaction(trans2);
//		service.processStoredTransaction();
		List<EquityPosition> resultList = service.listEquityPositions();
		Map<String, Long> resultMap = new HashMap<>();
		resultList.forEach(equity -> resultMap.put(equity.getEquity(), equity.getPosition()));

		Assert.assertEquals(new Long(50L), resultMap.get("REL"));
		Assert.assertEquals(new Long(-40L), resultMap.get("ITC"));
	}

	@Test
	public void testNonSequentialTransaction() throws UnexpectedTransaction {
		cleanup();

		List<Transaction> transList = new ArrayList<>();
		transList.add(new Transaction(4L, 1L, 2L, "REL", 60, "UPDATE", "Buy"));
		transList.add(new Transaction(1L, 1L, 1L, "REL", 50, "INSERT", "Buy"));
		transList.add(new Transaction(5L, 2L, 2L, "ITC", 30, "CANCEL", "Buy"));
		transList.add(new Transaction(2L, 2L, 1L, "ITC", 40, "INSERT", "Sell"));
		transList.add(new Transaction(6L, 4L, 1L, "INF", 20, "INSERT", "Sell"));
		transList.add(new Transaction(3L, 3L, 1L, "INF", 70, "INSERT", "Buy"));

		for (Transaction Transaction : transList) {
			service.acceptTransaction(Transaction);
		}
//		service.processStoredTransaction();
		List<EquityPosition> resultList = service.listEquityPositions();
		Map<String, Long> resultMap = new HashMap<>();
		resultList.forEach(equity -> resultMap.put(equity.getEquity(), equity.getPosition()));

		Assert.assertEquals(new Long(60L), resultMap.get("REL"));
		Assert.assertEquals(new Long(0L), resultMap.get("ITC"));
		Assert.assertEquals(new Long(50L), resultMap.get("INF"));
	}
	
	@Test
	public void testUpdateAfterCancel() throws UnexpectedTransaction {
		cleanup();

		List<Transaction> transList = new ArrayList<>();
		transList.add(new Transaction(1L, 2L, 1L, "ITC", 40, "INSERT", "Sell"));
		transList.add(new Transaction(2L, 2L, 2L, "ITC", 30, "UPDATE", "Buy"));
		transList.add(new Transaction(3L, 2L, 3L, "ITC", 40, "CANCEL", "Sell"));
		transList.add(new Transaction(4L, 2L, 4L, "ITC", 30, "UPDATE", "Buy"));

		for (Transaction Transaction : transList) {
			service.acceptTransaction(Transaction);
		}
//		service.processStoredTransaction();
		List<EquityPosition> resultList = service.listEquityPositions();
		Map<String, Long> resultMap = new HashMap<>();
		resultList.forEach(equity -> resultMap.put(equity.getEquity(), equity.getPosition()));

		Assert.assertEquals(new Long(0L), resultMap.get("ITC"));
	}

	@Transactional
	public void cleanup() {
		// EntityManager em = entityManagerFactory.createEntityManager();
		// em.getTransaction().begin();
		// em.createNativeQuery("truncate table Transaction").executeUpdate();
		// em.createNativeQuery("truncate table TradeStatus").executeUpdate();
		// em.getTransaction().commit();
		((EquityPositionServiceImpl) service).cleanup();
	}

	@Test(expected = UnexpectedTransaction.class)
	public void testExceptionTransaction1() throws UnexpectedTransaction {
		cleanup();

		Transaction trans1 = new Transaction(1L, 1L, 2L, "REL", 50, "INSERT", "Buy");
		Transaction trans2 = new Transaction(2L, 2L, 1L, "ITC", 40, "INSERT", "Sell");
		service.acceptTransaction(trans1);
		service.acceptTransaction(trans2);
//		service.processStoredTransaction();
		List<EquityPosition> resultList = service.listEquityPositions();
		Map<String, Long> resultMap = new HashMap<>();
		resultList.forEach(equity -> resultMap.put(equity.getEquity(), equity.getPosition()));
	}
	
	@Test(expected = UnexpectedTransaction.class)
	public void testExceptionTransaction2() throws UnexpectedTransaction {
		cleanup();

		List<Transaction> transList = new ArrayList<>();
		// CANCEL transaction of ITC have lower trade version then INSERT message.
		transList.add(new Transaction(2L, 2L, 3L, "ITC", 30, "UPDATE", "Buy"));
		transList.add(new Transaction(4L, 2L, 2L, "ITC", 40, "CANCEL", "Sell"));
		transList.add(new Transaction(3L, 2L, 1L, "ITC", 40, "INSERT", "Sell"));

		for (Transaction Transaction : transList) {
			service.acceptTransaction(Transaction);
		}
//		service.processTransaction();
		List<EquityPosition> resultList = service.listEquityPositions();
		Map<String, Long> resultMap = new HashMap<>();
		resultList.forEach(equity -> resultMap.put(equity.getEquity(), equity.getPosition()));
	}
}
