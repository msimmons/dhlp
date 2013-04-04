package net.contrapt.dhlp.jedit;

import java.util.Set;
import java.util.HashSet;

import net.contrapt.jeditutil.CompletionService;
import net.contrapt.jeditutil.CompletionSelector;

import org.gjt.sp.jedit.View;

import net.contrapt.dhlp.common.DHLPException;

/**
* Provide code completion suggestions for database objects
*/
public class DatabaseCompletionProvider implements CompletionService {

   private static Set<String> modes = new HashSet<String>();
   static {
      modes.add("transact-sql");
      modes.add("pl-sql");
   }
   private static DatabaseCompletionProvider INSTANCE;

   public static DatabaseCompletionProvider getInstance() {
      if ( INSTANCE==null ) INSTANCE = new DatabaseCompletionProvider();
      return INSTANCE;
   }

   public boolean supportsMode(String mode) {
      return modes.contains(mode);
   }

   public CompletionSelector getCompletionSelector(View view) {
      try {
         return DHLPlugin.getInstance().getCompletionSelector(view);
      }
      catch (DHLPException e) {
         return null;
      }
   }

}
