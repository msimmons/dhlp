package net.contrapt.dhlp.gui;

import java.io.BufferedWriter;
import java.sql.SQLException;

/**
* Define actions expected to be implemented by a SQL model
*/
public interface SQLModel {

   public void execute();
   public void fetch();
   public void cancel();
   public void close();
   public void commit();
   public void rollback();
   public void export(BufferedWriter out);
   public int getRowCount();
   public String getAction();
   public String getOperation();
   public String getSql();
}
