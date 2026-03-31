package edu.cmu.bookstore.controller;

import edu.cmu.bookstore.exception.BadRequestException;
import edu.cmu.bookstore.util.ForwardingClient;
import edu.cmu.bookstore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * REST controller for the web Backend For Frontend (BFF).
 *
 * This controller accepts requests intended only for web clients,
 * validates the required client-type and authorization headers,
 * and forwards requests to downstream backend services.
 *
 * Unlike the mobile BFF, this controller does not modify response bodies.
 * It mainly performs:
 * - X-Client-Type validation for web clients
 * - JWT authorization validation
 * - request forwarding to backend services
 */
@RestController
public class WebBffController {

    /**
     * Utility used to validate the Authorization header and JWT payload.
     */
    private final JwtUtil jwtUtil;

    /**
     * Client used to forward incoming requests to backend services.
     */
    private final ForwardingClient forwardingClient;

    /**
     * Creates a web BFF controller with the required dependencies.
     *
     * @param jwtUtil utility for JWT validation
     * @param forwardingClient client for forwarding HTTP requests to backend services
     */
    public WebBffController(JwtUtil jwtUtil, ForwardingClient forwardingClient) {
        this.jwtUtil = jwtUtil;
        this.forwardingClient = forwardingClient;
    }

    /**
     * Creates a new book through the web BFF.
     *
     * This endpoint:
     * - validates that the request is from a web client
     * - validates the Authorization header
     * - forwards the request body to the backend book service
     *
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @param body raw request body
     * @return response returned by the backend service
     */
    @PostMapping("/books")
    public ResponseEntity<String> createBook(@RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = false) String body) {
        // Ensure the request is coming from a valid web client.
        requireWebClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the create-book request to the backend service.
        return forwardingClient.post("/books", body);
    }

    /**
     * Updates an existing book identified by ISBN through the web BFF.
     *
     * This endpoint:
     * - validates the web client header
     * - validates the JWT authorization header
     * - safely encodes the ISBN for the URL path
     * - forwards the update request to the backend book service
     *
     * @param isbn ISBN of the book to update
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @param body raw request body
     * @return response returned by the backend service
     */
    @PutMapping("/books/{isbn}")
    public ResponseEntity<String> updateBook(@PathVariable String isbn,
                                             @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody(required = false) String body) {
        // Ensure the request is coming from a valid web client.
        requireWebClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the update request using a safely encoded path segment.
        return forwardingClient.put("/books/" + encodePathSegment(isbn), body);
    }

    /**
     * Retrieves book details by ISBN through the web BFF.
     *
     * This endpoint validates headers and forwards the request directly
     * to the backend book service without modifying the response.
     *
     * @param isbn ISBN of the requested book
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @return response returned by the backend service
     */
    @GetMapping("/books/{isbn}")
    public ResponseEntity<String> getBook(@PathVariable String isbn,
                                          @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Ensure the request is coming from a valid web client.
        requireWebClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the book lookup request to the backend service.
        return forwardingClient.get("/books/" + encodePathSegment(isbn));
    }

    /**
     * Retrieves book details by ISBN using the alternate /books/isbn/{isbn} path.
     *
     * This behaves similarly to the standard book lookup endpoint but uses
     * the alternate route expected by the API.
     *
     * @param isbn ISBN of the requested book
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @return response returned by the backend service
     */
    @GetMapping("/books/isbn/{isbn}")
    public ResponseEntity<String> getBookAlt(@PathVariable String isbn,
                                             @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Ensure the request is coming from a valid web client.
        requireWebClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the alternate book lookup request to the backend service.
        return forwardingClient.get("/books/isbn/" + encodePathSegment(isbn));
    }

    /**
     * Creates a new customer through the web BFF.
     *
     * This endpoint:
     * - validates that the request is from a web client
     * - validates the Authorization header
     * - forwards the request body to the backend customer service
     *
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @param body raw request body
     * @return response returned by the backend service
     */
    @PostMapping("/customers")
    public ResponseEntity<String> createCustomer(@RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @RequestBody(required = false) String body) {
        // Ensure the request is coming from a valid web client.
        requireWebClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the create-customer request to the backend service.
        return forwardingClient.post("/customers", body);
    }

    /**
     * Retrieves a customer by numeric ID through the web BFF.
     *
     * This endpoint validates headers and forwards the request directly
     * to the backend customer service without modifying the response.
     *
     * @param id numeric customer identifier
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @return response returned by the backend service
     */
    @GetMapping("/customers/{id}")
    public ResponseEntity<String> getCustomerById(@PathVariable String id,
                                                  @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Ensure the request is coming from a valid web client.
        requireWebClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Forward the customer lookup request to the backend service.
        return forwardingClient.get("/customers/" + encodePathSegment(id));
    }

    /**
     * Retrieves a customer by userId query parameter through the web BFF.
     *
     * The raw query string is read from the incoming request and forwarded
     * to the backend service exactly as received.
     *
     * @param request incoming HTTP servlet request
     * @param clientType value of the X-Client-Type header
     * @param authorization value of the Authorization header
     * @return response returned by the backend service
     * @throws BadRequestException if the required query parameter is missing
     */
    @GetMapping("/customers")
    public ResponseEntity<String> getCustomerByUserId(HttpServletRequest request,
                                                      @RequestHeader(value = "X-Client-Type", required = false) String clientType,
                                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        // Ensure the request is coming from a valid web client.
        requireWebClient(clientType);

        // Validate the bearer token and its required claims.
        jwtUtil.validateAuthorizationHeader(authorization);

        // Read the raw query string exactly as it arrived.
        String rawQuery = request.getQueryString();

        // Reject the request if the required query string is missing.
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new BadRequestException("Missing required query parameter: userId");
        }

        // Forward the request including the raw query string.
        return forwardingClient.get("/customers?" + rawQuery);
    }

    /**
     * Validates that the X-Client-Type header represents a supported web client.
     *
     * Accepted value:
     * - Web
     *
     * Comparison is case-insensitive after trimming surrounding whitespace.
     *
     * @param clientType value of the X-Client-Type header
     * @throws BadRequestException if the header is missing, blank, or invalid
     */
    private void requireWebClient(String clientType) {
        // Reject missing or blank client type values.
        if (clientType == null || clientType.trim().isEmpty()) {
            throw new BadRequestException("Missing X-Client-Type header.");
        }

        // Normalize the client type for validation.
        String normalized = clientType.trim();

        // Only web clients are allowed through this BFF.
        if (!normalized.equalsIgnoreCase("Web")) {
            throw new BadRequestException("Invalid X-Client-Type header.");
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