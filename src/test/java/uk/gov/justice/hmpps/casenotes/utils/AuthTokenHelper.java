package uk.gov.justice.hmpps.casenotes.utils;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthTokenHelper {

    private final Map<String, String> tokens = new HashMap<>();
    private String currentToken;

    public enum AuthToken {
        NORMAL_USER,
        }


    public AuthTokenHelper(final JwtAuthenticationHelper jwtAuthenticationHelper) {
        tokens.put(String.valueOf(AuthToken.NORMAL_USER), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE4NTg2NTg2NDMsImludGVybmFsVXNlciI6dHJ1ZSwidXNlcl9uYW1lIjoiQVBJX1RFU1RfVVNFUiIsImp0aSI6ImQyZDU5MTM0LWJhZGMtNDIxZS05OTI0LTAyZDU3YzhlZDEwZiIsImNsaWVudF9pZCI6ImVsaXRlMmFwaWNsaWVudCIsInNjb3BlIjpbInJlYWQiXX0.UmwOWECCnp6yDenn6pnQ0uM-Gw3DRkwBLGb5L-jrDW1cJTR5q5ASyv1sQ1QNTB0Xk4vlsSl9aNZXaMaAIkRadugmd83Nr5Q5kgFD29lG9sOvBukh2Py7nwzIzoU_pToMEJSKIl2c4UqaaQxgXqgI6F2ex2-W_TtyBwLmKIBwGmo0_KeqFpmZXivNPyUDu7OD61kflofzmliZl6Igen7O3WS5Q0lyChiIz9IGDnkngVKoCfZTBdFz4OAD98hmNi3Rxwzcd2ocFLSvYRZKjAR60uHcge2GtCoYChnNYbl_HSW1TXw8V-gPZH3eR1H_HfdrGnZRLyxiIxHXgZ3QxuW6yw");
    }

    public String getToken() {
        return currentToken;
    }

    public void setToken(final AuthToken clientId) {
        this.currentToken = getToken(clientId);
    }

    public String getToken(final AuthToken clientId) {
        return tokens.get(String.valueOf(clientId));
    }

}
