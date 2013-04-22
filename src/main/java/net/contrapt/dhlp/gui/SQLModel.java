package net.contrapt.dhlp.gui;

import java.sql.SQLException;

/**
* Define actions expected to be implemented by a SQL model
*/
public interface SQLModel {

   public void execute() throws SQLException;
   public void fetch() throws SQLException;
   public void cancel() throws SQLException;
   public void close() throws SQLException;
   public void commit() throws SQLException;
   public void rollback() throws SQLException;
   public int getRowCount();
   public String getAction();
   public String getOperation();
   public String getSql();
}
