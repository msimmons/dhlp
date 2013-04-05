package net.contrapt.dhlp.service;

import javax.swing.tree.DefaultMutableTreeNode;

/**
* This tree node represents a database driver
*/
public class DriverNode extends DefaultMutableTreeNode {

   private boolean loaded = false;

   //
   // Constructors
   //
   public DriverNode(String name) {
      super(name);
      add(new DefaultMutableTreeNode("Not Loaded"));
   }

   //
   // Methods
   //

   /**
   * Return the driver name
   */
   public String getName() {
      return (String)getUserObject();
   }

   /**
   * Mark this connection node as loaded
   */
   public void setLoaded() {
      loaded = true;
   }

   /**
   * Is this node loaded?
   */
   public boolean isLoaded() {
      return loaded;
   }
}
