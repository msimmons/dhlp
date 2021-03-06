package net.contrapt.dhlp.gui;

import net.contrapt.dhlp.common.ConnectionPool;

import javax.swing.tree.*;
import java.io.BufferedWriter;
import java.sql.*;
import java.util.*;
import javax.swing.*;

/**
* Execute a query plan and display as a tree
*/
public class QueryPlanTreeModel extends DefaultTreeModel implements SQLModel {

   private String sql;
   private ConnectionPool pool;
   private JTree tree;

   /**
   * Constructor to use connection pool
   */
   public QueryPlanTreeModel(ConnectionPool pool, String sql) {
      super(null);
      initialize();
      this.sql = sql;
      this.pool = pool;
   }

   /**
   * Return the tree component
   */
   public JTree getTree() { return tree; }

   public void execute() {
      explain();
   }

   public void fetch(boolean limited) {}
   public void cancel() {}
   public void close() {}
   public void commit() {}
   public void rollback() {}
   public void export(BufferedWriter out) {}

   public int getRowCount() { return 1; }
   public String getAction() { return "query plan generated"; }
   public String getOperation() { return "generating query plan"; }

   @Override
   public String getSql() {
      return sql;
   }

   /**
   * Initialize members
   */
   private void initialize() {
      tree = new JTree(this);
   }

   /**
   * Run the query plan and create the tree model
   */
   private void explain() {
      String explainString = "explain plan set statement_id='dhlp' into plan_table for "+sql;
      String planString = "select * from plan_table order by id, position";
      List<QueryPlanNode> nodes = new ArrayList<QueryPlanNode>();
      Connection db = null;
      try {
         db = pool.takeConnection();
         // Generate the query plan
         PreparedStatement explain = db.prepareStatement(explainString);
         explain.executeUpdate();
         explain.close();
         // Get the plan results
         PreparedStatement plan = db.prepareStatement(planString);
         ResultSet rows = plan.executeQuery();
         while ( rows.next() ) {
            int id = rows.getInt("ID");
            int parentId = rows.getInt("PARENT_ID");
            String operation = rows.getString("OPERATION");
            String options = rows.getString("OPTIONS");
            String objectName = rows.getString("OBJECT_NAME");
            String objectType = rows.getString("OBJECT_TYPE");
            long cost = rows.getLong("COST");
            long bytes = rows.getLong("BYTES");
            QueryPlanNode node = new QueryPlanNode(operation, options, objectName, objectType, cost, bytes);
            nodes.add(node);
            if ( id == parentId ) setRoot(node);
            else {
               QueryPlanNode parent = nodes.get(parentId);
               parent.add(node);
            }
         }
         plan.close();
      }
      catch (SQLException e) {
         throw new IllegalStateException("Error explain execution plan for "+sql);
      }
      finally {
         pool.returnConnection(db);
      }

   }

   /**
   * A node in the query plan tree
   */
   private class QueryPlanNode extends DefaultMutableTreeNode {

      private String operation;
      private String options;
      private String objectName;
      private String objectType;
      private long cost;
      private long bytes;

      /**
      * This would be the root node
      */
      QueryPlanNode() {
         super(true);
      }

      /**
      * Construct a new query plan node with the given attributes
      */
      QueryPlanNode(String operation, String options, String objectName, String objectType, long cost, long bytes) {
         this.operation = operation;
         this.options = options;
         this.objectName = objectName;
         this.objectType = objectType;
         this.cost = cost;
         this.bytes = bytes;
      }

      @Override
      public String toString() {
         return operation + " " +
            ((options==null) ? " " : options + " ") +
            ((objectName==null) ? " " : " ON " + objectType + " " + objectName + " ") +
            " (Cost=" + cost + "; Bytes=" + bytes + ")";
      }

   }

}
