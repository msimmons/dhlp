package net.contrapt.dhlp.service;

import javax.swing.tree.*;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;

import net.contrapt.dhlp.common.*;

/**
* This tree node represents a database object
*/
public class ObjectNode extends DefaultMutableTreeNode {
   
   private boolean isLoaded = false;

   //
   // Constructors
   //
   public ObjectNode(JDBCObject object) {
      super(object, true);
      if ( object.getClass().equals(JDBCTable.class) ) {
         add(new DetailNode(DetailNode.Type.COLUMN));
         add(new DetailNode(DetailNode.Type.INDEX));
         add(new DetailNode(DetailNode.Type.CHILD));
         add(new DetailNode(DetailNode.Type.PARENT));
      }
      else if ( object.getClass().equals(JDBCView.class) ) {
         add(new DetailNode(DetailNode.Type.COLUMN));
      }
      else if ( object.getClass().equals(JDBCSynonym.class) ) {
      }
      else if ( object.getClass().equals(JDBCProcedure.class) ) {
         add(new DetailNode(DetailNode.Type.PARAMETER));
     }
   }

   //
   // Methods
   //

   /**
   * Return the object
   */
   public JDBCObject getObject() {
      return (JDBCObject)getUserObject();
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
