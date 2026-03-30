package edu.cmu.bookstore.controller;

import edu.cmu.bookstore.model.Customer;
import edu.cmu.bookstore.model.request.CreateCustomerRequest;
import edu.cmu.bookstore.service.CustomerService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Customer> getCustomerByUserId(@RequestParam("userId") String userId) {
        Customer customer = customerService.getCustomerByUserId(userId);
        return ResponseEntity.ok(customer);
    }
}