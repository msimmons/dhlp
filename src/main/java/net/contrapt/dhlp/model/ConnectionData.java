package net.contrapt.dhlp.model;

import net.contrapt.jeditutil.model.BaseModel;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

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
    private String sshHost;

    @JsonProperty
    private String sshUser;

    @JsonProperty
    private String sshPassword;

    @JsonProperty
    private String sshKeyFile;

    @JsonProperty
    private Integer sshLocalPort;

    @JsonProperty
    private String sshRemoteHost;

    @JsonProperty
    private Integer sshRemotePort;

    @JsonProperty
    private String sshPassphrase;

    @JsonProperty
    private boolean autocommit = false;

    @JsonProperty
    private int fetchLimit;

    @JsonProperty
    private List<String> schema;

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

    public List<String> getSchema() {
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

    public String getSshHost() {
        return sshHost;
    }

    public String getSshUser() {
        return sshUser;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public String getSshKeyFile() {
        return sshKeyFile;
    }

    public Integer getSshLocalPort() {
        return sshLocalPort;
    }

    public String getSshRemoteHost() {
        return sshRemoteHost;
    }

    public Integer getSshRemotePort() {
        return sshRemotePort;
    }

    public String getSshPassphrase() {
        return sshPassphrase;
    }

    public boolean isSsh() {
        return sshHost != null;
    }
}
