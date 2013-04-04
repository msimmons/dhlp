package net.contrapt.dhlp.gui;

import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.contrapt.dhlp.common.*;

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

   public QueryPlanPanel(DHLPConnectionPool pool, String sql) {
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
