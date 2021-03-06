package net.contrapt.dhlp.jedit;

import net.contrapt.dhlp.model.ConnectionData;
import net.contrapt.jeditutil.selector.CompletionSelector;
import net.contrapt.jeditutil.selector.ValueSelectionDialog;
import org.apache.commons.lang.StringUtils;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.util.Log;

import java.io.*;
import java.util.*;


import net.contrapt.jeditutil.service.InfoTreeService;

import net.contrapt.dhlp.common.*;
import net.contrapt.dhlp.gui.*;
import net.contrapt.dhlp.service.DatabaseService;

/**
 * This class implements the jedit plugin protocol for the database helper
 */
public class DHLPlugin extends EBPlugin {

   //
   // PROPERTIES
   //
   public static final String NAME = "dhlp";
   public static final String PROPERTY_PREFIX = "plugin.dhlp.";
   public static final String OPTION_PREFIX = "options.dhlp.";
   public static final String MENU = PROPERTY_PREFIX + "menu";

   public static final String CONNECTION_PROPERTY = PROPERTY_PREFIX + "connection";

   public static final String DHLP_DIR = "dhlp";
   public static final String DHLP_FILE = "dhlp.json";

   public static final String REINIT_ERROR = PROPERTY_PREFIX + "reinit-error";
   public static final String EXEC_ACTION_ERROR = PROPERTY_PREFIX + "exec-sql-error";

   private static DHLPlugin instance;
   private static DatabaseService service;

   private boolean initialized = false;
   private Buffer lastBuffer; // The last buffer we handled change message for
   private String dhlpFile;

   /**
    * Startup routine; set the instance variable for use by others and create a controller
    */
   public void start() {
      instance = this;
      Log.log(Log.NOTICE, this, "Starting dhlp");
      this.dhlpFile = (jEdit.getSettingsDirectory() == null) ?
            DHLP_FILE :
            jEdit.getSettingsDirectory() + File.separator + DHLP_DIR + File.separator + DHLP_FILE;
      DHLPController.create(dhlpFile);
      init(null);
   }

   /**
    * Wrap up, save files, release resources etc.
    */
   public void stop() {
      Log.log(Log.NOTICE, this, "Stopping dhlp");
      // Close all sql result windows
      ConnectionPanel.removePluginPanels(ConnectionPanel.class);
      // Close all database connections
      DHLPController.getInstance().shutdown();
      // Close the service; don't nullify it since it may be referenced!
      if (service != null) service.close();
      // Invalidate instance
      instance = null;
   }

   /**
    * Return the constructed instance of this plugin (as assigned in the start() method)
    */
   public static DHLPlugin getInstance() {
      return instance;
   }

   /**
    * Return the database service instance
    */
   public static InfoTreeService getService() {
      if (service == null) service = new DatabaseService();
      return service;
   }

   /**
    * Initialize the plugin
    *
    * @param view The jedit view this was called from
    */
   public void init(View view) {
      if (initialized) return;
      try {
         DHLPController.getInstance().load();
         addJars();
         if (service != null) service.reinit();
         initialized = true;
      } catch (Exception e) {
         if (view == null) {
            Log.log(Log.ERROR, this, "Error initializing from " + dhlpFile + ": " + e);
            e.printStackTrace();
         }
         handleException(view, REINIT_ERROR, new Object[]{dhlpFile, e.getMessage()});
      }
   }

   /**
    * Reload the database configuration file
    */
   public void reinit(View view) {
      initialized = false;
      init(view);
   }

   /**
    * Run the gui to configure?
    *
    * @param view The invoking view
    */
   public void configure(View view) {
      init(view);
   }

   /**
    * Implement the edit bus listener interface
    */
   public void handleMessage(EBMessage message) {
      // Handle build and project file updates
      if (message instanceof BufferUpdate) handleBufferUpdate((BufferUpdate) message);
      else if (message instanceof EditPaneUpdate) handleEditPaneUpdate((EditPaneUpdate) message);
   }

   /**
    * Return a completion selector depending on the current context
    */
   public CompletionSelector getCompletionSelector(View view) {
      String connection = getConnection(view);
      if (connection == null) return null;
      //TODO figure out the context (from parsed buffer?)
      view.getTextArea().selectWord();
      String token = view.getTextArea().getSelectedText();
      view.getTextArea().removeFromSelection(view.getTextArea().getSelection(0));
      CompletionSelector selector = DHLPSelector.getObjectCompletionSelector(view, token, DHLPController.getInstance(), connection);
      return selector;
   }

   /**
    * Execute the current buffer or buffer selection as a sql statement using the current connection properties
    */
   public void execSql(View view) {
      try {
         // Add a connection panel to dock if not already added
         String connection = getConnection(view);
         if (connection == null) return;
         ConnectionPanel panel = getConnectionPanel(view, connection);
         // Execute the statement; either selection or full text
         String sql = panel.getSelectedText();
         if (sql == null) sql = panel.getText();
         panel.addStatement(sql);
         panel.execute();
      } catch (Exception e) {
         handleException(view, EXEC_ACTION_ERROR, new Object[]{"SQL Statement", getExceptionString(e)});
      }
   }

   /**
    * Generate a query plan for the given sql statement
    */
   public void generateQueryPlan(View view) {
      try {
         String connection = getConnection(view);
         if (connection == null) return;
         ConnectionPanel panel = getConnectionPanel(view, connection);
         String sql = panel.getSelectedText();
         if (sql == null) sql = panel.getText();
         panel.addQueryPlan(sql);
         panel.execute();
      } catch (Exception e) {
         handleException(view, EXEC_ACTION_ERROR, new Object[]{"Query Plan", getExceptionString(e)});
      }
   }

   public void createBuffer(String contents) {
      Buffer buf = jEdit.newFile(jEdit.getActiveView());
      buf.insert(0, contents);
   }

   /**
    * Describe the chosen database object
    */
   public void describeObject(View view) {
      try {
         String objectName = view.getTextArea().getSelectedText();
         String connection = getConnection(view);
         if (connection == null) return;
         JDBCObject object = null;
         if (objectName != null)
            object = DHLPController.getInstance().findObject(connection, objectName);
         if (object == null) object = promptForObject(view, connection, objectName).getSelectedObject();
         if (object == null) return;
         DHLPController.getInstance().describeObject(connection, object);
         Buffer buf = jEdit.newFile(view);
         buf.insert(0, object.describeAsString());
      } catch (Exception e) {
         handleException(view, EXEC_ACTION_ERROR, new Object[]{"Describing Object", getExceptionString(e)});
      }
   }

   /**
    * Display list of all objects in the database
    */
   public void findObjects(View view) {
      try {
         String connection = getConnection(view);
         if (connection == null) return;
         DHLPSelector<ConnectionData, JDBCObject> selector = promptForObject(view, connection, null);
         JDBCObject object = selector.getSelectedObject();
         if (object == null) return;
         ConnectionPanel panel = null;
         switch (selector.getAction()) {
            case DESCRIBE:
               panel = getConnectionPanel(view, connection);
               panel.addDescription(object);
               panel.execute();
               break;
            case INSERT:
               Buffer buf = view.getBuffer();
               int pos = view.getTextArea().getCaretPosition();
               buf.insert(pos, object.getName());
               break;
            case SELECT:
               panel = getConnectionPanel(view, connection);
               panel.addStatement("select * from " + object.getName());
               panel.execute();
               break;
         }
      } catch (Exception e) {
         handleException(view, EXEC_ACTION_ERROR, new Object[]{"Finding Object", getExceptionString(e)});
      }
   }

   /**
    * Return the connection if it is defined as a property or prompt the user
    * to choose it
    */
   private String getConnection(View view) {
      String connection = view.getBuffer().getStringProperty(CONNECTION_PROPERTY);
      Log.log(Log.NOTICE, this, "Found connection property: " + CONNECTION_PROPERTY + "=" + connection);
      if (StringUtils.isBlank(connection)) {
         ConnectionData c = promptForConnection(view);
         if (c != null) {
            connection = c.getName();
            view.getBuffer().setStringProperty(CONNECTION_PROPERTY, c.getName());
         }
      }
      DatabaseStatusService.getInstance().setConnection(connection);
      return connection;
   }

   /**
    * Allow the user to choose the connection to use
    */
   private ConnectionData promptForConnection(View view) {
      DHLPSelector<Object, ConnectionData> selector = DHLPSelector.getConnectionSelector(DHLPController.getInstance());
      ValueSelectionDialog.open(view, selector);
      ConnectionData connection = selector.getSelectedObject();
      return connection;
   }

   /**
    * Allow user to choose a database object from list for the current connection
    */
   private DHLPSelector<ConnectionData, JDBCObject> promptForObject(View view, String connection, String name) {
      DHLPSelector<ConnectionData, JDBCObject> selector = DHLPSelector.getObjectSelector(DHLPController.getInstance(), connection);
      selector.setDefault(name);
      ValueSelectionDialog.open(view, selector);
      return selector;
   }

   /**
    * Return a connection panel from the dock
    */
   private ConnectionPanel getConnectionPanel(View view, String connection) {
      ConnectionPanel panel = ConnectionPanel.getPluginPanel(view, ConnectionPanel.class);
      if (panel == null) {
         panel = new ConnectionPanel(DHLPController.getInstance().getPool(connection));
         ConnectionPanel.addPluginPanel(view, panel);
      }
      return panel;
   }

   /**
    * Handle buffer update messages
    */
   private void handleBufferUpdate(BufferUpdate message) {
      if (message.getWhat() == message.DIRTY_CHANGED) {
      } else if (message.getWhat() == message.SAVED) {
      }
   }

   /**
    * Handle edit pane update messages
    */
   private void handleEditPaneUpdate(EditPaneUpdate message) {
      if (message.getWhat() == message.BUFFER_CHANGED) {
      }
   }

   /**
    * Handle the exception by showing an error dialog and logging an error
    */
   private void handleException(View view, String propertyName, Object[] params) {
      GUIUtilities.error(view, propertyName, params);
      GUIUtilities.requestFocus(view, view);
   }

   /**
    * Add any database driver jar files to jedit classpath
    */
   private void addJars() {
      for (Iterator i = DHLPController.getInstance().getDriverJars().iterator(); i.hasNext(); ) {
         String jarFile = (String) i.next();
         addJar(jarFile);
      }
   }

   /**
    * Add a given jar file to jedit's classpath; this will be used to add database driver jar files
    * to the environment classpath
    */
   private void addJar(String jarFile) {
      if (jarFile == null || jarFile.equals("")) return;
      PluginJAR jar = jEdit.getPluginJAR(jarFile);
      if (jar != null) return; // Already added
      jEdit.addPluginJAR(jarFile);
   }

   private String getExceptionString(Exception e) {
      StringBuilder buf = new StringBuilder(e.toString());
      Throwable c = e.getCause();
      while ( c != null ) {
         buf.append("\n"+c);
         c = c.getCause();
      }
      return buf.toString();
   }

}
