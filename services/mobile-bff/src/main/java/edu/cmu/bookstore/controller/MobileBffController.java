package edu.cmu.bookstore.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.cmu.bookstore.exception.BadRequestException;
import edu.cmu.bookstore.util.ForwardingClient;
import edu.cmu.bookstore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * REST controller for the mobile Backend For Frontend (BFF).
 *
 * This controller accepts requests intended for mobile clients only
 * (iOS and Android), validates the JWT-based authorization header,
 * forwards requests to the appropriate backend service, and applies
 * mobile-specific response transformations where required.
 *
 * Mobile-specific behavior implemented here includes:
 * - replacing the book genre "non-fiction" with numeric value 3
 * - removing address-related customer fields from customer responses
 */
@RestController
public class MobileBffController {

    /**
     * Utility used to validate the Authorization header and JWT payload.
     */
    private final JwtUtil jwtUtil;

    /**
     * Client used to forward requests from this BFF to downstream backend services.
     */
    private final ForwardingClient forwardingClient;

    /**
     * Object mapper used to parse and modify JSON responses returned
     * by downstream services.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a mobile BFF controller with the required dependencies.
     *
     * @param jwtUtil utility for JWT validation
     * @param forwardingClient client for forwarding HTTP requests to backend services
     */
    public MobileBffController(JwtUtil jwtUtil, ForwardingClient forwardingClient) {
        this.jwtUtil = jwtUtil;
        this.forwardingClient = forwardingClient;
    }

    /**
     * Creates a new book through the mobile BFF.
     *
     * This endpoint:
     * - validates that the client type is mobile
     * - validates the Authorization header
     * - forwards the request body to the backend book service
     *
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @param body raw JSON request body
     * @return forwarded response from the backend service
     */
    @PostMapping("/books")
    public ResponseEntity<String> createBook(@RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = false) String body) {
        // Ensure the request is coming from a valid mobile client.
        requireMobileClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the create-book request to the backend service.
        return forwardingClient.post("/books", body);
    }

    /**
     * Updates an existing book identified by ISBN through the mobile BFF.
     *
     * This endpoint:
     * - validates the mobile client header
     * - validates the JWT authorization header
     * - encodes the ISBN safely for use in the path
     * - forwards the update request to the backend book service
     *
     * @param isbn ISBN of the book to update
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @param body raw JSON request body
     * @return forwarded response from the backend service
     */
    @PutMapping("/books/{isbn}")
    public ResponseEntity<String> updateBook(@PathVariable String isbn,
                                             @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = false) String body) {
        // Ensure the request is coming from a valid mobile client.
        requireMobileClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the update request using a safely encoded path segment.
        return forwardingClient.put("/books/" + encodePathSegment(isbn), body);
    }

    /**
     * Retrieves book details by ISBN through the mobile BFF.
     *
     * After forwarding the request to the backend service, the response
     * is transformed for mobile clients when needed.
     *
     * @param isbn ISBN of the requested book
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @return transformed or original response from the backend service
     */
    @GetMapping("/books/{isbn}")
    public ResponseEntity<String> getBook(@PathVariable String isbn,
                                          @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Ensure the request is coming from a valid mobile client.
        requireMobileClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the request to the backend book service.
        ResponseEntity<String> upstream = forwardingClient.get("/books/" + encodePathSegment(isbn));

        // Apply mobile-specific transformation to the book response.
        return transformBookResponse(upstream);
    }

    /**
     * Retrieves book details by ISBN using the alternate /books/isbn/{isbn} path.
     *
     * This behaves the same as the standard book lookup endpoint, but uses
     * the alternate route expected by the API.
     *
     * @param isbn ISBN of the requested book
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @return transformed or original response from the backend service
     */
    @GetMapping("/books/isbn/{isbn}")
    public ResponseEntity<String> getBookAlt(@PathVariable String isbn,
                                             @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Ensure the request is coming from a valid mobile client.
        requireMobileClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the request to the backend book service using the alternate route.
        ResponseEntity<String> upstream = forwardingClient.get("/books/isbn/" + encodePathSegment(isbn));

        // Apply mobile-specific transformation to the book response.
        return transformBookResponse(upstream);
    }

    /**
     * Creates a new customer through the mobile BFF.
     *
     * This endpoint:
     * - validates that the client type is mobile
     * - validates the Authorization header
     * - forwards the request body to the backend customer service
     *
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @param body raw JSON request body
     * @return forwarded response from the backend service
     */
    @PostMapping("/customers")
    public ResponseEntity<String> createCustomer(@RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @RequestBody(required = false) String body) {
        // Ensure the request is coming from a valid mobile client.
        requireMobileClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the create-customer request to the backend service.
        return forwardingClient.post("/customers", body);
    }

    /**
     * Retrieves a customer by numeric ID through the mobile BFF.
     *
     * After forwarding the request to the backend service, the response
     * is transformed to remove address-related fields for mobile clients.
     *
     * @param id numeric customer identifier
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @return transformed or original response from the backend service
     */
    @GetMapping("/customers/{id}")
    public ResponseEntity<String> getCustomerById(@PathVariable String id,
                                                  @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Ensure the request is coming from a valid mobile client.
        requireMobileClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the customer lookup request to the backend service.
        ResponseEntity<String> upstream = forwardingClient.get("/customers/" + encodePathSegment(id));

        // Apply mobile-specific transformation to the customer response.
        return transformCustomerResponse(upstream);
    }

    /**
     * Retrieves a customer by userId query parameter through the mobile BFF.
     *
     * The raw query string is preserved and forwarded to the backend service.
     * The response is then transformed to remove address-related fields for mobile clients.
     *
     * @param request incoming HTTP request
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @return transformed or original response from the backend service
     */
    @GetMapping("/customers")
    public ResponseEntity<String> getCustomerByUserId(HttpServletRequest request,
                                                      @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Ensure the request is coming from a valid mobile client.
        requireMobileClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Preserve the raw query string exactly as it arrived.
        String rawQuery = request.getQueryString();

        // Reject the request if the required query string is missing.
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new BadRequestException("Missing required query parameter: userId");
        }

        // Forward the request including the raw query string.
        ResponseEntity<String> upstream = forwardingClient.get("/customers?" + rawQuery);

        // Apply mobile-specific transformation to the customer response.
        return transformCustomerResponse(upstream);
    }

    /**
     * Validates that the X-Client-Type header represents a supported mobile client.
     *
     * Accepted values are:
     * - iOS
     * - Android
     *
     * Comparison is case-insensitive after trimming.
     *
     * @param clientType value of the X-Client-Type header
     * @throws BadRequestException if the header is missing, blank, or invalid
     */
    private void requireMobileClient(String clientType) {
        // Reject missing or blank client type values.
        if (clientType == null || clientType.trim().isEmpty()) {
            throw new BadRequestException("Missing X-Client-Type header.");
        }

        // Normalize the client type for validation.
        String normalized = clientType.trim();

        // Only iOS and Android clients are allowed through this BFF.
        if (!normalized.equalsIgnoreCase("iOS") && !normalized.equalsIgnoreCase("Android")) {
            throw new BadRequestException("Invalid X-Client-Type header.");
        }
    }

    /**
     * Applies the mobile-specific transformation to a book response.
     *
     * If the upstream response is successful and contains a JSON object,
     * the method replaces:
     * - "genre": "non-fiction"
     * with:
     * - "genre": 3
     *
     * If the response is not successful, has no body, or cannot be parsed safely,
     * the original response is returned unchanged.
     *
     * @param upstream original response from the backend book service
     * @return transformed response for mobile clients, or the original response
     */
    private ResponseEntity<String> transformBookResponse(ResponseEntity<String> upstream) {
        // Only transform successful JSON-bearing responses.
        if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null) {
            return upstream;
        }

        try {
            // Parse the upstream JSON response body.
            JsonNode root = objectMapper.readTree(upstream.getBody());

            // Proceed only if the root JSON node is an object.
            if (root instanceof ObjectNode objectNode) {
                // Read the genre field if present.
                JsonNode genreNode = objectNode.get("genre");

                // Replace the textual genre "non-fiction" with numeric value 3 for mobile clients.
                if (genreNode != null && genreNode.isTextual() && "non-fiction".equals(genreNode.asText())) {
                    objectNode.put("genre", 3);
                }

                // Return the transformed JSON body while preserving the upstream HTTP status.
                return ResponseEntity.status(upstream.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(objectNode));
            }

            // If the response is not a JSON object, return it unchanged.
            return upstream;
        } catch (Exception ex) {
            // If parsing or transformation fails, fall back to the original upstream response.
            return upstream;
        }
    }

    /**
     * Applies the mobile-specific transformation to a customer response.
     *
     * If the upstream response is successful and contains a JSON object,
     * the following fields are removed:
     * - address
     * - address2
     * - city
     * - state
     * - zipcode
     *
     * If the response is not successful, has no body, or cannot be parsed safely,
     * the original response is returned unchanged.
     *
     * @param upstream original response from the backend customer service
     * @return transformed response for mobile clients, or the original response
     */
    private ResponseEntity<String> transformCustomerResponse(ResponseEntity<String> upstream) {
        // Only transform successful JSON-bearing responses.
        if (!upstream.getStatusCode().is2xxSuccessful() || upstream.getBody() == null) {
            return upstream;
        }

        try {
            // Parse the upstream JSON response body.
            JsonNode root = objectMapper.readTree(upstream.getBody());

            // Proceed only if the root JSON node is an object.
            if (root instanceof ObjectNode objectNode) {
                // Remove address-related fields from the customer response for mobile clients.
                objectNode.remove("address");
                objectNode.remove("address2");
                objectNode.remove("city");
                objectNode.remove("state");
                objectNode.remove("zipcode");

                // Return the transformed JSON body while preserving the upstream HTTP status.
                return ResponseEntity.status(upstream.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(objectNode));
            }

            // If the response is not a JSON object, return it unchanged.
            return upstream;
        } catch (Exception ex) {
            // If parsing or transformation fails, fall back to the original upstream response.
            return upstream;
        }
    }

    /**
     * URL-encodes a value for safe use as a path segment.
     *
     * Standard URL encoding converts spaces to '+', but for path segments
     * this method replaces '+' with "%20" so spaces remain encoded as %20.
     *
     * @param value raw path segment value
     * @return encoded path segment safe for inclusion in a URL path
     */
    private String encodePathSegment(String value) {
        // Encode the path segment using UTF-8, then normalize space encoding for URL paths.
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}