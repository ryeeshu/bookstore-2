package edu.cmu.bookstore.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.cmu.bookstore.exception.BadRequestException;
import edu.cmu.bookstore.util.ForwardingClient;
import edu.cmu.bookstore.util.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class MobileBffController {

    private final JwtUtil jwtUtil;
    private final ForwardingClient forwardingClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MobileBffController(JwtUtil jwtUtil, ForwardingClient forwardingClient) {
        this.jwtUtil = jwtUtil;
        this.forwardingClient = forwardingClient;
    }

    @PostMapping("/books")
    public ResponseEntity<String> createBook(@RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = false) String body) {
        requireMobileClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.post("/books", body);
    }

    @PutMapping("/books/{isbn}")
    public ResponseEntity<String> updateBook(@PathVariable String isbn,
                                             @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = false) String body) {
        requireMobileClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.put("/books/" + encodePathSegment(isbn), body);
    }

    @GetMapping("/books/{isbn}")
    public ResponseEntity<String> getBook(@PathVariable String isbn,
                                          @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireMobileClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        ResponseEntity<String> upstream = forwardingClient.get("/books/" + encodePathSegment(isbn));
        return transformBookResponse(upstream);
    }

    @GetMapping("/books/isbn/{isbn}")
    public ResponseEntity<String> getBookAlt(@PathVariable String isbn,
                                             @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireMobileClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        ResponseEntity<String> upstream = forwardingClient.get("/books/isbn/" + encodePathSegment(isbn));
        return transformBookResponse(upstream);
    }

    @PostMapping("/customers")
    public ResponseEntity<String> createCustomer(@RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @RequestBody(required = false) String body) {
        requireMobileClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        return forwardingClient.post("/customers", body);
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<String> getCustomerById(@PathVariable String id,
                                                  @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireMobileClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        ResponseEntity<String> upstream = forwardingClient.get("/customers/" + encodePathSegment(id));
        return transformCustomerResponse(upstream);
    }

    @GetMapping("/customers")
    public ResponseEntity<String> getCustomerByUserId(@RequestParam("userId") String userId,
                                                      @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireMobileClient(clientType);
        jwtUtil.validateAuthorizationHeader(authorization);
        ResponseEntity<String> upstream = forwardingClient.get("/customers?userId=" + URLEncoder.encode(userId, StandardCharsets.UTF_8));
        return transformCustomerResponse(upstream);
    }

    private void requireMobileClient(String clientType) {
        if (clientType == null || clientType.trim().isEmpty()) {
            throw new BadRequestException("Missing X-Client-Type header.");
        }

        String normalized = clientType.trim();
        if (!normalized.equalsIgnoreCase("iOS") && !normalized.equalsIgnoreCase("Android")) {
            throw new BadRequestException("Invalid X-Client-Type header.");
        }
    }

    private ResponseEntity<String> transformBookResponse(ResponseEntity<String> upstream) {
        if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null) {
            return upstream;
        }

        try {
            JsonNode root = objectMapper.readTree(upstream.getBody());
            if (root instanceof ObjectNode objectNode) {
                JsonNode genreNode = objectNode.get("genre");
                if (genreNode != null && genreNode.isTextual() && "non-fiction".equals(genreNode.asText())) {
                    objectNode.put("genre", 3);
                }

                return ResponseEntity.status(upstream.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(objectNode));
            }
            return upstream;
        } catch (Exception ex) {
            return upstream;
        }
    }

    private ResponseEntity<String> transformCustomerResponse(ResponseEntity<String> upstream) {
        if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null) {
            return upstream;
        }

        try {
            JsonNode root = objectMapper.readTree(upstream.getBody());
            if (root instanceof ObjectNode objectNode) {
                objectNode.remove("address");
                objectNode.remove("address2");
                objectNode.remove("city");
                objectNode.remove("state");
                objectNode.remove("zipcode");

                return ResponseEntity.status(upstream.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(objectNode));
            }
            return upstream;
        } catch (Exception ex) {
            return upstream;
        }
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}