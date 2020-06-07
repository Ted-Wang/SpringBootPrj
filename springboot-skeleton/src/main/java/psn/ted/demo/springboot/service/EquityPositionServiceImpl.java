package psn.ted.demo.springboot.service;

import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import psn.ted.demo.springboot.dao.EquityPositionRepository;
import psn.ted.demo.springboot.dao.TradeStatusRepository;
import psn.ted.demo.springboot.dao.TransactionRepository;
import psn.ted.demo.springboot.entity.EquityPosition;
import psn.ted.demo.springboot.entity.TradeStatus;
import psn.ted.demo.springboot.entity.Transaction;
import psn.ted.demo.springboot.exception.UnexpectedTransaction;

@Service
public class EquityPositionServiceImpl implements EquityPositionService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String TRADE_ACTION_INSERT = "INSERT";
	public static final String TRADE_ACTION_UPDATE = "UPDATE";
	public static final String TRADE_ACTION_CANCEL = "CANCEL";

	public static final String TRANSACTION_TYPE_BUY = "Buy";
	public static final String TRANSACTION_TYPE_SELL = "Sell";

	@Resource
	private TradeStatusRepository tradeStatusRepository;
	@Resource
	private TransactionRepository transactionRepository;
	@Resource
	private EquityPositionRepository equityPositionRepository;

	/**
	 * Main service method, stand by to accept and process transactions For those
	 * non-sequenced transactions, store into database to process later.
	 */
	@Override
	public void acceptTransaction(Transaction transaction) throws UnexpectedTransaction {

		// check empty fields
		if (transaction.getTransactionId() == null || transaction.getTradeId() == null
				|| transaction.getVersion() == null || transaction.getQuantity() == null
				|| transaction.getSecurityCode() == null || transaction.getTradeActionType() == null
				|| transaction.getTransactionType() == null) {
			String msg = "Invalid transaction, empty field(s) detected: " + transaction.toString();
			logger.error(msg);
			throw new UnexpectedTransaction(msg);
		}

		// Business Rule check.
		// check if INSERT transaction with correct version number = 1
		if (TRADE_ACTION_INSERT.equals(transaction.getTradeActionType()) && transaction.getVersion() != 1) {
			String msg = "Invalid transaction, \"INSERT\" trade with incorrect version number(1 expected): "
					+ transaction.toString();
			logger.error(msg);
			throw new UnexpectedTransaction(msg);
		}

		// process correct INSERT trade immediately
		if (TRADE_ACTION_INSERT.equals(transaction.getTradeActionType())) {
			logger.info("Processing transaction: {}", transaction.toString());
			processTradeInsert(transaction);
		}

		// try to process UPDATE trade, if not able to, save to DB, to be processed
		// later.
		if (TRADE_ACTION_UPDATE.equals(transaction.getTradeActionType())) {
			boolean processed = processTradeUpdate(transaction);
			if (!processed) {
				logger.info("Save unprocessed transaction: {}", transaction.toString());
				transactionRepository.save(transaction);
			}
		}

		// process CANCEL trade like UPDATE trade.
		if (TRADE_ACTION_CANCEL.equals(transaction.getTradeActionType())) {
			boolean processed = processTradeCancel(transaction);
			if (!processed) {
				logger.info("Save unprocessed transaction: {}", transaction.toString());
				transactionRepository.save(transaction);
			}
		}

	}

	/**
	 * This method handle with those stored non-sequenced transactions. Normally, an
	 * UPDATE or CANCEL trade arrives before INSERT, would go into this process.
	 * This method is supposed to be invoked in any time. It is invoked in
	 * listEquityPositions() before query execute.
	 */
	@Override
	public void processStoredTransaction() throws UnexpectedTransaction {
		preProcess();

		List<Transaction> tranListInsert = transactionRepository
				.findByTradeActionTypeAndProcessedOrderByTransactionId(TRADE_ACTION_INSERT, Boolean.FALSE);
		for (Transaction transaction : tranListInsert) {
			processTradeInsert(transaction);
		}

		List<Transaction> tranListUpdate = transactionRepository
				.findByTradeActionTypeAndProcessedOrderByTransactionId(TRADE_ACTION_UPDATE, Boolean.FALSE);
		for (Transaction transaction : tranListUpdate) {
			if (processTradeUpdate(transaction)) {
				transactionRepository.delete(transaction);
			}
		}

		List<Transaction> tranListCancel = transactionRepository
				.findByTradeActionTypeAndProcessedOrderByTransactionId(TRADE_ACTION_CANCEL, Boolean.FALSE);
		for (Transaction transaction : tranListCancel) {
			if (processTradeCancel(transaction)) {
				transactionRepository.delete(transaction);
			}
		}

		postProcess();
	}

	@Transactional
	protected TradeStatus processTradeInsert(Transaction transaction) throws UnexpectedTransaction {

		Optional<TradeStatus> trade = tradeStatusRepository.findById(transaction.getTradeId());
		if (trade.isPresent()) {
			String msg = "Invalid transaction, duplicated \"INSERT\" trade detected: " + transaction.toString();
			logger.error(msg);
			throw new UnexpectedTransaction(msg);
		}

		TradeStatus tradeStatus = new TradeStatus();
		tradeStatus.setTradeId(transaction.getTradeId());
		tradeStatus.setSecurityCode(transaction.getSecurityCode());
		Integer quantiy = transaction.getQuantity();
		if (TRANSACTION_TYPE_SELL.equals(transaction.getTransactionType())) {
			quantiy = -1 * quantiy;
		}
		tradeStatus.setQuantity(quantiy);

		tradeStatus.setLastTradeVersion(transaction.getVersion());
		tradeStatus.setLastTransactionId(transaction.getTransactionId());
		tradeStatus.setLastTransactionType(transaction.getTransactionType());
		tradeStatus.setLastTradeActionType(transaction.getTradeActionType());

		// transaction.setProcessed(Boolean.TRUE);
		// transactionRepository.save(transaction);
		TradeStatus updatedTradeStatus = tradeStatusRepository.save(tradeStatus);

		updateEquityPosition(tradeStatus.getSecurityCode(), quantiy);

		return updatedTradeStatus;
	}

	@Transactional
	protected boolean processTradeUpdate(Transaction transaction) throws UnexpectedTransaction {

		Optional<TradeStatus> trade = tradeStatusRepository.findById(transaction.getTradeId());
		if (!trade.isPresent()) {
			return false;
		}

		TradeStatus tradeStatus = trade.get();
		if (tradeStatus.getLastTradeVersion() > transaction.getVersion()) {
			// if trade version in current transaction fall behind the processed trade
			// version, just skip and treat this transaction as processed.
			return true;
		}

		if (TRADE_ACTION_CANCEL.equals(tradeStatus.getLastTradeActionType())) {
			// if an UPDATE action was applied on an CANCELLED trade, skip and treat it as
			// processed.
			String msg = "And UPDATE transaction attempt to update an CANCELLED trade." + transaction.toString();
			logger.warn(msg);
			return true;
		}

		Integer quantityOld = tradeStatus.getQuantity();
		Integer quantityNew = transaction.getQuantity();
		if (TRANSACTION_TYPE_BUY.equals(transaction.getTransactionType())) {
			tradeStatus.setQuantity(quantityNew);
		} else if (TRANSACTION_TYPE_SELL.equals(transaction.getTransactionType())) {
			tradeStatus.setQuantity(-1 * quantityNew);
		}

		// the following three field is stored to be used in the future, for
		// non-sequential transactions handling support.
		tradeStatus.setLastTradeVersion(transaction.getVersion());
		tradeStatus.setLastTransactionId(transaction.getTransactionId());
		tradeStatus.setLastTransactionType(transaction.getTransactionType());
		tradeStatus.setLastTradeActionType(transaction.getTradeActionType());

		// transaction.setProcessed(Boolean.TRUE);
		// transactionRepository.save(transaction);
		tradeStatusRepository.save(tradeStatus);

		Integer positionChange = quantityNew - quantityOld;
		updateEquityPosition(tradeStatus.getSecurityCode(), positionChange);

		return true;
	}

	@Transactional
	protected boolean processTradeCancel(Transaction transaction) throws UnexpectedTransaction {
		Optional<TradeStatus> trade = tradeStatusRepository.findById(transaction.getTradeId());
		if (!trade.isPresent()) {
			return false;
		}

		TradeStatus tradeStatus = trade.get();
		if (tradeStatus.getLastTradeVersion() > transaction.getVersion()) {
			String msg = "Found a \"CANCEL\" transaction with trade version(${vc}) lower than processed version(${vp}): "
					.replace("${vc}", "" + transaction.getVersion())
					.replace("${vp}", "" + tradeStatus.getLastTradeVersion()) + transaction.toString();
			logger.error(msg);
			throw new UnexpectedTransaction(msg);
		}

		Integer quantityOld = tradeStatus.getQuantity();
		tradeStatus.setQuantity(0);

		tradeStatus.setLastTradeVersion(transaction.getVersion());
		tradeStatus.setLastTransactionId(transaction.getTransactionId());
		tradeStatus.setLastTransactionType(transaction.getTransactionType());
		tradeStatus.setLastTradeActionType(transaction.getTradeActionType());

		// transaction.setProcessed(Boolean.TRUE);
		// transactionRepository.save(transaction);
		tradeStatusRepository.save(tradeStatus);

		Integer positionChange = 0 - quantityOld;
		updateEquityPosition(tradeStatus.getSecurityCode(), positionChange);

		return true;
	}

	/**
	 * Update equity_postion table, this table is designed to avoid aggregate
	 * function calling every time in query equity position. This method accumulates
	 * equity position after each transaction handling.
	 * 
	 * @param equity
	 * @param quantiy
	 */
	@Transactional
	protected void updateEquityPosition(String equity, Integer quantiy) {
		Optional<EquityPosition> equityPosition = equityPositionRepository.findByEquity(equity);
		EquityPosition entity = null;
		if (equityPosition.isPresent()) {
			entity = equityPosition.get();

			entity.setPosition(entity.getPosition() + quantiy);
		} else {
			entity = new EquityPosition();
			entity.setEquity(equity);
			entity.setPosition(0L + quantiy);
		}
		equityPositionRepository.save(entity);
	}

	/**
	 * Query trade_status table, summarize and return the equity position result.
	 * 
	 * @throws UnexpectedTransaction
	 */
	@Override
	public List<EquityPosition> listEquityPositions() throws UnexpectedTransaction {
		processStoredTransaction();
		return equityPositionRepository.findAll();

		// return tradeStatusRepository.listEquityPosition();
	}

	protected void preProcess() throws UnexpectedTransaction {

	}

	protected void postProcess() throws UnexpectedTransaction {

	}

	// Just for cleaning data in JUnit Test.
	@PersistenceContext
	private EntityManager em;

	@Transactional
	public void cleanup() {
		em.createNativeQuery("delete from transaction").executeUpdate();
		em.createNativeQuery("delete from trade_status").executeUpdate();
		em.createNativeQuery("delete from equity_position").executeUpdate();
	}
}
