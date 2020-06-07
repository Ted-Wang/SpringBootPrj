package psn.ted.demo.springboot.rest;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import psn.ted.demo.springboot.entity.EquityPosition;
import psn.ted.demo.springboot.entity.Transaction;
import psn.ted.demo.springboot.exception.UnexpectedTransaction;
import psn.ted.demo.springboot.service.EquityPositionService;

@RestController
public class PositionController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private EquityPositionService service;

	private final AtomicLong counter = new AtomicLong();

	@GetMapping("/position")
	public @ResponseBody EquityPostionREST showCurrentPosition() {
		EquityPostionREST resp = new EquityPostionREST();
		resp.setId(counter.incrementAndGet());

		try {
			List<EquityPosition> resultList = service.listEquityPositions();
			resp.setEquityPositionList(resultList);
			return resp;
		} catch (UnexpectedTransaction e) {
			String msg = "Error occured while getting EquityPosition.";
			logger.error(msg + e);
			resp.setErrorMsg(msg);
		}

		return resp;
	}

	@PostMapping("/transaction")
	public @ResponseBody TransactionAcceptionREST showCurrentPosition(@RequestBody TransactionDO transaction) {
		TransactionAcceptionREST resp = new TransactionAcceptionREST();
		resp.setId(counter.incrementAndGet());

		try {
			Transaction entity = new Transaction();
			BeanUtils.copyProperties(transaction, entity);
			service.acceptTransaction(entity);
			resp.setRespCode("0");
			resp.setRespMsg("Success. Transaction(id=${id}) accepted".replace("${id}", "" + entity.getTransactionId()));
			return resp;
		} catch (UnexpectedTransaction e) {
			String msg = "Error occured while processing transaction with id=${id}".replace("${id}",
					"" + transaction.getTransactionId());
			resp.setRespCode("2");
			resp.setRespMsg("Failure. " + msg);
			logger.error(msg + e);
			return resp;
		}
	}

	@Deprecated
	private void prepareInitDataForRest() {
		Transaction trans1 = new Transaction(1L, 1L, 1L, "REL", 50, "INSERT", "Buy");
		Transaction trans2 = new Transaction(2L, 2L, 1L, "ITC", 40, "INSERT", "Sell");

		try {
			service.acceptTransaction(trans1);
			service.acceptTransaction(trans2);
			// service.processStoredTransaction();
		} catch (UnexpectedTransaction e) {
			e.printStackTrace();
		}
	}
}
