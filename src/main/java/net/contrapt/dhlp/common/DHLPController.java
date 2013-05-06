package net.contrapt.dhlp.common;

import java.util.*;
import java.io.*;
import java.sql.*;

import net.contrapt.dhlp.model.ConfigurationData;
import net.contrapt.dhlp.model.ConnectionData;
import net.contrapt.dhlp.model.DriverData;
import net.contrapt.jeditutil.model.BaseModel;

/**
 * The controller manages sql connections and execution of statements
 */
public class DHLPController {

   //
   // PROPERTIES
   //
   private static DHLPController INSTANCE;
   private String dhlpFile;
   private ConfigurationData dhlpData;
   private Map<String, ConnectionData> connections;
   private Map<String, ConnectionPool> pools;
   // A map of objects by connection
   private Map<String, SortedMap<String, JDBCObject>> objectsByConnection;

   //
   // CONSTRUCTORS
   //
   private DHLPController() {
      pools = new HashMap<String, ConnectionPool>();
      connections = new TreeMap<String, ConnectionData>();
      objectsByConnection = new HashMap<String, SortedMap<String, JDBCObject>>();
   }

   private DHLPController(String dhlpFile) {
      this();
      this.dhlpFile = dhlpFile;
   }

   //
   // Static Methods
   //

   /**
    * Create a new instance of the controller
    */
   public static void create(String dhlpFile) {
      if (INSTANCE != null) return;
      INSTANCE = new DHLPController(dhlpFile);
   }

   /**
    * Return the current controller instance
    */
   public static DHLPController getInstance() {
      return INSTANCE;
   }

   //
   // PUBLIC METHODS
   //

   /**
    * Read in the connection file
    */
   public void load() {
      if (dhlpFile == null) throw new IllegalStateException("Cannot load with null file");
      try {
         dhlpData = BaseModel.readData(dhlpFile, ConfigurationData.class);
      } catch (Exception e) {
         System.err.println("Error loading " + dhlpFile + ": " + e);
      }
      if (dhlpData == null) dhlpData = new ConfigurationData();
      mapConnections();
   }

   /**
    * Write out the current connection file
    */
   public void save() {
      if (dhlpFile == null) throw new IllegalStateException("Cannot save with null file");
      if (dhlpData == null) throw new IllegalStateException("Configuration has not been loaded yet");
      try {
         FileWriter out = new FileWriter(dhlpFile);
      } catch (Exception e) {
         throw new IllegalStateException("Error saving '" + dhlpFile + "': " + e.getMessage());
      }
   }

   /**
    * Close the controller
    */
   public void shutdown() {
      for (ConnectionPool pool : pools.values()) {
         pool.close();
      }
      pools.clear();
      connections.clear();
      objectsByConnection.clear();
      INSTANCE = null;
   }

   public void describeObject(String connection, JDBCObject object) {
      if (object.isDescribed()) return;
      ConnectionPool pool = getPool(connection);
      if (object.getClass().equals(JDBCTable.class)) describeObject(pool, (JDBCTable) object);
      if (object.getClass().equals(JDBCView.class)) describeObject(pool, (JDBCView) object);
      if (object.getClass().equals(JDBCSynonym.class)) describeObject(pool, (JDBCSynonym) object);
      if (object.getClass().equals(JDBCProcedure.class)) describeObject(pool, (JDBCProcedure) object);
   }

   /**
    * Look for an object with the given name; if not found return null
    */
   public JDBCObject findObject(String connection, String objectName) {
      SortedMap<String, JDBCObject> objects = findObjects(connection, false);
      SortedMap<String, JDBCObject> subMap = objects.subMap(objectName.toUpperCase(), objectName.toUpperCase() + "_");
      if (subMap.size() > 1) return null;
      else if (subMap.size() == 1) return subMap.get(subMap.firstKey());
      else return null;
   }

   /**
    * Return a map of database objects; used cached version unless asked to refresh
    * the list
    */
   public SortedMap<String, JDBCObject> findObjects(String connection, boolean refresh) {
      SortedMap<String, JDBCObject> objects = objectsByConnection.get(connection);
      if (objects != null) {
         if (refresh) objects.clear();
         else return objects;
      } else {
         objects = new TreeMap<String, JDBCObject>();
         objectsByConnection.put(connection, objects);
      }
      ConnectionPool pool = null;
      Connection db = null;
      try {
         pool = getPool(connection);
         db = pool.takeConnection();
         for (String schema : pool.getSchema()) {
            String[] splits = schema.split("\\.");
            schema = splits[0];
            String filter = splits.length == 2 ? splits[1] : "%";
            // Tables, Views and Synonyms
            ResultSet results = db.getMetaData().getTables(schema, schema, filter, null);
            while (results.next()) {
               JDBCObject object = JDBCObject.create(results);
               objects.put(object.getDisplayName(), object);
            }
            results.close();
            // Procedures
            results = db.getMetaData().getProcedures(schema, schema, filter);
            while (results.next()) {
               JDBCObject object = JDBCObject.create(results);
               objects.put(object.getDisplayName(), object);
            }
            results.close();
         }
      } catch (Exception e) {
         System.err.println("Error loading object list for " + connection + ": " + e);
      } finally {
         try {
            pool.returnConnection(db);
         } catch (Exception e) {
         }
      }
      return objects;
   }

   /**
    * Return the fetch limit
    */
   public int getFetchLimit() {
      return (dhlpData == null) ? 0 : dhlpData.getFetchLimit();
   }

   /**
    * Add a new driver record
    */
   public void addDriver(String name, String className, String jarFile) {
   }

   /**
    * Find a driver by name
    */
   public DriverData getDriver(String name) {
      if (dhlpData == null) return null;
      for (DriverData data : dhlpData.getDrivers()) {
         if (data.getName().equals(name)) return data;
      }
      return null;
   }

   /**
    * Return a collection of driver jar files
    */
   public Collection getDriverJars() {
      List jars = new ArrayList();
      if (dhlpData == null) return jars;
      for (DriverData data : dhlpData.getDrivers()) {
         jars.add(data.getJarFile());
      }
      return jars;
   }

   /**
    * Return list of drivers
    */
   public Collection<DriverData> getDrivers() {
      if (dhlpData == null) return Collections.EMPTY_LIST;
      return dhlpData.getDrivers();
   }

   /**
    * Add a new connection record
    */
   public void addConnection(String name, String driver, String url, String user, String password) {
      if (getConnection(name) != null) return;
   }

   /**
    * Find a connection by name
    */
   public ConnectionData getConnection(String name) {
      return connections.get(name);
   }

   /**
    * Return the map of connections
    */
   public Map<String, ConnectionData> getConnections() {
      return connections;
   }

   /**
    * Get a connection pool for the given connection name
    */
   public ConnectionPool getPool(String name) {
      ConnectionData connectionData = getConnection(name);
      if (connectionData == null) throw new IllegalStateException("No connection named '" + name + "' found");
      DriverData driverData = getDriver(connectionData.getDriver());
      if (driverData == null)
         throw new IllegalStateException("No driver named '" + connectionData.getDriver() + "' found");
      ConnectionPool pool = pools.get(name);
      if (pool == null) {
         if (!connectionData.isAutocommit()) connectionData.setAutocommit(dhlpData.isAutocommit());
         pool = new ConnectionPool(driverData, connectionData);
         pools.put(name, pool);
      }
      return pool;
   }

   /**
    * Describe a view
    */
   public void describeObject(ConnectionPool pool, JDBCView view) {
      describeColumns(pool, view);
      // TODO Get the text of the view; database specific
      view.setDescribed();
   }

   /**
    * Describe a synonym
    */
   public void describeObject(ConnectionPool pool, JDBCSynonym synonym) {
      // TODO Resolve the synonym
      synonym.setDescribed();
   }

   /**
    * Describe a table
    */
   public void describeObject(ConnectionPool pool, JDBCTable table) {
      describeColumns(pool, table);
      describePrimaryKey(pool, table);
      describeIndices(pool, table);
      describeChildren(pool, table);
      describeParents(pool, table);
      table.setDescribed();
   }

   /**
    * Describe a procedure
    */
   public void describeObject(ConnectionPool pool, JDBCProcedure procedure) {
      describeParameters(pool, procedure);
      procedure.setDescribed();
   }

   /**
    * For use by tree model
    */
   public void describeColumns(String connection, JDBCView view) {
      try {
         ConnectionPool pool = getPool(connection);
         describeColumns(pool, view);
      } catch (Exception e) {
         System.err.println("Error describing columns for " + view + ": " + e);
      }
   }

   /**
    * For use by the tree model
    */
   public void describePrimaryKey(String connection, JDBCTable table) {
      try {
         ConnectionPool pool = getPool(connection);
         describePrimaryKey(pool, table);
      } catch (Exception e) {
         System.err.println("Error describing primary key for " + table + ": " + e);
      }
   }

   /**
    * For use by the tree model
    */
   public void describeIndices(String connection, JDBCTable table) {
      try {
         ConnectionPool pool = getPool(connection);
         describeIndices(pool, table);
      } catch (Exception e) {
         System.err.println("Error describing indices for " + table + ": " + e);
      }
   }

   /**
    * For use by the tree model
    */
   public void describeChildren(String connection, JDBCTable table) {
      try {
         ConnectionPool pool = getPool(connection);
         describeChildren(pool, table);
      } catch (Exception e) {
         System.err.println("Error describing children for " + table + ": " + e);
      }
   }

   /**
    * For use by the tree model
    */
   public void describeParents(String connection, JDBCTable table) {
      try {
         ConnectionPool pool = getPool(connection);
         describeParents(pool, table);
      } catch (Exception e) {
         System.err.println("Error describing parents for " + table + ": " + e);
      }
   }

   /**
    * For use by the tree model
    */
   public void describeParameters(String connection, JDBCProcedure procedure) {
      try {
         ConnectionPool pool = getPool(connection);
         describeParameters(pool, procedure);
      } catch (Exception e) {
         System.err.println("Error describing parameters for " + procedure + ": " + e);
      }
   }

   /**
    * Describe the columns of a table
    */
   public void describeColumns(ConnectionPool pool, JDBCView table) {
      Connection db = null;
      try {
         db = pool.takeConnection();
         ResultSet results = db.getMetaData().getColumns(table.getCatalog(), table.getSchema(), table.getName(), "%");
         // Columns
         table.addColumns(results);
         results.close();
      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalStateException("Error describing columns for " + table, e);
      } finally {
         pool.returnConnection(db);
      }
   }

   /**
    * Describe the primary keys of a table
    */
   public void describePrimaryKey(ConnectionPool pool, JDBCTable table) {
      Connection db = null;
      try {
         db = pool.takeConnection();
         // Primary key columns
         ResultSet results = db.getMetaData().getPrimaryKeys(table.getCatalog(), table.getSchema(), table.getName());
         table.addPrimaryKey(results);
         results.close();
      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalStateException("Error describing columns for " + table, e);
      } finally {
         pool.returnConnection(db);
      }
   }

   /**
    * Describe the indices of a table
    */
   public void describeIndices(ConnectionPool pool, JDBCTable table) {
      Connection db = null;
      try {
         db = pool.takeConnection();
         // Indexes
         ResultSet results = db.getMetaData().getIndexInfo(table.getCatalog(), table.getSchema(), table.getName(), false, false);
         table.addIndices(results);
         results.close();
      } catch (Exception e) {
         System.out.println("Error getting indices for " + table + ": " + e);
      } finally {
         pool.returnConnection(db);
      }
   }

   /**
    * Describe the child references of a table
    */
   public void describeChildren(ConnectionPool pool, JDBCTable table) {
      Connection db = null;
      try {
         db = pool.takeConnection();
         // Child constraints
         ResultSet results = db.getMetaData().getExportedKeys(table.getCatalog(), table.getSchema(), table.getName());
         table.addChildren(results);
         results.close();
      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalStateException("Error describing table " + table, e);
      } finally {
         pool.returnConnection(db);
      }
   }

   /**
    * Describe the parent references to a table
    */
   public void describeParents(ConnectionPool pool, JDBCTable table) {
      Connection db = null;
      try {
         db = pool.takeConnection();
         // Parent constraints
         ResultSet results = db.getMetaData().getImportedKeys(table.getCatalog(), table.getSchema(), table.getName());
         table.addParents(results);
         results.close();
      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalStateException("Error describing table " + table, e);
      } finally {
         pool.returnConnection(db);
      }
   }

   /**
    * Describe the parameters of a procedure
    */
   private void describeParameters(ConnectionPool pool, JDBCProcedure procedure) {
      Connection db = null;
      try {
         db = pool.takeConnection();
         ResultSet results = db.getMetaData().getProcedureColumns(procedure.getCatalog(), procedure.getSchema(), procedure.getName(), "%");
         // Columns
         procedure.addColumns(results);
         results.close();
         // That's it
         procedure.setDescribed();
      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalStateException("Error describing procedure " + procedure, e);
      } finally {
         pool.returnConnection(db);
      }
   }

   //
   // PRIVATE METHODS
   //

   /**
    * Put connections into map
    */
   private void mapConnections() {
      if (dhlpData == null) return;
      for (ConnectionData data : dhlpData.getConnections()) {
         connections.put(data.getName(), data);
      }
   }

}
