package net.contrapt.dhlp.common;
//
//
//

/**
* An exception in the dhlp system
* 
*/
public class DHLPException extends Exception {
   
   //
   // PROPERTIES
   //
   private Exception nested;
   
   //
   // CONSTRUCTORS
   //

   /**
   * Pass a nested exception
   */
   public DHLPException(Exception e) {
      super(e.toString());
      nested = e;
   }
   
   /**
   * Pass a string message
   */
   public DHLPException(String s) {
      super(s);
   }
   
   //
   // PUBLIC METHODS
   //
   
   //
   // PRIVATE
   //
   
}

