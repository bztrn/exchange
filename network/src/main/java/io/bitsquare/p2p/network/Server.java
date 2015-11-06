package io.bitsquare.p2p.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

public class Server implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final ServerSocket serverSocket;
    private final MessageListener messageListener;
    private final ConnectionListener connectionListener;
    private final Set<Connection> connections = new HashSet<>();
    private volatile boolean stopped;


    public Server(ServerSocket serverSocket, MessageListener messageListener, ConnectionListener connectionListener) {
        this.serverSocket = serverSocket;
        this.messageListener = messageListener;
        this.connectionListener = connectionListener;
    }

    @Override
    public void run() {
        try {
            // Thread created by NetworkNode
            Thread.currentThread().setName("NetworkNode:Server-" + serverSocket.getLocalPort());
            try {
                while (!stopped && !Thread.currentThread().isInterrupted()) {
                    log.info("Ready to accept new clients on port " + serverSocket.getLocalPort());
                    final Socket socket = serverSocket.accept();
                    if (!stopped) {
                        log.info("Accepted new client on localPort/port " + socket.getLocalPort() + "/" + socket.getPort());
                        Connection connection = new Connection(socket, messageListener, connectionListener);

                        StringBuilder result = new StringBuilder("\n\n############################################################\n" +
                                "Server created new inbound connection:"
                                + "\nlocalPort/port=" + serverSocket.getLocalPort()
                                + "/" + socket.getPort()
                                + "\nconnection.uid=" + connection.getUid());
                        result.append("\n############################################################\n");
                        log.info(result.toString());

                        if (!stopped)
                            connections.add(connection);
                    }
                }
            } catch (IOException e) {
                if (!stopped)
                    e.printStackTrace();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            log.error("Executing task failed. " + t.getMessage());
        }
    }

    public void shutDown() {
        if (!stopped) {
            stopped = true;

            connections.stream().forEach(e -> e.shutDown());

            try {
                serverSocket.close();
            } catch (SocketException e) {
                log.warn("SocketException at shutdown might be expected " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Exception at shutdown. " + e.getMessage());
            } finally {
                log.info("Server shutdown complete");
            }
        }
    }
}
