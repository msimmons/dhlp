package net.contrapt.dhlp.gui;

import javax.swing.tree.*;
import java.sql.*;
import java.util.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;
import net.contrapt.dhlp.common.*;

/**
* Execute a sql statement and model the result set as a table.
*/
public class DescriptionTreeModel extends DefaultTreeModel implements SQLModel, TreeWillExpandListener {
   
   private JDBCObject object;
   private DHLPConnectionPool pool;
   private JTree tree;

   /**
   * Constructor to use connection pool
   */
   public DescriptionTreeModel(DHLPConnectionPool pool, JDBCObject object) {
      super(new DefaultMutableTreeNode(object, true));
      initialize();
      this.object = object;
      this.pool = pool;
   }
   
   /**
   * Excecute the sql statement for this table model
   */
   public void execute() throws SQLException {
      if ( object == null ) return;
      describe();
   }
   
   /**
   * Fetch the rows from the result set
   */
   public void fetch() throws SQLException {
      // This is a no-op
   }
   
   /**
   * Cancel the current statement if possible
   */
   public void cancel() {
   }

   /**
   * Commit the current connection
   */
   public void commit() throws SQLException {
      // noop
   }

   /**
   * Rollback the current connection
   */
   public void rollback() throws SQLException {
      // noop
   }

   public void export() {
   }

   /**
   * Close resources used by this model
   */
   public void close() throws SQLException {
      cancel();
   }
   
   public int getRowCount() {
      return 1;
   }
   
   /**
   * Return the tree component
   */
   public JTree getTree() { return tree; }
   
   /**
   * Return a string describing whether rows were selected or affected by DML
   */
   public String getAction() {
      return "object described";
   }

   /**
   * Return a string describing the operation for error messages
   */
   public String getOperation() {
      return "Describing "+object;
   }

   @Override
   public String getSql() {
      return object.getName();
   }

   //
   // Implement tree will expand to have a dynamic tree
   //
   public void treeWillCollapse(TreeExpansionEvent e) throws ExpandVetoException {
   }

   public void treeWillExpand(TreeExpansionEvent e) throws ExpandVetoException {
      Object node = e.getPath().getLastPathComponent();
   }

   /**
   * Initialize members
   */
   private void initialize() {
      tree = new JTree(this);
   }
   
   /**
   * Get the description for the object and populate the tree model
   */
   private void describe() throws SQLException {
      if ( object.isDescribed() ) return;
      if ( object instanceof JDBCTable ) {
         describeTable((JDBCTable)object);
         createTree((JDBCTable)object);
      }
      if ( object instanceof JDBCProcedure ) {
         describeProcedure((JDBCProcedure)object);
         createTree((JDBCProcedure)object);
      }
   }

   /**
   * Display table description in the tree
   */
   private void createTree(JDBCTable table) {
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)getRoot();
      root.removeAllChildren();
      DefaultMutableTreeNode columns = new DefaultMutableTreeNode("Columns", true);
      DefaultMutableTreeNode key = new DefaultMutableTreeNode("Primary Key", true);
      DefaultMutableTreeNode indices = new DefaultMutableTreeNode("Indices", true);
      DefaultMutableTreeNode parents = new DefaultMutableTreeNode("Parents", true);
      DefaultMutableTreeNode children = new DefaultMutableTreeNode("Children", true);
      for ( JDBCTable.Column c : table.getColumns() ) columns.add(new DefaultMutableTreeNode(c));
      key.add(new DefaultMutableTreeNode(table.getPrimaryKey()));
      for ( JDBCTable.Index i : table.getIndices() ) indices.add(new DefaultMutableTreeNode(i));
      for ( JDBCTable.Constraint c : table.getChildren() ) children.add(new DefaultMutableTreeNode(c));
      for ( JDBCTable.Constraint c : table.getParents() ) parents.add(new DefaultMutableTreeNode(c));
      root.add(columns);
      root.add(key);
      root.add(indices);
      root.add(children);
      root.add(parents);
   }

   /**
   * Display the procedure description as a tree
   */
   private void createTree(JDBCProcedure procedure) {
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)getRoot();
      root.removeAllChildren();
      DefaultMutableTreeNode parameters = new DefaultMutableTreeNode("Parameters", true);
      for ( JDBCProcedure.Column c : procedure.getColumns() ) parameters.add(new DefaultMutableTreeNode(c));
      root.add(parameters);
   }

   /**
   * Describe a table
   */
   private void describeTable(JDBCTable table) throws SQLException {
      Connection db = null;
      try {
         db = pool.takeConnection();
         ResultSet results = db.getMetaData().getColumns(table.getCatalog(), table.getSchema(), table.getName(), "%");
         // Columns
         table.addColumns(results);
         results.close();
         // If its not a table, return here
         if ( !table.getType().contains("TABLE") ) return;
         // Primary key columns
         results = db.getMetaData().getPrimaryKeys(table.getCatalog(), table.getSchema(), table.getName());
         table.addPrimaryKey(results);
         results.close();
         // Indexes
         try { 
            results = db.getMetaData().getIndexInfo(table.getCatalog(), table.getSchema(), table.getName(), false, false);
            table.addIndices(results);
            results.close();
         }
         catch (SQLException e) {
            System.out.println("Error getting indices for "+table+": "+e);
         }
         // Child constraints
         results = db.getMetaData().getExportedKeys(table.getCatalog(), table.getSchema(), table.getName());
         table.addChildren(results);
         results.close();
         // Parent constraints
         results = db.getMetaData().getImportedKeys(table.getCatalog(), table.getSchema(), table.getName());
         table.addParents(results);
         results.close();
         // That's it
         table.setDescribed();
      }
      finally {
         pool.returnConnection(db);
      }
   }

   /**
   * Describe a procedure
   */
   private void describeProcedure(JDBCProcedure procedure) throws SQLException {
      Connection db = null;
      try {
         db = pool.takeConnection();
         ResultSet results = db.getMetaData().getProcedureColumns(procedure.getCatalog(), procedure.getSchema(), procedure.getName(), "%");
         // Columns
         procedure.addColumns(results);
         results.close();
         // That's it
         procedure.setDescribed();
      }
      finally {
         pool.returnConnection(db);
      }
   }

}
