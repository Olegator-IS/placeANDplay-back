package com.is.auth.model.logger;

public class Api {
    private String method;
    private String url;
    private String requestId;

    // Геттеры и сеттеры
    public Api() {

    }

    public Api(String method, String url, String requestId) {
        this.method = method;
        this.url = url;
        this.requestId = requestId;
    }



    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return String.format("Api(method=%s, url=%s, requestId=%s)", method, url, requestId);
    }
}