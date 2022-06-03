package application.utility;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Created By hiepnd
 * Date: 29/03/2021
 * Time: 10:30 AM
 * Contact me via mail hiepnd@vnpt-technology.vn
 */
@Component
public class ApiRequester {

    public ResponseEntity<String> sendGetRequest(String token, final String url) {

        try {
            final RestTemplate restTemplate = new RestTemplate();

            final HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            final HttpEntity<String> entity =
                    new HttpEntity<String>("parameters", headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            return response;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public ResponseEntity<String> sendGetRequestForRefreshToken(final String url) {

        try {
            final RestTemplate restTemplate = new RestTemplate();

            final HttpHeaders headers = new HttpHeaders();
            final HttpEntity<String> entity =
                    new HttpEntity<String>("parameters", headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return response;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
