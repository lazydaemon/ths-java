package ths.commons.exception;

/**
 * 代表不可能到达的代码的异常。
 */
public class UnreachableCodeException extends RuntimeException {
	
	private static final long serialVersionUID = -2315680326190592010L;

	public UnreachableCodeException() {
		super();
	}

	public UnreachableCodeException(String message) {
		super(message);
	}

	public UnreachableCodeException(Throwable cause) {
		super(cause);
	}

	public UnreachableCodeException(String message, Throwable cause) {
		super(message, cause);
	}
}
