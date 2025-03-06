package com.is.auth.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.util.Properties;

public class SSHTunnel {
    public static void createSSHTunnel() {
        String sshHost = "placeandplay.uz";
        String sshUser = "placeand";
        String sshPassword = "bwq6sjVvQwsYhhrqLs";
        int sshPort = 22;

        String localHost = "localhost";
        int localPort = 5433; // Изменен порт на 5433
        String remoteHost = "localhost";
        int remotePort = 5432;

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            int assignedPort = session.setPortForwardingL(localPort, remoteHost, remotePort);
            System.out.println("Tunnel established. Local port: " + assignedPort);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}