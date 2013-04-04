package net.contrapt.dhlp.common;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
* Represents a database view; retrieving the text for the view will be
* database specific
*/
public class JDBCView extends JDBCObject {
   
   //
   // Properties
   //
   protected Map<String,Column> columns = new TreeMap<String,Column>();
   
   //
   // Constructors
   //
   public JDBCView(ResultSet row) throws SQLException {
      super(
         row.getString(CATALOG_COLUMN),
         row.getString(SCHEMA_COLUMN), 
         row.getString(NAME_COLUMN),
         row.getString(TYPE_COLUMN)
      );
   }

   //
   // Methods
   //
   public void addColumns(ResultSet rows) throws SQLException {
      columns.clear();
      while ( rows.next() ) {
         Column c = new Column();
         c.name = rows.getString(4);
         c.type = rows.getString(6);
         c.size = rows.getInt(7);
         c.precision = rows.getInt(9);
         c.comment = rows.getString(12);
         c.defaultValue = rows.getString(13);
         if ( rows.getString(18) == null || rows.getString(18).equals("") ) c.nullable = null;
         else c.nullable = ( rows.getString(18).equals("NO") ) ? false : true;
         columns.put(c.name, c);
      }
   }

   public Collection<Column> getColumns() {
      return columns.values();
   }

   @Override
   public String describeAsString() {
      StringBuilder buf = new StringBuilder(this.toString());
      for ( Column c : columns.values() ) {
         buf.append("\n   "+c);
      }
      return buf.toString();
   }

   /**
   * Stores a column description
   */
   public class Column {
      String name;
      String type;
      int size;
      int precision;
      Boolean nullable; // null means we don't know
      String comment;
      String defaultValue;
      
      @Override
      public String toString() {
         return name+" "+type+"("+size+","+precision+") "+(nullable?"NULL":"NOT-NULL")+((defaultValue==null)?"":" DEFAULT:"+defaultValue)+
            " "+((comment==null)?"":"("+comment+")");
      }
   }

}
