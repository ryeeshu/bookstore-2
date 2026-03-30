package edu.cmu.bookstore.service;

import edu.cmu.bookstore.exception.ConflictException;
import edu.cmu.bookstore.exception.NotFoundException;
import edu.cmu.bookstore.model.Customer;
import edu.cmu.bookstore.model.request.CreateCustomerRequest;
import edu.cmu.bookstore.repository.CustomerRepository;
import edu.cmu.bookstore.validation.CustomerValidator;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerValidator customerValidator;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerValidator customerValidator) {
        this.customerRepository = customerRepository;
        this.customerValidator = customerValidator;
    }

    public Customer createCustomer(CreateCustomerRequest request) {
        customerValidator.validateCreateRequest(request);

        String trimmedUserId = request.getUserId().trim();

        if (customerRepository.existsByUserId(trimmedUserId)) {
            throw new ConflictException("This user ID already exists in the system.");
        }

        Customer customer = new Customer();
        customer.setUserId(trimmedUserId);
        customer.setName(request.getName().trim());
        customer.setPhone(request.getPhone().trim());
        customer.setAddress(request.getAddress().trim());
        customer.setAddress2(request.getAddress2() == null ? null : request.getAddress2().trim());
        customer.setCity(request.getCity().trim());
        customer.setState(request.getState().trim().toUpperCase());
        customer.setZipcode(request.getZipcode().trim());

        long generatedId = customerRepository.insertCustomer(customer);
        customer.setId(generatedId);

        return customer;
    }

    public Customer getCustomerById(Long id) {
        customerValidator.validateCustomerId(id);

        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found."));
    }

    public Customer getCustomerByUserId(String userId) {
        String normalizedUserId = normalizeUserId(userId);

        customerValidator.validateUserIdQuery(normalizedUserId);

        return customerRepository.findByUserId(normalizedUserId)
                .orElseThrow(() -> new NotFoundException("Customer not found."));
    }

    private String normalizeUserId(String userId) {
        if (userId == null) {
            return null;
        }

        return userId.trim().replace(' ', '+');
    }
}