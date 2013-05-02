package net.contrapt.dhlp.gui;

import net.contrapt.dhlp.common.ConnectionPool;

import javax.swing.*;

/**
* A panel which shows an object description
*/
public class QueryPlanPanel extends SQLPanel {
   
   //
   // PROPERTIES
   //
   private QueryPlanTreeModel model;
   private String sql;
   
   //
   // CONSTRUCTORS
   //

   public QueryPlanPanel(ConnectionPool pool, String sql) {
      this.sql = sql;
      model = new QueryPlanTreeModel(pool, sql);
      initialize();
   }
   
   //
   // OVERRIDES
   //
   
   //
   // PUBLIC METHODS
   //

   @Override
   public SQLModel getModel() {
      return model;
   }

   @Override
   public JComponent getComponent() {
      return model.getTree();
   }
   
}
