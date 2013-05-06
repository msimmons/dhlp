package net.contrapt.dhlp.common;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
* Represent a database synonym; each database will have a different way
* of resolving a synonym
*/
public class JDBCSynonym extends JDBCTable {
   
   //
   // Properties
   //

   //
   // Constructors
   //
   public JDBCSynonym(ResultSet row) {
      super(row);
   }

   //
   // Methods
   //

}
