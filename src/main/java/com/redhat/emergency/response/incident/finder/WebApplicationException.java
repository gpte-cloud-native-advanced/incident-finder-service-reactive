package com.redhat.emergency.response.incident.finder;

public class WebApplicationException extends RuntimeException {

    private final int status;

    public WebApplicationException(Throwable throwable, int status) {
        this("HTTP status code" + status, throwable, status);
    }

    public WebApplicationException(final int status) {
        this((Throwable) null, status);
    }

    public WebApplicationException(final String message, final Throwable cause, final int status) {
        super(message, cause);
        this.status = status;
    }

}
