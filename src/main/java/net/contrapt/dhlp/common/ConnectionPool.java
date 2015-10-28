package net.contrapt.dhlp.common;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import net.contrapt.dhlp.model.ConnectionData;
import net.contrapt.dhlp.model.DriverData;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Stack;


/**
 * A frame which shows one or more statement result panels for the same connection
 */
public class ConnectionPool {

    //
    // PROPERTIES
    //
    private DriverData driverData;
    private ConnectionData connectionData;
    private Stack<Connection> connections;
    private int maxConnections = 5;
    private Session sshSession;

    //
    // Constructors
    //
    public ConnectionPool(DriverData driverData, ConnectionData connectionData) {
        this.driverData = driverData;
        this.connectionData = connectionData;
        this.connections = new Stack<Connection>();
    }

    public Connection takeConnection() {
        if (connectionData.isSsh()) {
            ensureSshSession();
        }
        Connection connection = null;
        if (!connections.empty()) {
            connection = connections.pop();
        } else {
            connection = connection();
        }
        // Make sure the connection is still ok
        if (!isValid(connection)) connection = connection();
        return connection;
    }

    private void ensureSshSession() {
        if (sshSession != null && sshSession.isConnected()) return;
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        JSch jsch = new JSch();
        try {
            if (connectionData.getSshKeyFile() != null) {
                File keyFile = new File(connectionData.getSshKeyFile());
                jsch.addIdentity(keyFile.getAbsolutePath(), connectionData.getSshPassphrase());
            }
            sshSession = jsch.getSession(connectionData.getSshUser(), connectionData.getSshHost(), 22);
            sshSession.setConfig(config);
            sshSession.setPassword(connectionData.getPassword());
            sshSession.connect();
            sshSession.setPortForwardingL(connectionData.getSshLocalPort(), connectionData.getSshRemoteHost(), connectionData.getSshRemotePort());
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    public void returnConnection(Connection connection) {
        if (isValid(connection) && connections.size() < maxConnections) connections.push(connection);
    }

    public int openConnections() {
        return connections.size();
    }

    public void setMaxConnections(int connections) {
        this.maxConnections = connections;
    }

    public String getURL() {
        return connectionData.getUrl();
    }

    public String getUser() {
        return connectionData.getUser();
    }

    public List<String> getSchema() {
        return connectionData.getSchema();
    }

    public int getFetchLimit() {
        return connectionData.getFetchLimit();
    }

    public void close() {
        // Close all connections in the pool
        while (!connections.empty()) {
            Connection c = connections.pop();
            closeConnection(c);
        }
        if (sshSession != null && sshSession.isConnected()) {
            sshSession.disconnect();
            sshSession = null;
        }
    }

    /**
     * Check if the connection is valid by attempting a rollback; if not valid,
     * try to close it
     */
    private boolean isValid(Connection c) {
        boolean valid = true;
        // If null or closed, return immediately
        if (c == null) return false;
        try {
            if (c.isClosed()) return false;
        } catch (Exception e) {
            return false;
        }
        // Try rollback and close if there are errors
        try {
            c.rollback();
        } catch (Exception e) {
            valid = false;
        }
        if (!valid) {
            closeConnection(c);
        }
        return valid;
    }

    /**
     * Create a new connection
     */
    private Connection connection() {
        Connection c = null;
        try {
            Class.forName(driverData.getClassName());
            c = DriverManager.getConnection(connectionData.getUrl(), connectionData.getUser(), connectionData.getPassword());
            c.setAutoCommit(connectionData.isAutocommit());
        } catch (Exception e) {
            c = null;
        }
        return c;
    }

    /**
     * Try and close a connection; do it in a new thread to avoid hanging on
     * invalid or f@#$(*&ed connections
     */
    private void closeConnection(final Connection c) {
        new Thread("closeConnection:" + c) {
            public void run() {
                try {
                    c.close();
                } catch (Exception e) {
                    System.err.println("Trying to close connection: " + e);
                }
            }
        }.start();
    }
}
