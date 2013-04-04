package net.contrapt.dhlp.model;

import net.contrapt.jeditutil.model.BaseModel;
import org.codehaus.jackson.annotate.JsonProperty;

public class DriverData extends BaseModel {

   @JsonProperty
   private String name;

   @JsonProperty
   private String className;

   @JsonProperty
   private String jarFile;

   public String getClassName() {
      return className;
   }

   public String getJarFile() {
      return jarFile;
   }
   public String getName() {
      return name;
   }

}