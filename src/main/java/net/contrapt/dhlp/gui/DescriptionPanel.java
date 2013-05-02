package net.contrapt.dhlp.gui;

import javax.swing.*;

import net.contrapt.dhlp.common.*;

/**
* A panel which shows an object description
*/
public class DescriptionPanel extends SQLPanel {
   
   //
   // PROPERTIES
   //
   private DescriptionTreeModel model;
   private JDBCObject object;
   
   //
   // CONSTRUCTORS
   //

   public DescriptionPanel(ConnectionPool pool, JDBCObject object) {
      this.object = object;
      model = new DescriptionTreeModel(pool, object);
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
