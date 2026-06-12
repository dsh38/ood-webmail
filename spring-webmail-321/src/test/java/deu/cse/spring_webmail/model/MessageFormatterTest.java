package deu.cse.spring_webmail.model;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageFormatterTest {

    private Session session;
    private MimeMessage message;

    @BeforeEach
    void setUp() throws Exception {
        session = Session.getDefaultInstance(new Properties());
        message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@example.com"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("to@example.com"));
        message.setSubject("Test Email Subject", "UTF-8");
        message.setSentDate(new Date());
        message.setText("Test Body.", "UTF-8");
    }

    @Test
    @DisplayName("메일 목록 HTML 테이블 포맷터 테스트")
    void testGetMessageTable() {
        MessageFormatter formatter = new MessageFormatter("testuser");
        String tableHtml = formatter.getMessageTable(new Message[]{message});

        assertNotNull(tableHtml);
        assertTrue(tableHtml.contains("<table>"));
        assertTrue(tableHtml.contains("sender@example.com"));
        assertTrue(tableHtml.contains("Test Email Subject"));
        assertTrue(tableHtml.contains("</table>"));
    }

    @Test
    @DisplayName("HTML 특수 문자가 포함된 보낸 사람 이메일 주소의 HTML 이스케이프 테스트")
    void testGetMessageTableWithHtmlSpecialChars() throws Exception {
        MimeMessage msgWithDisplay = new MimeMessage(session);
        msgWithDisplay.setFrom(new InternetAddress("test@example.com", "홍길동", "UTF-8"));
        msgWithDisplay.setRecipients(Message.RecipientType.TO, InternetAddress.parse("to@example.com"));
        msgWithDisplay.setSubject("Test Subject", "UTF-8");
        msgWithDisplay.setSentDate(new Date());
        msgWithDisplay.setText("Body.", "UTF-8");

        MessageFormatter formatter = new MessageFormatter("testuser");
        String tableHtml = formatter.getMessageTable(new Message[]{msgWithDisplay});

        assertNotNull(tableHtml);
        assertTrue(tableHtml.contains("test@example.com"));
        assertFalse(tableHtml.contains("홍길동"));
    }


    @Test
    @DisplayName("메일 상세 보기 HTML 포맷터 테스트")
    void testGetMessage() {
        MessageFormatter formatter = new MessageFormatter("testuser");
        
        // Mockito를 이용한 HttpServletRequest 및 ServletContext 모킹
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletContext context = mock(ServletContext.class);
        when(request.getServletContext()).thenReturn(context);
        when(context.getRealPath(anyString())).thenReturn("target/test-download/");
        
        formatter.setRequest(request);
        
        String messageHtml = formatter.getMessage(message);

        assertNotNull(messageHtml);
        assertTrue(messageHtml.contains("보낸 사람: sender@example.com"));
        assertTrue(messageHtml.contains("받은 사람: to@example.com"));
        assertTrue(messageHtml.contains("Test Body."));
    }
}
