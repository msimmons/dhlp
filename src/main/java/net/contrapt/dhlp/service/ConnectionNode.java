package net.contrapt.dhlp.service;

import javax.swing.tree.*;

/**
* This tree node represents a database connection
*/
public class ConnectionNode extends DefaultMutableTreeNode {
   
   private boolean loaded = false;

   //
   // Constructors
   //
   public ConnectionNode(String name) {
      super(name);
      add(new DefaultMutableTreeNode("Not Loaded"));
   }

   //
   // Methods
   //

   /**
   * Return the connection name
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
