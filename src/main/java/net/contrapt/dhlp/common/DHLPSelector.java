package net.contrapt.dhlp.common;

import java.util.*;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import javax.swing.KeyStroke;

import net.contrapt.dhlp.model.ConnectionData;
import net.contrapt.jeditutil.selector.ValueSelector;
import org.gjt.sp.jedit.View;

import net.contrapt.jeditutil.selector.CompletionSelector;

/**
* Class that handles selecting various connection specific values
* for presentation to the user
*/
public abstract class DHLPSelector<P,V> extends ValueSelector<P,V> {
   
   //
   // Properties
   //
   protected DHLPController controller;
   protected String connection;
   protected String defaultName;
   protected ActionEnum selectedAction = ActionEnum.DESCRIBE;

   public enum ActionEnum {
      DESCRIBE,
      INSERT,
      SELECT
   }
   //
   // Constructors
   //
   protected DHLPSelector(DHLPController controller, String connection) {
      this.controller = controller;
      this.connection = connection;
   }

   protected DHLPSelector(DHLPController controller) {
      this.controller = controller;
   }

   public String getParentLabel() { return "Connection:"; }
   public String getParentKey() { return connection; }

   public final Map<String,ConnectionData> getConnections() {
      return ( controller == null ) ? null : controller.getConnections();
   }

   public final void setDefault(String defaultName) {
      this.defaultName = defaultName;
   }

   public ActionEnum getAction() { return selectedAction; }

   //
   // ANONYMOUS Classes to do the work for standard items
   //
   
   // Allow selection of a project

   /**
   * Selector for choosing connection
   */
   public static DHLPSelector<Object,ConnectionData> getConnectionSelector(DHLPController controller) {
      return new DHLPSelector<Object,ConnectionData>(controller) {
         public String getTitle() { return "Choose Connection"; }
         public Map<String,ConnectionData> loadValueMap() { return getConnections(); }
         public String getDefault() { return null; }
         public String getParentLabel() { return null; }
         public String getParentKey() { return null; }
         public boolean isCaseSensitive() { return false; }
      };
   }

   /**
   * Selector for returning database objects for the current connection
   */
   public static DHLPSelector<ConnectionData,JDBCObject> getObjectSelector(DHLPController controller, String connection) {
      return new DHLPSelector<ConnectionData,JDBCObject>(controller, connection) {
         public boolean isCaseSensitive() { return false; }
         public String getTitle() { return "Select Database Object"; }
         public Map<String,JDBCObject> loadValueMap() { return controller.findObjects(connection, false); }
         public String getDefault() { return defaultName; }
         public Map<String,ConnectionData> getParents() { return getConnections(); }
         public void setParent(ConnectionData connection) { 
            this.connection = ( connection == null ) ? null : connection.getName(); 
         }
         public ConnectionData getParent() { return controller.getConnection(this.connection); }
         public Collection<Action> getActions() {
            List<Action> actions = new ArrayList<Action>();
            Action currentAction;
            // Describe the selected object
            currentAction = new AbstractAction("Describe Object") {
               public void actionPerformed(ActionEvent e) {
                  selectedAction = ActionEnum.DESCRIBE;
               }
            };
            currentAction.putValue(currentAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, 2));
            actions.add(currentAction);
            // Insert the object name at the current buffer position
            currentAction = new AbstractAction("Insert Object Name") {
               public void actionPerformed(ActionEvent e) {
                  selectedAction = ActionEnum.INSERT;
               }
            };
            currentAction.putValue(currentAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, 2));
            actions.add(currentAction);
            // Select * from the selected object
            currentAction = new AbstractAction("SQL Select") {
               public void actionPerformed(ActionEvent e) {
                  selectedAction = ActionEnum.SELECT;
               }
            };
            currentAction.putValue(currentAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, 2));
            actions.add(currentAction);
            // All done
            return actions;
         }
      };
   }

   /**
   * Code completion selector for the given connection
   */
   public static CompletionSelector<JDBCObject> getObjectCompletionSelector(View view, String defaultValue, final DHLPController controller, final String connection) {
      return new CompletionSelector<JDBCObject>(view, defaultValue) {
         public boolean isCaseSensitive() { return false; }
         public String getTitle() { return "Database Objects"; }
         public Map<String,JDBCObject> loadValueMap() { return controller.findObjects(connection, false); }
         public String getCompletionString() {
            return ( getSelectedString()==null ) ? null : getSelectedObject().getName();
         }
      };
   }

}

