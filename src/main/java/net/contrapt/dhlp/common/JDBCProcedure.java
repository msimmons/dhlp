package net.contrapt.dhlp.common;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

/**
* Represent a database Procedure
*/
public class JDBCProcedure extends JDBCObject {
   
   //
   // Properties
   //
   private List<Column> columns = new ArrayList<Column>();
   
   //
   // Constructors
   //
   public JDBCProcedure(ResultSet row) throws SQLException {
      super(
         row.getString(CATALOG_COLUMN),
         row.getString(SCHEMA_COLUMN),
         row.getString(NAME_COLUMN),
         "PROCEDURE"
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
         c.type = rows.getString(7);
         switch ( rows.getInt(5) ) {
            case DatabaseMetaData.procedureColumnIn:
               c.inout = ColumnType.IN;
               break;
            case DatabaseMetaData.procedureColumnOut:
               c.inout = ColumnType.OUT;
               break;
            case DatabaseMetaData.procedureColumnInOut:
               c.inout = ColumnType.INOUT;
               break;
            case DatabaseMetaData.procedureColumnReturn:
               c.inout = ColumnType.RETURN;
               break;
            case DatabaseMetaData.procedureColumnResult:
               c.inout = ColumnType.RESULT;
               break;
            default:
               c.inout = ColumnType.UNKNOWN;
         }
         c.size = rows.getInt(9);
         c.precision = rows.getInt(8);
         c.comment = rows.getString(13);
         if ( rows.getInt(12) == DatabaseMetaData.procedureNoNulls ) c.nullable = true;
         else if ( rows.getInt(12) == DatabaseMetaData.procedureNullable ) c.nullable = false;
         else c.nullable = false;
         columns.add(c);
      }
   }

   public Collection<Column> getColumns() {
      return columns;
   }

   @Override
   public String describeAsString() {
      StringBuilder buf = new StringBuilder(this.toString());
      for ( Column c : columns ) {
         buf.append("\n   "+c);
      }
      return buf.toString();
   }

   enum ColumnType {
      IN,
      OUT,
      INOUT,
      RETURN,
      RESULT,
      UNKNOWN;
   }

   /**
   * Stores a column description
   */
   public class Column {
      String name;
      String type;
      ColumnType inout;
      int size;
      int precision;
      Boolean nullable; // null means we don't know
      String comment;
      
      @Override
      public String toString() {
         return name+" "+type+"("+size+","+precision+") "+inout+" "+(nullable?"NULL":"NOT-NULL")+
            " "+((comment==null)?"":"("+comment+")");
      }
   }

}
