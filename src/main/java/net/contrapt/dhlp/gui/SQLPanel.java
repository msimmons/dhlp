package net.contrapt.dhlp.gui;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.contrapt.dhlp.common.*;
import net.contrapt.dhlp.jedit.DHLPlugin;
import org.gjt.sp.jedit.GUIUtilities;

/**
 * This abstract class defines the actions expected for various types of SQL display panels as well
 * as a framework for running the potentially long running jdbc requests in separate threads.  Various
 * types of display panels can extend this class and be invoked by the connection panel
 */
public abstract class SQLPanel extends JPanel {

   private int executionCount;
   private long executionTime;
   private boolean pinned = false;
   private boolean limited = true;

   private JScrollPane resultPanel;
   private JPanel statusPanel;
   private JTextField statusText;

   protected SQLPanel() {
   }

   public boolean isPinned() {
      return pinned;
   }

   public boolean isLimited() {
      return limited;
   }

   /**
    * Setup and spawn task to execute the sql statement
    */
   public final void execute() {
      startTaskThread(
            new Runnable() {
               @Override
               public void run() {
                  doExecute();
               }
            }, "execute");
   }

   /**
    * Setup and spawn task to fetch rows
    */
   public final void fetch() {
      startTaskThread(
            new Runnable() {
               @Override
               public void run() {
                  doFetch();
               }
            }, "fetch");
   }

   /**
    * Cancel the currently running sql statement
    */
   public final void cancel() {
      startTaskThread(
            new Runnable() {
               @Override
               public void run() {
                  doCancel();
               }
            }, "cancel");
   }

   /**
    * Commit the current connection
    */
   public final void commit() {
      startTaskThread(
            new Runnable() {
               @Override
               public void run() {
                  doCommit();
               }
            }, "commit");
   }

   /**
    * Rollback the current connection
    */
   public final void rollback() {
      startTaskThread(
            new Runnable() {
               @Override
               public void run() {
                  doRollback();
               }
            }, "rollback");
   }

   /**
    * Close this panel
    */
   public final void close() {
      startTaskThread(
            new Runnable() {
               @Override
               public void run() {
                  doClose();
               }
            }, "close");
   }

   /**
    * Export the data in the panel
    */
   public final void export() {
      doExport();
   }

   /**
    * Implement this method to return SQLModel which implements the various operations called by
    * the actions
    */
   public abstract SQLModel getModel();

   /**
    * Implement this method to return a component to display in the result panel
    */
   public abstract JComponent getComponent();

   /**
    * Initialize variables and layout
    */
   protected void initialize() {
      layoutComponents();
   }

   /**
    * Reset the result panel
    */
   protected void reinit() {
      doClose();
      removeAll();
      layoutComponents();
   }

   /**
    * Create and layout components
    */
   private void layoutComponents() {
      // Create a scrolling panel for the sql results
      resultPanel = new JScrollPane(getComponent());
      resultPanel.setAutoscrolls(true);
      // Create a panel to show status
      statusText = new JTextField("", 60);
      statusText.setEditable(false);
      JCheckBox pinnedBox = new JCheckBox(PinnedAction);
      JCheckBox limitedBox = new JCheckBox(LimitedAction);
      limitedBox.setSelected(limited);
      statusPanel = new JPanel();
      statusPanel.setLayout(new BorderLayout());
      statusPanel.add(statusText, BorderLayout.WEST);
      JPanel boxPanel = new JPanel();
      boxPanel.add(pinnedBox);
      boxPanel.add(limitedBox);
      statusPanel.add(boxPanel, BorderLayout.EAST);
      // Put them all together on the content pane
      setLayout(new BorderLayout());
      add(resultPanel, BorderLayout.CENTER);
      add(statusPanel, BorderLayout.SOUTH);
   }

   /**
    * Excecute the sql statement and fetch the rows
    */
   private void doExecute() {
      try {
         statusText.setText("Executing...");
         executionTime = System.currentTimeMillis();
         getModel().execute();
         executionTime = System.currentTimeMillis() - executionTime;
         executionCount++;
         displayExecutionStatus();
         doFetch();
      } catch (Exception e) {
         statusText.setText("Error executing statement");
         displayError(e);
      }
   }

   /**
    * Fetch rows
    */
   private void doFetch() {
      try {
         statusText.setText("Fetching...");
         getModel().fetch(limited);
         displayExecutionStatus();
      } catch (Exception e) {
         statusText.setText("Error fetching rows: " + e);
         //displayError(e);
      }
   }

   /**
    * Cancel the currently executing sql statement
    */
   private void doCancel() {
      try {
         statusText.setText("Cancelling...");
         getModel().cancel();
         statusText.setText("Cancelled");
      } catch (Exception e) {
         statusText.setText("Error cancelling statement");
         displayError(e);
      }
   }

   /**
    * Do what's necessary when panel is closed
    */
   private void doClose() {
      try {
         statusText.setText("Closing...");
         getModel().close();
         statusText.setText("Closed");
      } catch (Exception e) {
         statusText.setText("Error closing model");
         displayError(e);
         System.err.println(getClass() + ".doClose(): " + e);
      }
   }

   /**
    * Commit the model transaction
    */
   private void doCommit() {
      try {
         statusText.setText("Committing...");
         getModel().commit();
         statusText.setText("Committed");
      } catch (Exception e) {
         statusText.setText("Error committing transaction");
         displayError(e);
      }
   }

   /**
    * Rollback the model transaction
    */
   private void doRollback() {
      try {
         statusText.setText("Rolling back...");
         getModel().rollback();
         statusText.setText("Rolled back");
      } catch (Exception e) {
         statusText.setText("Error rolling back transaction");
         displayError(e);
      }
   }

   /**
    * Export the data for the model
    */
   private void doExport() {
      try {
         StringWriter buf = new StringWriter();
         BufferedWriter out = new BufferedWriter(buf);
         getModel().export(out);
         out.close();
         DHLPlugin.getInstance().createBuffer(buf.toString());
      } catch (Exception e) {
         statusText.setText("Error exporting data: " + e.getMessage());
      }
   }

   /**
    * Display the query execution statistics in the status bar
    */
   private void displayExecutionStatus() {
      double elapsed = executionTime / 1000.00;
      statusText.setText("Execution #" + executionCount + " (" + elapsed + "s): " + getModel().getAction() + ": " + getModel().getSql());
      statusText.setToolTipText(getModel().getSql());
   }

   /**
    * Display an execution error
    */
   private void displayError(Exception e) {
      remove(resultPanel);
      JTextArea error = new JTextArea(getExceptionString(e) + "\n\nOperation:\n" + getModel().getOperation());
      JScrollPane pane = new JScrollPane(error);
      pane.setAutoscrolls(true);
      error.setEditable(false);
      add(BorderLayout.CENTER, pane);
      revalidate();
   }

   private String getExceptionString(Exception e) {
      StringBuilder msg = new StringBuilder("SQLException: " + e.getMessage());
      Throwable c = e.getCause();
      while (c != null) {
         msg.append("\n      " + c);
         c = c.getCause();
      }
      return msg.toString();
   }

   /**
    * Start a thread to execute the given sql task
    */
   private void startTaskThread(Runnable task, String name) {
      Thread taskThread = new Thread(task, getClass().getSimpleName() + "." + name);
      taskThread.setDaemon(true);
      taskThread.setPriority(Thread.NORM_PRIORITY);
      taskThread.start();
   }

   private Action PinnedAction = new AbstractAction("Pin?") {
      public void actionPerformed(ActionEvent e) {
         pinned = !pinned;
      }
   };

   private Action LimitedAction = new AbstractAction("Limit?") {
      public void actionPerformed(ActionEvent e) {
         limited = !limited;
      }
   };

}
