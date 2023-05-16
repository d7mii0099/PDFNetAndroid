package com.pdftron.actions;

public class ResponsePair {
    public int code;
    public boolean successful;
    public String body;

    public ResponsePair(int code, boolean successful, String body) {
        this.code = code;
        this.successful = successful;
        this.body = body;
    }
}
