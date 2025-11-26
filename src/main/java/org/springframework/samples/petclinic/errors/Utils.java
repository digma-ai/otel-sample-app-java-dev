package org.springframework.samples.petclinic.errors;

import org.apache.coyote.BadRequestException;public class Utils {
	public static void ThrowIllegalStateException() throws IllegalStateException {
		String env = System.getProperty("env", "unknown");
		throw new IllegalStateException(String.format("[%s] Application state error: System is in an invalid state. Recovery suggestion: Please check system configuration and restart the application", env));
	}

	public static void ThrowBadRequestException() throws BadRequestException {
		String env = System.getProperty("env", "unknown");
		throw new BadRequestException(String.format("[%s] Validation Error: Request parameters are invalid. Recovery suggestion: Please verify input parameters and try again", env));
	}

	public static void ThrowUnsupportedOperationException() throws UnsupportedOperationException {
		String env = System.getProperty("env", "unknown");
		throw new UnsupportedOperationException(String.format("[%s] Unsupported Operation: This operation is not supported in the current context. Recovery suggestion: Please check API documentation for supported operations", env));
	}
public static <T extends Exception> void throwException(Class<T> exceptionClass, String message) throws T {
		if (exceptionClass == null) {
			throw new IllegalArgumentException("Exception class cannot be null. Please provide a valid exception class.");
		}
		if (message == null) {
			message = "No message provided";
		}

		// Add environment information to the message
		String enhancedMessage = String.format("[Env: %s] %s\nRecovery: Please check the input parameters and system configuration.", 
			System.getProperty("env.name", "unknown"), 
			message);

		try {
			// Use reflection to create a new instance of the exception with the message
			T exceptionInstance = exceptionClass.getConstructor(String.class).newInstance(enhancedMessage);
			throw exceptionInstance;
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Exception type must have a constructor that accepts a String message. Recovery: Use an exception class with proper constructor.", e);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("Failed to create exception instance. Recovery: Verify exception class accessibility and permissions.", e);
		}
	}
}