package net.contrapt.dhlp.model;

import net.contrapt.jeditutil.model.BaseModel;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationData extends BaseModel {

   @JsonProperty
   private boolean autocommit = false;

   @JsonProperty
   private int fetchLimit = 5000;

   private List<DriverData> drivers = new ArrayList<DriverData>();

   private List<ConnectionData> connections = new ArrayList<ConnectionData>();

   public boolean isAutocommit() {
      return autocommit;
   }

   public List<ConnectionData> getConnections() {
      return connections;
   }

   public List<DriverData> getDrivers() {
      return drivers;
   }

   public int getFetchLimit() {
      return fetchLimit;
   }

}