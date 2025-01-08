package com.is.rbs.model.logger;

public class Client {
    private String clientName;
    private String clientIp;
    private String clientHost;

    // Геттеры и сеттеры


    public Client(String clientName, String clientIp, String clientHost) {
        this.clientName = clientName;
        this.clientIp = clientIp;
        this.clientHost = clientHost;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getClientHost() {
        return clientHost;
    }

    public void setClientHost(String clientHost) {
        this.clientHost = clientHost;
    }

    @Override
    public String toString() {
        return String.format("Client(clientName=%s, clientIp=%s, clientHost=%s)", clientName, clientIp, clientHost);
    }
}
