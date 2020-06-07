package psn.ted.demo.springboot.exception;

public class UnexpectedTransaction extends Exception {

	private static final long serialVersionUID = 8679145629501959757L;

	public UnexpectedTransaction() {
		super();
	}

	public UnexpectedTransaction(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UnexpectedTransaction(String message, Throwable cause) {
		super(message, cause);
	}

	public UnexpectedTransaction(String message) {
		super(message);
	}

	public UnexpectedTransaction(Throwable cause) {
		super(cause);
	}
}
