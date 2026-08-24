package com.sunrise.helpclass.exception;

import lombok.Getter;

@Getter
public class MyException extends RuntimeException {

    private final MyErrorCode code;

    public MyException(MyErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }

    public MyException(MyErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public MyException(MyErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}