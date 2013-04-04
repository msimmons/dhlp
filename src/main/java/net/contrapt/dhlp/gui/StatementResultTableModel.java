package net.contrapt.dhlp.gui;

import java.lang.Math;
import javax.swing.table.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.table.*;

import net.contrapt.dhlp.common.*;

/**
* Execute a sql statement and model the result set as a table.
*/
public class StatementResultTableModel extends AbstractTableModel implements SQLModel {
   
   //
   // PROPERTIES
   //
   private String sql;
   private Connection connection;
   private DHLPConnectionPool pool;
   private PreparedStatement statement;
   private ResultSet results;
   private ArrayList<Object[]> rows;
   private int updateCount = -1;
   private boolean executing = false;
   private boolean fetching = false;
   private int fetchBatchSize = 500;
   private int fetchSleepTime = 1000;
   private JTable table;
   private TableColumnModel columnModel;

   private static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
   private static SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

   //
   // CONSTRUCTORS
   //
   
   /**
   * Default constructor
   */
   public StatementResultTableModel() {
      initialize();
   }
   
   /**
   * Constructor to use connection pool
   */
   public StatementResultTableModel(DHLPConnectionPool pool, String sql) {
      initialize();
      this.sql = sql;
      this.pool = pool;
   }
   
   //
   // PUBLIC METHODS
   //
   
   /**
   * Set the connection
   */
   public void setConnection(Connection connection) { this.connection = connection; }
   
   /**
   * Set the sql statement
   */
   public void setSql(String sql) { this.sql = sql; }

   /**
   * Set the connection pool
   */
   public void setPool(DHLPConnectionPool pool) { this.pool = pool; }
   
   /**
   * Excecute the sql statement for this table model
   */
   public void execute() throws SQLException {
      if ( sql == null ) return;
      if ( connection == null ) connection = pool.takeConnection();
      if ( statement == null ) statement = connection.prepareStatement(sql);
      if ( rows == null ) rows = new ArrayList<Object[]>();
      else {
         rows.clear();
         updateCount = -1;
         fireTableDataChanged();
      }
      executing = true;
      statement.execute();
      updateCount = statement.getUpdateCount();
      if ( results != null ) results.close();
      results = statement.getResultSet();
      executing = false;
      if ( table.getColumnModel().getColumnCount() == 0 ) {
         table.setAutoCreateColumnsFromModel(true);
         fireTableStructureChanged();
         setColumnAttributes();
      }
   }
   
   /**
   * Fetch the rows from the result set
   * @@ Support fetching subset of rows
   */
   public void fetch() throws SQLException {
      if ( results==null ) return;
      fetching = true;
      List<Object[]> tempRows = new ArrayList<Object[]>(fetchBatchSize);
      int lastRow = 0;
      int columnCount = results.getMetaData().getColumnCount();
      while ( results != null && results.next() ) {
         Object[] row = new Object[columnCount];
         for ( int j = 0; j < columnCount; j++ ) {
            row[j] = convertToDisplay(results, j+1);
         }
         tempRows.add(row);
         if ( tempRows.size() % fetchBatchSize == 0 ) {
            addRows(tempRows, lastRow);
            tempRows.clear();
            lastRow = rows.size();
            sleep(fetchSleepTime);
         }
         if ( results==null ) break;
      }
      if ( tempRows.size() > 0 ) addRows(tempRows, lastRow);
      fetching = false;
   }

   /**
   * Sleep for given number of milliseconds unless interrupted
   */
   private void sleep(long milliseconds) {
      try { Thread.currentThread().sleep(milliseconds); }
      catch (InterruptedException e) { System.out.println(Thread.currentThread()+" interrupted: "+e); }
   }

   /**
   * Add rows to the table model
   */
   private void addRows(Collection<Object[]> rowsToAdd, int start) {
      rows.addAll(rowsToAdd);
      fireTableRowsInserted(start, rows.size()-1);
   }
   
   /**
   * Cancel the current statement if possible
   */
   public void cancel() {
      try { 
         if ( statement != null ) {
            statement.cancel();
            connection.rollback();
         }
      }
      catch (SQLException e) { System.err.println(getClass()+".cancel(): "+e); }
      try { if ( results != null ) results.close(); }
      catch (SQLException e) { System.err.println(getClass()+".cancel(): "+e); }
      results = null;
   }

   /**
   * Commit the current connection
   */
   public void commit() throws SQLException {
      if ( connection != null ) connection.commit();
   }

   /**
   * Rollback the current connection
   */
   public void rollback() throws SQLException {
      if ( connection != null ) connection.rollback();
   }
   
   /**
   * Close resources used by this model
   */
   public void close() {
      cancel();
      try { if ( statement != null ) statement.close(); }
      catch (SQLException e) { System.err.println(getClass()+".close(): "+e); }
      statement = null;
      pool.returnConnection(connection);
      connection = null;
   }
   
   //
   // OVERRIDES
   //

   /**
   * Tells the table view how many columns we have
   */
   public int getColumnCount() {
      try {
         return ( results==null ) ? 0 : results.getMetaData().getColumnCount();
      } catch (SQLException e) {
         return 0;
      }
   }
   
   @Override
   public String getColumnName(int column) {
      try {
         if ( results == null || column >= results.getMetaData().getColumnCount() ) return "?";
         else return results.getMetaData().getColumnName(column+1);
      }
      catch (Exception e) {
         return "?";
      }
   }

   @Override
   public Class getColumnClass(int column) {
      try {
         if ( results == null || column >= results.getMetaData().getColumnCount() ) return String.class;
         switch ( results.getMetaData().getColumnType(column+1) ) {
            case Types.DATE:
            case Types.TIMESTAMP:
            case Types.TIME:
               return String.class;
            case Types.OTHER:
               return String.class;
            default:
               return Class.forName(results.getMetaData().getColumnClassName(column+1));
         }
      }
      catch (Exception e) {
         return String.class;
      }
   }
   
   /**
   * Return the row count for use by the table view
   */
   public int getRowCount() {
      if ( updateCount >=0 ) return updateCount;
      if ( rows == null ) return 0;
      return rows.size();
   }
   
   /**
   * Used by table view to get the objects for each table cell
   */
   public Object getValueAt(int row, int column) {
      if ( rows == null ) return null;
      return ( row < rows.size() ) ? (rows.get(row))[column] : null;
   }
   
   //
   // PUBLIC METHODS
   //

   /**
   * Return a table column model for use in a JTable to access info about SQL Metadata
   */
   public JTable getTable() { return table; }
   
   /**
   * Return a string describing whether rows were selected or affected by DML
   */
   public String getAction() {
      return getRowCount()+ " " + getRowString() + (( updateCount >= 0 ) ? " affected" : " retrieved");
   }

   /**
   * Return the current operation (sql statement)
   */
   public String getOperation() {
      return sql;
   }
   
   //
   // PRIVATE METHODS
   //
   /**
   * Initialize members
   */
   private void initialize() {
      table = new JTable(this);
      table.setAutoResizeMode(table.AUTO_RESIZE_OFF);
      columnModel = table.getColumnModel();
      columnModel.setColumnSelectionAllowed(true);
   }
   
   /**
   * Return the appropriate string row or rows depending on row count
   */
   private String getRowString() {
      int rows = getRowCount();
      if ( rows == 1 ) return "row";
      else return "rows";
   }
   
   /**
   * Use result set meta data to set column attributes in the column model
   */
   private void setColumnAttributes() throws SQLException {
      if ( results == null ) return;
      ResultSetMetaData meta = results.getMetaData();
      int charWidth = table.getFontMetrics(table.getFont()).charWidth('A');
      for ( int i=0; i<meta.getColumnCount(); i++ ) {
         int headerLength = meta.getColumnLabel(i+1).length();
         int valueLength = 3*meta.getColumnDisplaySize(i+1);
         int maxLength = Math.max(valueLength, headerLength);
         TableColumn column = columnModel.getColumn(i);
         column.setHeaderValue(meta.getColumnLabel(i+1));
         column.setMinWidth(0);
         column.setMaxWidth(maxLength*charWidth*2);
         column.setPreferredWidth(headerLength*charWidth);
      }
      table.setAutoCreateColumnsFromModel(false);
      fireTableStructureChanged();
   }

   /**
   * Convert certain objects (esp date/timestamp) returned from sql to appropriate
   * displayable objects
   */
   private Object convertToDisplay(ResultSet row, int column) {
      if ( row == null ) return null;
      try {
         switch ( row.getMetaData().getColumnType(column) ) {
            case Types.DATE:
            case Types.TIMESTAMP:
            case Types.TIME:
               java.sql.Timestamp value = row.getTimestamp(column);
               return (value==null) ? null : dateFormat.format(value);
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
            case Types.CLOB:
            case Types.OTHER:
               return row.getString(column);
//               Object o = row.getObject(column);
//               return (o==null) ? null : o.toString();
            default:
               return row.getObject(column);
         }
      }
      catch (Exception e) {
         return e.toString();
      }
   }

   //
   // STATIC
   //
   public static void setDateFormat(String format) {
      dateFormat = new SimpleDateFormat(format);
   }

}
