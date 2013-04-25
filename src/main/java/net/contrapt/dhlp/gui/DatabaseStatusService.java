package net.contrapt.dhlp.gui;

import net.contrapt.dhlp.common.DHLPController;
import net.contrapt.dhlp.jedit.DHLPlugin;
import net.contrapt.jeditutil.service.BufferStatusService;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.PluginJAR;
import org.gjt.sp.jedit.jEdit;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created with IntelliJ IDEA.
 * User: msimmons
 * Date: 4/24/13
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseStatusService implements BufferStatusService<JComboBox> {

   private static DatabaseStatusService INSTANCE;

   private JComboBox comboBox;

   public static DatabaseStatusService getInstance() {
      if ( INSTANCE == null ) INSTANCE = new DatabaseStatusService();
      return INSTANCE;
   }

   @Override
   public JComboBox getComponent() {
      comboBox = new JComboBox();
      comboBox.setToolTipText("Choose Database Connection");
      comboBox.addItem("");
      for (String name :DHLPController.getInstance().getConnections().keySet()) {
         comboBox.addItem(name);
      }
      comboBox.addActionListener(new AbstractAction() {
         @Override
         public void actionPerformed(ActionEvent e) {
            handleConnectionChosen();
         }
      });
      return comboBox;
   }

   @Override
   public void update(JComboBox c, Buffer buffer, boolean b) {
      String name = buffer.getStringProperty(DHLPlugin.CONNECTION_PROPERTY);
      name = (name==null) ? "" : name;
      setConnection(name);
   }

   @Override
   public PluginJAR getPluginJAR() {
      return DHLPlugin.getInstance().getPluginJAR();
   }

   private void handleConnectionChosen() {
      jEdit.getActiveView().getBuffer().setStringProperty(DHLPlugin.CONNECTION_PROPERTY, (String)comboBox.getModel().getSelectedItem());
   }

   public void setConnection(String name) {
      comboBox.getModel().setSelectedItem(name);
   }
}
