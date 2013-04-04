package net.contrapt.dhlp.service;

import javax.swing.tree.*;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;

import net.contrapt.dhlp.common.*;

/**
* This node represents details about a database object
*/
public class DetailNode extends DefaultMutableTreeNode {
   
   public static enum Type {
      COLUMN("Columns"),
      INDEX("Indices"),
      CHILD("Children"),
      PARENT("Parents"),
      PARAMETER("Parameters");
      
      protected String label;
      
      Type(String label) {
         this.label = label;
      }
   }

   private boolean isLoaded = false;
   private Type type;

   //
   // Constructors
   //
   public DetailNode(Type type) {
      super(type.label, true);
      this.type = type;
      add(new DefaultMutableTreeNode("?"));
   }

   //
   // Methods
   //

   /**
   * Return the object
   */
   public String getName() {
      return (String)getUserObject();
   }
   
   /**
   * Return the type
   */
   public Type getType() {
      return type;
   }

   /**
   * Set whether this node has been opened
   */
   public void setLoaded() {
      this.isLoaded = true;
   }

   /**
   * Has this node been opened already?
   */
   public boolean isLoaded() { return this.isLoaded; }

}
