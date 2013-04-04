package net.contrapt.dhlp.gui;

import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Stack;

import net.contrapt.jeditutil.PluginPanel;
import net.contrapt.dhlp.common.*;

/**
* A frame which shows one or more statement result panels for the same connection
*/
public class ConnectionPanel extends PluginPanel {
   
   //
   // PROPERTIES
   //
   private String name;
   private DHLPConnectionPool pool;
   private int statementCount;
   
   private JToolBar toolBar;
   private JCheckBox followBufferCheck;
   private JTabbedPane executionPanel;
   private JPanel infoPanel;
   private JTextField infoText;
   private Component defaultFocusComponent;
   
   //
   // CONSTRUCTORS
   //
   public ConnectionPanel(DHLPConnectionPool pool) {
      super();
      this.pool = pool;
      initialize();
   }
   
   //
   // OVERRIDES
   //
   
   //
   // PUBLIC METHODS
   //
   
   /**
   * Return the connection name
   */
   public String getName() {
      return pool.getUser()+" @ "+pool.getURL();
   }

   /**
   * Return the connection pool
   */
   public DHLPConnectionPool getPool() { return this.pool; }

   /**
   * Add a sql statement tab
   */
   public void addStatement(String sql) {
      StatementPanel panel = new StatementPanel(pool, sql);
      executionPanel.addTab("Statement"+(++statementCount), panel);
      executionPanel.setSelectedComponent(panel);
   }

   /**
   * Add an object description tab
   */
   public void addDescription(JDBCObject object) {
      DescriptionPanel panel = new DescriptionPanel(pool, object);
      executionPanel.addTab(object.getName()+(++statementCount), panel);
      executionPanel.setSelectedComponent(panel);
   }
   
   /**
   * Add a query execution plan tab
   */
   public void addQueryPlan(String sql) {
      QueryPlanPanel panel = new QueryPlanPanel(pool, sql);
      executionPanel.addTab("Plan"+(++statementCount), panel);
      executionPanel.setSelectedComponent(panel);
   }

   /**
   * Execute the current sql statement
   */
   public void execute() {
      // Execute the current tabs statement
      SQLPanel panel = (SQLPanel)executionPanel.getSelectedComponent();
      if ( panel == null ) return;
      panel.execute();
   }
   
   /**
   * Fetch more rows from the current sql statement
   */
   public void fetch() {
   }
   
   /**
   * Cancel the currently running sql statement
   */
   public void cancel() {
      SQLPanel panel = (SQLPanel)executionPanel.getSelectedComponent();
      if ( panel == null ) return;
      panel.cancel();
   }
   
   /**
   * Close the current tab
   */
   public void close() {
      SQLPanel panel = (SQLPanel)executionPanel.getSelectedComponent();
      close(panel);
      if ( executionPanel.getTabCount() == 0 ) closeAll();
   }
   
   /**
   * Commit transactions on this connection
   */
   public void commit() {
      SQLPanel panel = (SQLPanel)executionPanel.getSelectedComponent();
      if ( panel == null ) return;
      panel.commit();
   }
   
   /**
   * Rollback transactions on this connection
   */
   public void rollback() {
      SQLPanel panel = (SQLPanel)executionPanel.getSelectedComponent();
      if ( panel == null ) return;
      panel.rollback();
   }
   
   /**
   * Close all statement panels
   */
   public void closeAll() {
      // Close all the statement panels
      closeAllTabs();
      // Close all connections in the pool
      pool.close();
      // Remove from the dock
      removePluginPanel();
   }
   
   //
   // PRIVATE METHODS
   //

   /**
   * Initialize variables and layout
   */
   private void initialize() {
      // Some frame options
      layoutComponents();
   }

   /**
   * Create and layout components
   */
   private void layoutComponents() {
      // Create the tool bar
      toolBar = new JToolBar();
      toolBar.setFloatable(false);
      toolBar.setRollover(true);
      toolBar.addSeparator();
      (toolBar.add(ExecuteAction)).setMnemonic(KeyEvent.VK_X);
      (toolBar.add(CommitAction)).setMnemonic(KeyEvent.VK_T);
      (toolBar.add(RollbackAction)).setMnemonic(KeyEvent.VK_R);
      (toolBar.add(CancelAction)).setMnemonic(KeyEvent.VK_C);
      toolBar.addSeparator();
      (toolBar.add(CloseAction)).setMnemonic(KeyEvent.VK_L);
      (toolBar.add(CloseAllAction)).setMnemonic(KeyEvent.VK_A);
      for ( int i=0; ; i++ ) {
         Component c = toolBar.getComponentAtIndex(i);
         if ( c == null ) break;
         c.setFont(c.getFont().deriveFont(10.0f));
      }
      // Create the follow buffer checkbox
//      followBufferCheck = new JCheckBox("Follow Buffer", showWithBuffer());
//      followBufferCheck.setFont(followBufferCheck.getFont().deriveFont(10.0f));
      defaultFocusComponent = followBufferCheck;
      // Create the tabbed pane; that's it i guess
      executionPanel = new JTabbedPane();
      // Create the status components
      infoText = new JTextField(getName());
      infoText.setEditable(false);
      infoText.setFont(infoText.getFont().deriveFont(10.0f));
      infoPanel = new JPanel();
      infoPanel.setLayout(new BorderLayout());
      infoPanel.add(BorderLayout.WEST, infoText);
//      infoPanel.add(BorderLayout.CENTER, followBufferCheck);
      infoPanel.add(BorderLayout.EAST, toolBar);
      // Put it on the content pane
      setLayout(new BorderLayout());
      add(executionPanel, BorderLayout.CENTER);
      add(infoPanel, BorderLayout.SOUTH);
   }

   /**
   * Close all the tabs in this panel
   */
   private void closeAllTabs() {
      int tabCount = executionPanel.getTabCount();
      for ( int i=0; i<tabCount; i++ ) {
         SQLPanel panel = (SQLPanel)executionPanel.getSelectedComponent();
         if ( panel == null ) break;
         close(panel);
      }
   }

   /**
   * Close the given sql panel
   */
   private void close(SQLPanel panel) {
      if ( panel == null ) return;
      panel.close();
      executionPanel.remove(panel);
   }
   
   @Override
   public String getPanelName() {
      return "SQL Results";
   }

   @Override
   public void pluginPanelRemoved() {
      // Close all the statement panels
      closeAllTabs();
      // Close all connections in the pool
      pool.close();
   }

   @Override
   public Component getFocusComponent() {
      return defaultFocusComponent;
   }

   //
   // Define actions for the toolbar
   //

   private Action CloseAction = new AbstractAction("Close") {
      public void actionPerformed(ActionEvent e) {
         close();
      }
   };

   private Action CancelAction = new AbstractAction("Cancel") {
      public void actionPerformed(ActionEvent e) {
         cancel();
      }
   };

   private Action CloseAllAction = new AbstractAction("Close All") {
      public void actionPerformed(ActionEvent e) {
         closeAll();
      }
   };
   
   private Action ExecuteAction = new AbstractAction("Execute") {
      public void actionPerformed(ActionEvent e) {
         execute();
      }
   };

   private Action CommitAction = new AbstractAction("Commit") {
      public void actionPerformed(ActionEvent e) {
         commit();
      }
   };

   private Action RollbackAction = new AbstractAction("Rollback") {
      public void actionPerformed(ActionEvent e) {
         rollback();
      }
   };

   //
   // STATIC METHODS
   //
   
}
