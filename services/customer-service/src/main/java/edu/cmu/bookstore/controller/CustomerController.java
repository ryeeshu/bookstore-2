package edu.cmu.bookstore.controller;

import edu.cmu.bookstore.exception.BadRequestException;
import edu.cmu.bookstore.model.Customer;
import edu.cmu.bookstore.model.request.CreateCustomerRequest;
import edu.cmu.bookstore.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody CreateCustomerRequest request) {
        Customer createdCustomer = customerService.createCustomer(request);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/customers/" + createdCustomer.getId());
        return new ResponseEntity<>(createdCustomer, headers, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable("id") Long id) {
        Customer customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    @GetMapping
    public ResponseEntity<Customer> getCustomerByUserId(HttpServletRequest request) {
        String rawQuery = request.getQueryString();

        if (rawQuery == null || rawQuery.isBlank()) {
            throw new BadRequestException("userId is required.");
        }

        String userId = extractUserId(rawQuery);
        Customer customer = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(customer);
    }

    private String extractUserId(String rawQuery) {
        for (String part : rawQuery.split("&")) {
            int idx = part.indexOf('=');
            if (idx <= 0) {
                continue;
            }

            String key = part.substring(0, idx);
            String value = part.substring(idx + 1);

            if ("userId".equals(key)) {
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }

        throw new BadRequestException("userId is required.");
    }
}