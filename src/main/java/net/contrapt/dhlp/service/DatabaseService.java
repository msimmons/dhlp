package net.contrapt.dhlp.service;

import javax.swing.tree.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;
import java.util.Map;

import org.gjt.sp.jedit.PluginJAR;

import net.contrapt.dhlp.common.*;
import net.contrapt.dhlp.jedit.DHLPlugin;

import net.contrapt.jeditutil.InfoTreeService;

/**
* Supply a tree model implementing the PHLP tab service for displaying database information
* in a standard dockable
*/
public class DatabaseService extends DefaultTreeModel implements InfoTreeService, TreeWillExpandListener {

   private DefaultMutableTreeNode drivers;
   private DefaultMutableTreeNode connections;
   private boolean initialized = false;

   //
   // Constructor
   //
   public DatabaseService() {
      super(new DefaultMutableTreeNode());
      drivers = new DefaultMutableTreeNode("Drivers", true);
      connections = new DefaultMutableTreeNode("Connections", true);
   }

   /**
   * Returns a name for the tab containing your tree
   */
   public String getTabName() {
      return "Database";
   }

   /**
   * Return the tree model for your tree
   */
   public TreeModel getTreeModel() {
      return this;
   }

   /**
   * Return a key listener for your tree
   */
   public KeyListener getKeyListener() {
      return null;
   }

   /**
   * Return a mouse listener for your tree
   */
   public MouseListener getMouseListener() {
      return null;
   }

   /**
   * Tree will expand listener
   */
   public TreeWillExpandListener getTreeWillExpandListener() {
      return this;
   }

   /**
   * Return the plugin jar for identification
   */
   public PluginJAR getPluginJAR() {
      return DHLPlugin.getInstance().getPluginJAR();
   }

   /**
   * Initialize the tree model
   */
   public void init() {
      if ( initialized ) return;
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)getRoot();
      node.removeAllChildren();
      // Drivers
      node.add(drivers);
      // Connections
      for ( String name : DHLPController.getInstance().getConnections().keySet() ) {
         connections.add(new ConnectionNode(name));
      }
      node.add(connections);
      reload(node);
      initialized = true;;
   }

   /**
   * Close the service releasing resources
   */
   public void close() {
      drivers.removeAllChildren();
      connections.removeAllChildren();
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)getRoot();
      reload(node);
      initialized = false;
   }

   /**
   * Support reinitializing the service
   */
   public void reinit() {
      init();
   }

   //
   // Implement tree will expand to have a dynamic tree
   //
   public void treeWillCollapse(TreeExpansionEvent e) throws ExpandVetoException {
   }

   public void treeWillExpand(TreeExpansionEvent e) throws ExpandVetoException {
      Object node = e.getPath().getLastPathComponent();
      if ( node instanceof ConnectionNode ) openConnection((ConnectionNode)node);
      if ( node instanceof DetailNode ) openDetail((DetailNode)node);
   }

   //
   // Private implementation
   //

   /**
   * Open a connection node by populating the tree with list of database objects
   */
   private void openConnection(ConnectionNode node) {
      if ( node.isLoaded() ) return;
      node.removeAllChildren();
      DefaultMutableTreeNode tables = new DefaultMutableTreeNode("Tables");
      DefaultMutableTreeNode views = new DefaultMutableTreeNode("Views");
      DefaultMutableTreeNode synonyms = new DefaultMutableTreeNode("Synonyms");
      DefaultMutableTreeNode procedures = new DefaultMutableTreeNode("Procedures");
      Map<String, JDBCObject> objects = DHLPController.getInstance().findObjects(node.getName(), false);
      for ( JDBCObject o : objects.values() ) {
         if ( o.getClass().equals(JDBCTable.class) ) tables.add(new ObjectNode(o));
         if ( o.getClass().equals(JDBCView.class) ) views.add(new ObjectNode(o));
         if ( o.getClass().equals(JDBCSynonym.class) ) synonyms.add(new ObjectNode(o));
         if ( o.getClass().equals(JDBCProcedure.class) ) procedures.add(new ObjectNode(o));
      }
      node.add(tables);
      node.add(views);
      node.add(synonyms);
      node.add(procedures);
      reload(node);
      node.setLoaded();
   }

   /**
   * Open a detail tree node
   */
   private void openDetail(DetailNode node) {
      if ( node.isLoaded() ) return;
      ObjectNode parent = (ObjectNode)node.getParent();
      String connection = ((ConnectionNode)parent.getParent().getParent()).getName();
      switch ( node.getType() ) {
         case COLUMN:
            loadColumns(connection, (JDBCView)parent.getObject(), node);
            break;
         case INDEX:
            loadIndices(connection, (JDBCTable)parent.getObject(), node);
            break;
         case CHILD:
            loadChildren(connection, (JDBCTable)parent.getObject(), node);
            break;
         case PARENT:
            loadParents(connection, (JDBCTable)parent.getObject(), node);
            break;
         case PARAMETER:
            loadParameters(connection, (JDBCProcedure)parent.getObject(), node);
            break;
      }
      reload(node);
      node.setLoaded();
   }

   private void loadColumns(String connection, JDBCView view, DetailNode detail) {
      DHLPController.getInstance().describeColumns(connection, view);
      detail.removeAllChildren();
      if ( view instanceof JDBCTable ) {
         DHLPController.getInstance().describePrimaryKey(connection, (JDBCTable)view);
         detail.add(new DefaultMutableTreeNode(((JDBCTable)view).getPrimaryKey()));
      }
      for ( JDBCView.Column column : view.getColumns() ) {
         detail.add(new DefaultMutableTreeNode(column, false));
      }
   }

   private void loadPrimaryKey(String connection, JDBCTable table) {
      DHLPController.getInstance().describePrimaryKey(connection, table);
   }

   private void loadIndices(String connection, JDBCTable table, DetailNode detail) {
      DHLPController.getInstance().describeIndices(connection, table);
      detail.removeAllChildren();
      for ( JDBCTable.Index index : table.getIndices() ) {
         detail.add(new DefaultMutableTreeNode(index, false));
      }
   }

   private void loadChildren(String connection, JDBCTable table, DetailNode detail) {
      DHLPController.getInstance().describeChildren(connection, table);
      detail.removeAllChildren();
      for ( JDBCTable.Constraint child : table.getChildren() ) {
         detail.add(new DefaultMutableTreeNode(child, false));
      }
   }

   private void loadParents(String connection, JDBCTable table, DetailNode detail) {
      DHLPController.getInstance().describeParents(connection, table);
      detail.removeAllChildren();
      for ( JDBCTable.Constraint parent : table.getParents() ) {
         detail.add(new DefaultMutableTreeNode(parent, false));
      }
   }

   private void loadParameters(String connection, JDBCProcedure procedure, DetailNode detail) {
      DHLPController.getInstance().describeParameters(connection, procedure);
      detail.removeAllChildren();
      for ( JDBCProcedure.Column parameter : procedure.getColumns() ) {
         detail.add(new DefaultMutableTreeNode(parameter, false));
      }
   }

}
