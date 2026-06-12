package deu.cse.spring_webmail.model;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageParserTest {

    private Session session;
    private MimeMessage message;

    @BeforeEach
    void setUp() throws Exception {
        session = Session.getDefaultInstance(new Properties());
        message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@example.com"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("to@example.com"));
        message.setRecipients(Message.RecipientType.CC, InternetAddress.parse("cc@example.com"));
        message.setSubject("Test Email Subject", "UTF-8");
        message.setSentDate(new Date());
        message.setText("This is a test message body.", "UTF-8");
    }

    @Test
    @DisplayName("기본 이메일 봉투(Envelope) 정보 파싱 테스트")
    void testParseEnvelopeOnly() {
        MessageParser parser = new MessageParser(message, "testuser");
        boolean success = parser.parse(false);

        assertTrue(success, "이메일 헤더 파싱이 성공해야 합니다.");
        assertEquals("sender@example.com", parser.getFromAddress(), "보낸 사람 주소가 일치해야 합니다.");
        
        // getAddresses는 포맷상 뒤에 공백이 존재할 수 있으므로 trim()하여 일치 여부 비교
        assertEquals("to@example.com", parser.getToAddress().trim(), "받는 사람 주소가 일치해야 합니다.");
        assertEquals("cc@example.com", parser.getCcAddress().trim(), "참조 주소가 일치해야 합니다.");
        
        assertEquals("Test Email Subject", parser.getSubject(), "제목이 일치해야 합니다.");
        assertNotNull(parser.getSentDate(), "발송 날짜가 존재해야 합니다.");
        assertNull(parser.getBody(), "본문 파싱을 생략(parseBody=false)했으므로 본문은 null이어야 합니다.");
    }

    @Test
    @DisplayName("이메일 본문 파싱 테스트")
    void testParseWithBody() {
        MessageParser parser = new MessageParser(message, "testuser");
        parser.setDownloadTempDir("target/test-download/");
        boolean success = parser.parse(true);

        assertTrue(success, "이메일 전체 파싱이 성공해야 합니다.");
        assertEquals("sender@example.com", parser.getFromAddress());
        assertEquals("Test Email Subject", parser.getSubject());
        assertNotNull(parser.getBody(), "본문 내용이 채워져야 합니다.");
        assertTrue(parser.getBody().contains("This is a test message body."));
    }
}
