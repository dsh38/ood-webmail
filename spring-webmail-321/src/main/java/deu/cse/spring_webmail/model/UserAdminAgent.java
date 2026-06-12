package deu.cse.spring_webmail.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import lombok.extern.slf4j.Slf4j;

/**
 * Apache James 3.9.0 JMX 기반 사용자 관리 Agent
 */
@Slf4j
public class UserAdminAgent {

    private String server;
    private int port;
    private JMXConnector jmxc = null;
    private MBeanServerConnection mbsc = null;
    private ObjectName mbeanName = null;
    private boolean isConnected = false;
    private String ROOT_ID;        // JMX ID
    private String ROOT_PASSWORD;  // JMX Password
    private String ADMIN_ID;
    private String defaultDomain;
    private String cwd;

    public UserAdminAgent() {
    }

    public UserAdminAgent(String server, int port, String cwd,
            String root_id, String root_pass, String admin_id, String defaultDomain) {
        log.debug("UserAdminAgent (JMX) created: server = {}, port = {}, defaultDomain = {}", server, port, defaultDomain);
        this.server = server;
        this.port = port;
        this.cwd = cwd;
        this.ROOT_ID = root_id;
        this.ROOT_PASSWORD = root_pass;
        this.ADMIN_ID = admin_id;
        this.defaultDomain = defaultDomain;

        this.isConnected = connect();
    }

    private boolean connect() {
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + server + ":" + port + "/jmxrmi");
            
            Map<String, Object> env = null;
            // JMX ID 및 Password가 존재하면 JMX 인증 설정
            if (ROOT_ID != null && !ROOT_ID.trim().isEmpty() && ROOT_PASSWORD != null && !ROOT_PASSWORD.trim().isEmpty()) {
                env = new HashMap<>();
                env.put(JMXConnector.CREDENTIALS, new String[]{ROOT_ID, ROOT_PASSWORD});
                log.debug("Using JMX credentials for connection.");
            }

            this.jmxc = JMXConnectorFactory.connect(url, env);
            this.mbsc = jmxc.getMBeanServerConnection();
            this.mbeanName = new ObjectName("org.apache.james:type=component,name=usersrepository");
            log.debug("JMX MBean connection established successfully.");
            return true;
        } catch (Exception e) {
            log.error("JMX Connection failed: {}", e.getMessage());
            return false;
        }
    }

    private String formatUsername(String username) {
        if (username != null && !username.contains("@") && defaultDomain != null && !defaultDomain.isEmpty()) {
            return username + "@" + defaultDomain;
        }
        return username;
    }

    public boolean addUser(String userId, String password) {
        if (!isConnected) {
            log.error("addUser failed: JMX is not connected.");
            return false;
        }

        try {
            String formattedId = formatUsername(userId);
            mbsc.invoke(mbeanName, "addUser", 
                    new Object[]{formattedId, password}, 
                    new String[]{"java.lang.String", "java.lang.String"});
            log.info("User {} added successfully via JMX.", formattedId);
            return true;
        } catch (Exception ex) {
            log.error("addUser failed: {}", ex.getMessage());
            return false;
        } finally {
            quit();
        }
    }

    public List<String> getUserList() {
        List<String> userList = new LinkedList<>();
        if (!isConnected) {
            log.error("getUserList failed: JMX is not connected.");
            return userList;
        }

        try {
            Object result = mbsc.invoke(mbeanName, "listAllUsers", null, null);
            if (result != null) {
                if (result instanceof String[]) {
                    for (String user : (String[]) result) {
                        if (!user.equals(ADMIN_ID) && !user.startsWith(ADMIN_ID + "@")) {
                            userList.add(user);
                        }
                    }
                } else if (result instanceof java.util.Collection) {
                    for (Object item : (java.util.Collection<?>) result) {
                        String user = item.toString();
                        if (!user.equals(ADMIN_ID) && !user.startsWith(ADMIN_ID + "@")) {
                            userList.add(user);
                        }
                    }
                } else if (result instanceof java.util.Iterator) {
                    java.util.Iterator<?> it = (java.util.Iterator<?>) result;
                    while (it.hasNext()) {
                        String user = it.next().toString();
                        if (!user.equals(ADMIN_ID) && !user.startsWith(ADMIN_ID + "@")) {
                            userList.add(user);
                        }
                    }
                } else {
                    log.warn("getUserList: Unknown result type returned from JMX: {}", result.getClass().getName());
                }
            }
        } catch (Exception ex) {
            log.error("getUserList failed: {}", ex.getMessage(), ex);
        } finally {
            quit();
        }
        return userList;
    }

    public boolean deleteUsers(String[] userList) {
        if (!isConnected) {
            log.error("deleteUsers failed: JMX is not connected.");
            return false;
        }

        boolean status = false;
        try {
            for (String userId : userList) {
                String formattedId = formatUsername(userId);
                mbsc.invoke(mbeanName, "deleteUser", 
                        new Object[]{formattedId}, 
                        new String[]{"java.lang.String"});
                log.info("User {} deleted successfully via JMX.", formattedId);
                status = true;
            }
        } catch (Exception ex) {
            log.error("deleteUsers failed: {}", ex.getMessage());
            status = false;
        } finally {
            quit();
        }
        return status;
    }

    public boolean verify(String userId) {
        if (!isConnected) {
            log.error("verify failed: JMX is not connected.");
            return false;
        }

        boolean exists = false;
        try {
            String formattedId = formatUsername(userId);
            exists = (Boolean) mbsc.invoke(mbeanName, "verifyExists", 
                    new Object[]{formattedId}, 
                    new String[]{"java.lang.String"});
        } catch (Exception ex) {
            log.error("verify failed: {}", ex.getMessage());
        } finally {
            quit();
        }
        return exists;
    }

    public boolean quit() {
        try {
            if (jmxc != null) {
                jmxc.close();
            }
            isConnected = false;
            return true;
        } catch (IOException ex) {
            log.error("quit (JMX Close) failed: {}", ex.getMessage());
            return false;
        }
    }
}
