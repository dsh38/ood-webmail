package deu.cse.spring_webmail.model;

import java.io.IOException;
import java.net.Socket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImapAgentTest {

    private static final String HOST = "127.0.0.1";
    private static final int IMAPS_PORT = 993;

    private boolean isImapServerAvailable() {
        try (Socket socket = new Socket(HOST, IMAPS_PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    @DisplayName("IMAPS 연결 실패 시 예외 제어 테스트")
    void testConnectionFailureHandling() {
        ImapAgent agent = new ImapAgent(HOST, "invalid_user", "pwd");
        boolean validated = agent.validate();
        assertFalse(validated, "잘못된 크레덴셜 및 기본 포트 미개방 시 로그인 검증에 실패해야 합니다.");
    }
}
