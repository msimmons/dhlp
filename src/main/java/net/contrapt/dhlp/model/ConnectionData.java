package net.contrapt.dhlp.model;

import net.contrapt.jeditutil.model.BaseModel;
import org.codehaus.jackson.annotate.JsonProperty;

public class ConnectionData extends BaseModel {

   @JsonProperty
   private String name;

   @JsonProperty
   private String driver;

   @JsonProperty
   private String url;

   @JsonProperty
   private String user;

   @JsonProperty
   private String password;

   @JsonProperty
   private boolean autocommit = false;

   @JsonProperty
   private int fetchLimit;

   @JsonProperty
   private String catalog;

   @JsonProperty
   private String schema;

   public String getCatalog() {
      return catalog;
   }

   public String getDriver() {
      return driver;
   }

   public int getFetchLimit() {
      return fetchLimit;
   }

   public String getName() {
      return name;
   }

   public String getPassword() {
      return password;
   }

   public String getSchema() {
      return schema;
   }

   public String getUrl() {
      return url;
   }

   public String getUser() {
      return user;
   }

   public boolean isAutocommit() {
      return autocommit;
   }

   public void setAutocommit(boolean autocommit) {
      this.autocommit = autocommit;
   }

}
