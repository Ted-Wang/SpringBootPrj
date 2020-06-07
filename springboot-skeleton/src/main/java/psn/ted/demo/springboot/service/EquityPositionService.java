package psn.ted.demo.springboot.service;

import java.util.List;
import java.util.Map;

import psn.ted.demo.springboot.entity.EquityPosition;
import psn.ted.demo.springboot.entity.Transaction;
import psn.ted.demo.springboot.exception.UnexpectedTransaction;

public interface EquityPositionService {

	public void acceptTransaction(Transaction transaction) throws UnexpectedTransaction;

	public void processStoredTransaction() throws UnexpectedTransaction;

	public List<EquityPosition> listEquityPositions() throws UnexpectedTransaction;

}
