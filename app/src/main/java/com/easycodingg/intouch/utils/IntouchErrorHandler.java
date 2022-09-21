package com.easycodingg.intouch.utils;

public class IntouchErrorHandler extends Exception {
    private static final String NO_INTERNET_MESSAGE = "Not Connected to Internet";

    private IntouchErrorHandler(String message) {
        super(message);
    }

    public static IntouchErrorHandler getNoInternetException() {
        return new IntouchErrorHandler(NO_INTERNET_MESSAGE);
    }
}
