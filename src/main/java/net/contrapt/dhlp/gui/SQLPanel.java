package net.contrapt.dhlp.gui;

import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import net.contrapt.dhlp.common.*;

/**
* This abstract class defines the actions expected for various types of SQL display panels as well
* as a framework for running the potentially long running jdbc requests in separate threads.  Various
* types of display panels can extend this class and be invoked by the connection panel
*/
public abstract class SQLPanel extends JPanel implements Runnable {
   
   //
   // PROPERTIES
   //
   private int executionCount;
   private long executionTime;
   
   private enum TaskEnum {
      NO_TASK,
      EXECUTE,
      FETCH,
      CANCEL,
      CLOSE,
      COMMIT,
      ROLLBACK
   };
   private TaskEnum task;
   
   private JScrollPane resultPanel;
   private JPanel statusPanel;
   private JTextField statusText;
   
   //
   // CONSTRUCTORS
   //

   protected SQLPanel() {
   }
   
   //
   // OVERRIDES
   //
   
   //
   // PUBLIC METHODS
   //
   
   /**
   * Run the current task as a background process
   */
   public final void run() {
      try {
         switch ( task ) {
            case EXECUTE:
               doExecute();
               break;
            case FETCH:
               doFetch();
               break;
            case CANCEL:
               doCancel();
               break;
            case CLOSE:
               doClose();
               break;
            case COMMIT:
               doCommit();
               break;
            case ROLLBACK:
               doRollback();
               break;
         }
         task = TaskEnum.NO_TASK;
      }
      catch (Exception e) {
         statusText.setText("Error performaing SQL task: "+e);
         e.printStackTrace();
      }
   }
   
   /**
   * Setup and spawn task to execute the sql statement
   */
   public final void execute() {
      startTaskThread(TaskEnum.EXECUTE);
   }
   
   /**
   * Setup and spawn task to fetch rows
   */
   public final void fetch() {
      startTaskThread(TaskEnum.FETCH);
   }
   
   /**
   * Cancel the currently running sql statement
   */
   public final void cancel() {
      startTaskThread(TaskEnum.CANCEL);
   }
   
   /**
   * Commit the current connection
   */
   public final void commit() {
      startTaskThread(TaskEnum.COMMIT);
   }

   /**
   * Rollback the current connection
   */
   public final void rollback() {
      startTaskThread(TaskEnum.ROLLBACK);
   }
   
   /**
   * Close this panel
   */
   public final void close() {
      startTaskThread(TaskEnum.CLOSE);
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

   //
   // PRIVATE METHODS
   //

   /**
   * Initialize variables and layout
   */
   protected void initialize() {
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
      statusText = new JTextField("", 30);
      statusText.setEditable(false);
      statusPanel = new JPanel();
      statusPanel.setLayout(new GridLayout(1, 1));
      statusPanel.add(statusText);
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
      }
      catch(SQLException e) {
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
         getModel().fetch();
         displayExecutionStatus();
      }
      catch(SQLException e) {
         statusText.setText("Error fetching rows: "+e);
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
      }
      catch(SQLException e) {
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
      }
      catch (SQLException e) { 
         statusText.setText("Error closing model");
         displayError(e);
         System.err.println(getClass()+".doClose(): "+e); 
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
      }
      catch (SQLException e) { 
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
      }
      catch (SQLException e) { 
         statusText.setText("Error rolling back transaction");
         displayError(e);
      }
   }

  /**
   * Display the query execution statistics in the status bar
   */
   private void displayExecutionStatus() {
      double elapsed = executionTime/1000.00;
      statusText.setText("Execution #"+executionCount+" ("+elapsed+"s): "+getModel().getAction());
   }
   
   /**
   * Display an execution error
   */
   private void displayError(SQLException e) {
      remove(resultPanel);
      JTextArea error = new JTextArea("Error:\n"+e.getMessage()+"\n"+e.getNextException()+"\n\nOperation:\n"+getModel().getOperation());
      JScrollPane pane = new JScrollPane(error);
      pane.setAutoscrolls(true);
      error.setEditable(false);
      add(BorderLayout.CENTER, pane);
      revalidate();
   }

   /**
   * Start a thread to execute the given sql task
   */
   private void startTaskThread(TaskEnum task) {
      this.task = task; 
      Thread taskThread = new Thread(this, getClass().getName()+"."+task.name());
      taskThread.setDaemon(true);
      taskThread.setPriority(Thread.NORM_PRIORITY);
      taskThread.start();
   }
   
   //
   // STATIC METHODS
   //
   
}
