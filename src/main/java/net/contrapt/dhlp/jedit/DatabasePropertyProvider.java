package net.contrapt.dhlp.jedit;

import java.util.List;
import java.util.ArrayList;

import org.gjt.sp.jedit.Buffer;

import net.contrapt.jeditutil.DynamicPropertyProvider;
import net.contrapt.jeditutil.DynamicPropertyDescriptor;

/**
* Implement the dynamic property provider service
*/
public class DatabasePropertyProvider extends DynamicPropertyProvider {

   private static DatabasePropertyProvider instance;

   static final List<DynamicPropertyDescriptor> consumed = new ArrayList<DynamicPropertyDescriptor>();
   {
      consumed.add(new DatabasePropertyDescriptor(DHLPlugin.CONNECTION_PROPERTY, "The name of the DHLP connection to use", false));
   }

   /**
   * Private constructor for singleton creation
   */
   private DatabasePropertyProvider() {}

   /**
   * Create and/or return the singleton instance of this provider
   */
   public static DatabasePropertyProvider getInstance() {
      if ( instance == null ) instance = new DatabasePropertyProvider();
      return instance;
   }

   /**
   * Shutdown the singleton
   */
   public static void shutdown() {
      instance = null;
   }

   @Override
   public List<DynamicPropertyDescriptor> getConsumedProperties() {
      return consumed;
   }

   @Override
   protected Object getProvidedProperty(String name, Buffer buffer) {
      return null;
   }

   @Override
   public boolean providesProperty(String name) {
      return false;
   }

   /**
   * Convenience class for property descriptor to fill in the classname
   */
   private static class DatabasePropertyDescriptor extends DynamicPropertyDescriptor {
      
      DatabasePropertyDescriptor(String name, String description, boolean isProvider) {
         super(DatabasePropertyProvider.class, name, description, isProvider);
      }

   }

}
