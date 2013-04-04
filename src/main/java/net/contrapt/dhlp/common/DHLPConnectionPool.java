package net.contrapt.dhlp.common;

import java.sql.*;

/**
* Manage a pool of connections; allow client to take and give back a connection
*/
public interface DHLPConnectionPool {

   //
   // PUBLIC METHODS
   //

   /**
   * Take a connection
   */
   public Connection takeConnection();

   /**
   * Return a connection
   */
   public void returnConnection(Connection connection);
   
   /**
   * How many open connections?
   */
   public int openConnections();
   
   /**
   * Close all the connections in the pool
   */
   public void close();
   
   /**
   * Return the user name
   */
   public String getUser();
   
   /**
   * Return the db url
   */
   public String getURL();

}
