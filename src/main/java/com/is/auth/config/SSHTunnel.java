package com.is.auth.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

public class SSHTunnel {
    private static Session session;
    private static final String sshHost = "placeandplay.uz";
    private static final String sshUser = "placeand";
    private static final String sshPassword = "bwq6sjVvQwsYhhrqLs";
    private static final int sshPort = 22;
    private static final String localHost = "localhost";
    private static final int localPort = 5434;
    private static final String remoteHost = "localhost";
    private static final int remotePort = 5432;

    public static void createSSHTunnel() {
        if (session != null && session.isConnected()) {
            System.out.println("SSH tunnel already established.");
            return;
        }
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();
            int assignedPort = session.setPortForwardingL(localPort, remoteHost, remotePort);
            System.out.println("Tunnel established. Local port: " + assignedPort);

            waitForPort(localPort, 15000); // Ждём до 15 секунд, пока порт не станет доступен
            System.out.println("Local port " + localPort + " is now available for DB connections.");
        } catch (Exception e) {
            System.err.println("Failed to establish SSH tunnel: " + e.getMessage());
            throw new RuntimeException("Failed to establish SSH tunnel", e);
        }
    }

    private static void waitForPort(int port, int timeoutMillis) throws InterruptedException, IOException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            try (Socket socket = new Socket(localHost, port)) {
                return; // порт доступен
            } catch (IOException e) {
                Thread.sleep(300);
            }
        }
        throw new IOException("Port " + port + " not available after " + timeoutMillis + "ms");
    }

    public static void closeTunnel() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            System.out.println("SSH tunnel closed.");
        }
    }
}