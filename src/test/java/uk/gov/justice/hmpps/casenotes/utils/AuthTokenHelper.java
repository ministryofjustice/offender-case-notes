package uk.gov.justice.hmpps.casenotes.utils;

import org.springframework.stereotype.Component;

import java.util.Map;

import static uk.gov.justice.hmpps.casenotes.utils.AuthTokenHelper.AuthToken.API_TEST_USER;
import static uk.gov.justice.hmpps.casenotes.utils.AuthTokenHelper.AuthToken.SECURE_CASENOTE_USER;

@Component
public class AuthTokenHelper {

    private final Map<AuthToken, String> tokens;

    public enum AuthToken {
        API_TEST_USER,
        SECURE_CASENOTE_USER
    }

    public AuthTokenHelper() {
        tokens = Map.of(
            API_TEST_USER, "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE4NTg2NTg2NDMsImludGVybmFsVXNlciI6dHJ1ZSwidXNlcl9uYW1lIjoiQVBJX1RFU1RfVVNFUiIsImp0aSI6ImQyZDU5MTM0LWJhZGMtNDIxZS05OTI0LTAyZDU3YzhlZDEwZiIsImNsaWVudF9pZCI6ImVsaXRlMmFwaWNsaWVudCIsInNjb3BlIjpbInJlYWQiXX0.UmwOWECCnp6yDenn6pnQ0uM-Gw3DRkwBLGb5L-jrDW1cJTR5q5ASyv1sQ1QNTB0Xk4vlsSl9aNZXaMaAIkRadugmd83Nr5Q5kgFD29lG9sOvBukh2Py7nwzIzoU_pToMEJSKIl2c4UqaaQxgXqgI6F2ex2-W_TtyBwLmKIBwGmo0_KeqFpmZXivNPyUDu7OD61kflofzmliZl6Igen7O3WS5Q0lyChiIz9IGDnkngVKoCfZTBdFz4OAD98hmNi3Rxwzcd2ocFLSvYRZKjAR60uHcge2GtCoYChnNYbl_HSW1TXw8V-gPZH3eR1H_HfdrGnZRLyxiIxHXgZ3QxuW6yw",
            SECURE_CASENOTE_USER, "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJTRUNVUkVfQ0FTRU5PVEVfVVNFUiIsInNjb3BlIjpbInJlYWQiXSwiYXV0aF9zb3VyY2UiOiJub21pcyIsImV4cCI6MTg4MTA5MTY5MywiYXV0aG9yaXRpZXMiOlsiUk9MRV9WSUVXX1NFTlNJVElWRV9DQVNFX05PVEVTIiwiUk9MRV9BRERfU0VOU0lUSVZFX0NBU0VfTk9URVMiXSwianRpIjoiMGZjOTc1NTYtMTgyOC00N2YyLWFmNmItMjRkMzU2MWU0ODljIiwiY2xpZW50X2lkIjoiZWxpdGUyYXBpY2xpZW50In0.aG7TavQwYgulHoPcVEtk8pmbhVdcPNgAhfUuoJ9tsYr4lf0C0h1SIjnfZfGBOsxNsxDCErAM_cuZO12ufYwq0LOV-qQCDoLDk8242mqwI3PQqI056xovg3_DQjN21rNwl_mAA0rdHTU5hfQ5d_inHsz2nxS00rn8Dd8WYrjSjHLqC1LNHgmaMeuRdkSSbnuFd3H3bGtGOHeGsrSPaMpZfiq-10I9HYAA7HgXxovdzIjq70s1a0_gTJKgOWriNgZkfWGWeyBoI3p-7vR9X1oXvr9Qo2M7emrvoOsxKZhAPE3OIwO_BuTGkrxkCAgkBFhEJO2qrgQdIfvF7P9V8cx6rw"
        );
    }

    public String getToken(final AuthToken clientId) {
        return tokens.get(clientId);
    }

}
