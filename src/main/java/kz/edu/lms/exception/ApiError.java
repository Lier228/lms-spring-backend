package kz.edu.lms.exception;

public record ApiError(int status, String error, String message) {
}
