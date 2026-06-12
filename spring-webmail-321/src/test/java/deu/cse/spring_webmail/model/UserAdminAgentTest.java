package deu.cse.spring_webmail.model;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserAdminAgentTest {

    private static final String HOST = "127.0.0.1";
    private static final int JMX_PORT = 9999;

    private boolean isJmxServerAvailable() {
        try (Socket socket = new Socket(HOST, JMX_PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    @DisplayName("JMX 원격 접속 차단 상태 시 예외 제어 테스트")
    void testConnectionFailureHandling() {
        // 일부러 잘못된 포트를 전달하여 연결 예외가 발생하는지 검증
        UserAdminAgent agent = new UserAdminAgent(HOST, 11111, "target/test-cwd", "james-admin", "changeme", "admin", "localhost");
        
        // 연결되지 않은 상태이므로 CRUD API가 false 또는 빈 결과를 리턴하는지 확인
        assertFalse(agent.addUser("test", "pwd"));
        assertFalse(agent.deleteUsers(new String[]{"test"}));
        assertTrue(agent.getUserList().isEmpty());
    }

    @Test
    @DisplayName("실제 James 3.9.0 서버 구동 상태 연동 테스트")
    void testRealJmxOperations() {
        if (!isJmxServerAvailable()) {
            System.out.println("James JMX Server (port 9999) is not running. Skipping real integration tests.");
            return;
        }

        // 서버가 켜진 상태라면 JMX 사용자 추가, 목록 조회, 삭제 검증을 진행합니다.
        UserAdminAgent agent = new UserAdminAgent(HOST, JMX_PORT, "target/test-cwd", "", "", "admin", "localhost");
        
        String testUser = "jmx_test_user";
        String testPassword = "test_password";
        
        // 1. 기존 유저가 있으면 지운다
        agent.deleteUsers(new String[]{testUser});
        
        // 2. 유저 추가
        boolean added = agent.addUser(testUser, testPassword);
        assertTrue(added, "사용자 등록에 성공해야 합니다.");
        
        // 3. 유저 확인
        UserAdminAgent agentVerify = new UserAdminAgent(HOST, JMX_PORT, "target/test-cwd", "", "", "admin", "localhost");
        boolean exists = agentVerify.verify(testUser);
        assertTrue(exists, "사용자가 존재해야 합니다.");
        
        // 4. 유저 목록 조회 및 포함 여부 확인
        UserAdminAgent agentList = new UserAdminAgent(HOST, JMX_PORT, "target/test-cwd", "", "", "admin", "localhost");
        List<String> users = agentList.getUserList();
        assertTrue(users.contains(testUser + "@localhost"), "목록에 테스트 유저가 포함되어 있어야 합니다.");
        
        // 5. 유저 제거
        UserAdminAgent agentDelete = new UserAdminAgent(HOST, JMX_PORT, "target/test-cwd", "", "", "admin", "localhost");
        boolean deleted = agentDelete.deleteUsers(new String[]{testUser});
        assertTrue(deleted, "사용자 삭제에 성공해야 합니다.");
    }
}
