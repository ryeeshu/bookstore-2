package edu.cmu.bookstore.controller;

import edu.cmu.bookstore.exception.BadRequestException;
import edu.cmu.bookstore.util.ForwardingClient;
import edu.cmu.bookstore.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class WebBffController {

    private final JwtUtil jwtUtil;
    private final ForwardingClient forwardingClient;

    public WebBffController(JwtUtil jwtUtil, ForwardingClient forwardingClient) {
        this.jwtUtil = jwtUtil;
        this.forwardingClient = forwardingClient;
    }

    @PostMapping("/books")
    public ResponseEntity<String> createBook(@RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = false) String body) {
        requireWebClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.post("/books", body);
    }

    @PutMapping("/books/{isbn}")
    public ResponseEntity<String> updateBook(@PathVariable String isbn,
                                             @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = false) String body) {
        requireWebClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.put("/books/" + encodePathSegment(isbn), body);
    }

    @GetMapping("/books/{isbn}")
    public ResponseEntity<String> getBook(@PathVariable String isbn,
                                          @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireWebClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.get("/books/" + encodePathSegment(isbn));
    }

    @GetMapping("/books/isbn/{isbn}")
    public ResponseEntity<String> getBookAlt(@PathVariable String isbn,
                                             @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireWebClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.get("/books/isbn/" + encodePathSegment(isbn));
    }

    @PostMapping("/customers")
    public ResponseEntity<String> createCustomer(@RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @RequestBody(required = false) String body) {
        requireWebClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.post("/customers", body);
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<String> getCustomerById(@PathVariable String id,
                                                  @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireWebClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.get("/customers/" + encodePathSegment(id));
    }

    @GetMapping("/customers")
    public ResponseEntity<String> getCustomerByUserId(@RequestParam("userId") String userId,
                                                      @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireWebClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.get("/customers?userId=" + URLEncoder.encode(userId, StandardCharsets.UTF_8));
    }

    private void requireWebClient(String clientType) {
        if (clientType == null || clientType.trim().isEmpty()) {
            throw new BadRequestException("Missing X-Client-Type header.");
        }

        String normalized = clientType.trim();
        if (!normalized.equalsIgnoreCase("Web")) {
            throw new BadRequestException("Invalid X-Client-Type header.");
        }
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}