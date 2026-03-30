package edu.cmu.bookstore.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class ForwardingClient {

    private final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    private String backendBaseUrl;

    public ForwardingClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<String> get(String path) {
        return exchange(path, HttpMethod.GET, null);
    }

    public ResponseEntity<String> post(String path, String body) {
        return exchange(path, HttpMethod.POST, body);
    }

    public ResponseEntity<String> put(String path, String body) {
        return exchange(path, HttpMethod.PUT, body);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, String body) {
        URI uri = URI.create(backendBaseUrl + path);

        HttpHeaders headers = new HttpHeaders();
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, method, entity, String.class);
            return ResponseEntity.status(response.getStatusCode())
                    .headers(copyResponseHeaders(response.getHeaders()))
                    .body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(copyResponseHeaders(ex.getResponseHeaders()))
                    .body(ex.getResponseBodyAsString());
        }
    }

    private HttpHeaders copyResponseHeaders(HttpHeaders source) {
        HttpHeaders target = new HttpHeaders();
        if (source == null) {
            return target;
        }

        if (source.getLocation() != null) {
            target.setLocation(source.getLocation());
        }
        if (source.getContentType() != null) {
            target.setContentType(source.getContentType());
        }

        return target;
    }
}