package net.contrapt.dhlp.common;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
* Represent a database table
*/
public class JDBCTable extends JDBCView {
   
   //
   // Properties
   //
   private PrimaryKey pk;
   private Map<String,Index> indices = new TreeMap<String,Index>();
   private Map<String,Constraint> children = new TreeMap<String,Constraint>();
   private Map<String,Constraint> parents = new TreeMap<String,Constraint>();
   
   
   //
   // Constructors
   //
   public JDBCTable(ResultSet row) throws SQLException {
      super(row);
   }

   //
   // Methods
   //
   public void addPrimaryKey(ResultSet rows) throws SQLException {
      pk = new PrimaryKey();
      while ( rows.next() ) {
         if (pk.name == null) pk.name = rows.getString(6);
         int sequence = rows.getInt(5);
         String name = rows.getString(4);
         Column column = columns.get(name);
         if ( column == null ) continue;
         while ( pk.columns.size() < sequence ) pk.columns.add(null);
         pk.columns.set(sequence-1, column);
      }
   }

   public PrimaryKey getPrimaryKey() {
      return pk;
   }
   
   public void addIndices(ResultSet rows) throws SQLException {
      indices.clear();
      while ( rows.next() ) {
         String name = rows.getString(6);
         if ( name == null ) continue;
         Index i = indices.get(name);
         if ( i == null ) {
            i = new Index();
            i.name = name;
            i.unique = !rows.getBoolean(4);
            indices.put(i.name, i);
         }
         addIndexColumn(rows, i);
      }
   }

   public Collection<Index> getIndices() {
      return indices.values();
   }
   
   private void addIndexColumn(ResultSet row, Index i) throws SQLException {
      String name = row.getString(9);
      Column column = columns.get(name);
      if ( column == null ) return;
      int sequence = row.getInt(8);
      while ( i.columns.size() < sequence ) i.columns.add(null);
      i.columns.set(sequence-1, column);
   }
   
   public void addChildren(ResultSet rows) throws SQLException {
      children.clear();
      while ( rows.next() ) {
         String name = rows.getString(12);
         if ( name == null ) continue;
         Constraint c = children.get(name);
         if ( c == null ) {
            c = new Constraint();
            c.name = name;
            c.table = rows.getString(7);
            children.put(c.name, c);
         }
         addConstraintColumn(rows, c);
      }
   }

   public Collection<Constraint> getChildren() {
      return children.values();
   }

   public void addParents(ResultSet rows) throws SQLException {
      parents.clear();
      while ( rows.next() ) {
         String name = rows.getString(12);
         if ( name == null ) continue;
         Constraint c = parents.get(name);
         if ( c == null ) {
            c = new Constraint();
            c.name = name;
            c.table = rows.getString(3);
            parents.put(c.name, c);
         }
         addConstraintColumn(rows, c);
      }
   }

   public Collection<Constraint> getParents() {
      return parents.values();
   }

   private void addConstraintColumn(ResultSet row, Constraint c) throws SQLException {
      String column = row.getString(8);
      if ( column == null ) return;
      int sequence = row.getInt(9);
      while ( c.columns.size() < sequence ) c.columns.add(null);
      c.columns.set(sequence-1, column);
   }
   
   @Override
   public String describeAsString() {
      StringBuilder buf = new StringBuilder(this.toString());
      buf.append("\n PK: "+pk);
      for ( Index i : indices.values() ) {
         buf.append("\n INDEX: "+i);
      }
      buf.append("\nChildren:");
      for ( Constraint c : children.values() ) {
         buf.append("\n   "+c);
      }
      buf.append("\nParents:");
      for ( Constraint c : parents.values() ) {
         buf.append("\n   "+c);
      }
      return buf.toString();
   }

   public class PrimaryKey {
      String name;
      List<Column> columns = new ArrayList<Column>();
      
      @Override
      public String toString() {
         StringBuilder buf = new StringBuilder(name+" (");
         for ( Column c : columns ) buf.append(c.name+",");
         if ( buf.charAt(buf.length()-1) == ',' ) buf.setCharAt(buf.length()-1, ')');
         else buf.append(")");
         return buf.toString();
      }
   }

   public class Index {
      String name;
      boolean unique;
      List<Column> columns = new ArrayList<Column>();

      public String toString() {
         StringBuilder buf = new StringBuilder(((unique)?"Unique ":"")+name+" (");
         for ( Column c : columns ) buf.append(c.name+",");
         if ( buf.charAt(buf.length()-1) == ',' ) buf.setCharAt(buf.length()-1, ')');
         else buf.append(")");
         return buf.toString();
      }
   }

   public class Constraint {
      String name;
      String table;
      List<String> columns = new ArrayList<String>();
      
      public String toString() {
         StringBuilder buf = new StringBuilder(name+" -> "+table+" (");
         for ( String c : columns ) buf.append(c+",");
         if ( buf.charAt(buf.length()-1) == ',' ) buf.setCharAt(buf.length()-1, ')');
         else buf.append(")");
         return buf.toString();
      }
   }

}
