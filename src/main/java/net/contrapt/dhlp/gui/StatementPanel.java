package net.contrapt.dhlp.gui;

import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.contrapt.dhlp.common.*;

/**
* A panel which shows results from a sql statement in a table, the text of the sql statement in a text area and
* status information.
* @@ Allow for fetching limited number of rows
* @@ Support prepared statement parameters and parameter metadata (1.4)
*/
public class StatementPanel extends SQLPanel {
   
   private StatementResultTableModel model;
   private String sql;
   
   public StatementPanel(DHLPConnectionPool pool, String sql) {
      this.sql = sql;
      model = new StatementResultTableModel(pool, sql);
      initialize();
   }

   public void reset(DHLPConnectionPool pool, String sql) {
      this.sql = sql;
      model = new StatementResultTableModel(pool, sql);
      reinit();
   }
   
   @Override
   public SQLModel getModel() {
      return model;
   }

   @Override
   public JComponent getComponent() {
      return model.getTable();
   }
   
}
