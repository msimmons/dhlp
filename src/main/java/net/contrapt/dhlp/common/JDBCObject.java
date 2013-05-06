package net.contrapt.dhlp.common;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represent a database object returned by various jdbc database metadata methods
 */
public abstract class JDBCObject {

   //
   // Properties
   //
   public static final int CATALOG_COLUMN = 1;
   public static final int SCHEMA_COLUMN = 2;
   public static final int NAME_COLUMN = 3;
   public static final int TYPE_COLUMN = 4;


   private String catalog;
   private String schema;
   private String name;
   private String type;
   private boolean described = false;

   protected JDBCObject(String catalog, String schema, String name, String type) {
      this.catalog = catalog;
      this.schema = schema;
      this.name = name;
      this.type = type;
   }

   protected JDBCObject(ResultSet row, String type) {
      try {
         this.catalog = row.getString(CATALOG_COLUMN);
         this.schema = row.getString(SCHEMA_COLUMN);
         this.name = row.getString(NAME_COLUMN);
         this.type = type;
      } catch (SQLException e) {
         throw new IllegalStateException("Getting object metadata from " + row, e);
      }
   }

   protected JDBCObject(ResultSet row) {
      try {
         this.catalog = row.getString(CATALOG_COLUMN);
         this.schema = row.getString(SCHEMA_COLUMN);
         this.name = row.getString(NAME_COLUMN);
         this.type = row.getString(TYPE_COLUMN);
      } catch (SQLException e) {
         throw new IllegalStateException("Getting object metadata from " + row, e);
      }
   }

   /**
    * Create the appropriate type of object for a metadata row; these could be synonyms, views or
    * tables or procedures
    */
   public static JDBCObject create(ResultSet row) {
      try {
         String type = row.getString(TYPE_COLUMN);
         if ("VIEW".equals(type)) return new JDBCView(row);
         if ("SYNONYM".equals(type)) return new JDBCSynonym(row);
         if (null == type) return new JDBCProcedure(row);
         else return new JDBCTable(row);
      } catch (SQLException e) {
         throw new IllegalStateException("Error creating object from " + row, e);
      }
   }

   //
   // Methods
   //
   public final String getCatalog() {
      return this.catalog;
   }

   public final String getSchema() {
      return this.schema;
   }

   public final String getName() {
      return this.name;
   }

   public final String getType() {
      return this.type;
   }

   public final boolean isDescribed() {
      return this.described;
   }

   public final void setDescribed() {
      this.described = true;
   }

   public final String getDisplayName() {
      return this.name + " (" + this.type + " " + ((this.catalog == null) ? "" : this.catalog + ".") + this.schema + ")";
   }

   public abstract String describeAsString();

   @Override
   public String toString() {
      //return ((schema==null)? "" : schema)+"."+((catalog==null)? "" : catalog+".")+name+" ("+type+")";
      return name;
   }
}
