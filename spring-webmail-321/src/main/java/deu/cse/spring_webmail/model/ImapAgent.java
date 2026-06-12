package deu.cse.spring_webmail.model;

import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.util.Properties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * IMAPS (IMAP over SSL/TLS) 기반 메일 조회 Agent
 */
@Slf4j
@NoArgsConstructor
public class ImapAgent {
    @Getter @Setter private String host;
    @Getter @Setter private String userid;
    @Getter @Setter private String password;
    @Getter @Setter private Store store;
    @Getter @Setter private String exceptionType;
    @Getter @Setter private HttpServletRequest request;
    
    @Getter private String sender;
    @Getter private String subject;
    @Getter private String body;
    
    public ImapAgent(String host, String userid, String password) {
        this.host = host;
        this.userid = userid;
        this.password = password;
    }
    
    public boolean validate() {
        boolean status = false;
        try {
            status = connectToStore();
            if (store != null) {
                store.close();
            }
        } catch (Exception ex) {
            log.error("ImapAgent.validate() error : " + ex);
            status = false;
        }
        return status;
    }

    public boolean deleteMessage(int msgid, boolean really_delete) {
        boolean status = false;
        if (!connectToStore()) {
            return status;
        }

        try {
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);

            Message msg = folder.getMessage(msgid);
            msg.setFlag(Flags.Flag.DELETED, really_delete);

            folder.close(true);  // expunge == true
            store.close();
            status = true;
        } catch (Exception ex) {
            log.error("deleteMessage() error: {}", ex.getMessage());
        }
        return status;
    }

    public String getMessageList() {
        String result = "";
        Message[] messages = null;

        if (!connectToStore()) {
            log.error("IMAPS connection failed!");
            return "IMAPS 연결이 되지 않아 메일 목록을 볼 수 없습니다.";
        }

        try {
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            messages = folder.getMessages();
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            folder.fetch(messages, fp);

            MessageFormatter formatter = new MessageFormatter(userid);
            result = formatter.getMessageTable(messages);

            folder.close(true);
            store.close();
        } catch (Exception ex) {
            log.error("ImapAgent.getMessageList() : exception = {}", ex.getMessage());
            result = "ImapAgent.getMessageList() : exception = " + ex.getMessage();
        }
        return result;
    }

    public String getMessage(int n) {
        String result = "IMAPS 서버 연결이 되지 않아 메시지를 볼 수 없습니다.";

        if (!connectToStore()) {
            log.error("IMAPS connection failed!");
            return result;
        }

        try {
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            Message message = folder.getMessage(n);

            MessageFormatter formatter = new MessageFormatter(userid);
            formatter.setRequest(request);
            result = formatter.getMessage(message);
            sender = formatter.getSender();
            subject = formatter.getSubject();
            body = formatter.getBody();

            folder.close(true);
            store.close();
        } catch (Exception ex) {
            log.error("ImapAgent.getMessage() : exception = {}", ex);
            result = "ImapAgent.getMessage() : exception = " + ex;
        }
        return result;
    }

    private boolean connectToStore() {
        boolean status = false;
        Properties props = System.getProperties();
        
        // IMAPS (IMAP over SSL/TLS) 암호화 연결 설정
        props.setProperty("mail.imaps.host", host);
        props.setProperty("mail.imaps.port", "993");
        props.setProperty("mail.imaps.user", userid);
        // 자체 서명 인증서 신뢰 설정
        props.setProperty("mail.imaps.ssl.trust", "*");
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.debug", "false");
        props.setProperty("mail.imaps.debug", "false");

        Session session = Session.getInstance(props);
        session.setDebug(false);

        try {
            store = session.getStore("imaps");
            store.connect(host, 993, userid, password);
            status = true;
        } catch (Exception ex) {
            log.error("connectToStore (IMAPS) 예외: {}", ex.getMessage());
        }
        return status;
    }
}
