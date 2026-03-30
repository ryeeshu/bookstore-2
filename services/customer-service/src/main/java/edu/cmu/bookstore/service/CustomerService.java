package edu.cmu.bookstore.service;

import edu.cmu.bookstore.exception.ConflictException;
import edu.cmu.bookstore.exception.NotFoundException;
import edu.cmu.bookstore.model.Customer;
import edu.cmu.bookstore.model.request.CreateCustomerRequest;
import edu.cmu.bookstore.repository.CustomerRepository;
import edu.cmu.bookstore.validation.CustomerValidator;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for customer-related business logic.
 *
 * This class coordinates request validation and repository access
 * for customer creation and retrieval operations.
 */
@Service
public class CustomerService {

    /**
     * Repository used for customer persistence operations.
     */
    private final CustomerRepository customerRepository;

    /**
     * Validator used to enforce request and input constraints.
     */
    private final CustomerValidator customerValidator;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param customerRepository repository used for customer persistence
     * @param customerValidator validator used for request checking
     */
    public CustomerService(CustomerRepository customerRepository,
                           CustomerValidator customerValidator) {
        this.customerRepository = customerRepository;
        this.customerValidator = customerValidator;
    }

    /**
     * Creates a new customer after validating the request and checking for conflicts.
     *
     * If a customer with the same user ID already exists, a conflict exception
     * is thrown. On success, the generated database identifier is assigned to
     * the returned customer object.
     *
     * @param request request payload containing customer data
     * @return created customer object
     */
    public Customer createCustomer(CreateCustomerRequest request) {
        customerValidator.validateCreateRequest(request);

        String trimmedUserId = request.getUserId().trim();

        // Prevent duplicate customer creation for the same user identifier.
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

    /**
     * Retrieves a customer by internal numeric identifier.
     *
     * @param id internal identifier of the requested customer
     * @return matching customer object
     */
    public Customer getCustomerById(Long id) {
        customerValidator.validateCustomerId(id);

        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found."));
    }

    /**
     * Retrieves a customer by user identifier.
     *
     * @param userId user identifier associated with the requested customer
     * @return matching customer object
     */
    public Customer getCustomerByUserId(String userId) {
        customerValidator.validateUserIdQuery(userId);

        return customerRepository.findByUserId(userId.trim())
                .orElseThrow(() -> new NotFoundException("Customer not found."));
    }
}