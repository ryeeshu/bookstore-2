package edu.cmu.bookstore.service;

import edu.cmu.bookstore.exception.BadRequestException;
import edu.cmu.bookstore.exception.ConflictException;
import edu.cmu.bookstore.exception.NotFoundException;
import edu.cmu.bookstore.model.Book;
import edu.cmu.bookstore.model.request.CreateBookRequest;
import edu.cmu.bookstore.model.request.UpdateBookRequest;
import edu.cmu.bookstore.repository.BookRepository;
import edu.cmu.bookstore.validation.BookValidator;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for book-related business logic.
 *
 * This class coordinates request validation, repository access,
 * and summary generation through the LLM service for book creation,
 * update, and retrieval operations.
 */
@Service
public class BookService {

    /**
     * Repository used for book persistence operations.
     */
    private final BookRepository bookRepository;

    /**
     * Validator used to enforce request and input constraints.
     */
    private final BookValidator bookValidator;

    /**
     * Service used to generate book summaries using an LLM.
     */
    private final LlmService llmService;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param bookRepository repository used for book persistence
     * @param bookValidator validator used for request checking
     * @param llmService service used for summary generation
     */
    public BookService(BookRepository bookRepository,
                       BookValidator bookValidator,
                       LlmService llmService) {
        this.bookRepository = bookRepository;
        this.bookValidator = bookValidator;
        this.llmService = llmService;
    }

    /**
     * Creates a new book after validating the request and checking for conflicts.
     *
     * The book is initially inserted without a summary. The service then attempts
     * to generate a summary through the LLM service. If summary generation fails,
     * the create operation still succeeds.
     *
     * @param request request payload containing book data
     * @return created book object
     */
    public Book createBook(CreateBookRequest request) {
        bookValidator.validateCreateRequest(request);

        String isbn = request.getIsbn().trim();

        // Prevent duplicate book creation for the same ISBN.
        if (bookRepository.existsByIsbn(isbn)) {
            throw new ConflictException("This ISBN already exists in the system.");
        }

        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setDescription(request.getDescription().trim());
        book.setGenre(request.getGenre().trim());
        book.setPrice(request.getPrice());
        book.setQuantity(request.getQuantity());
        book.setSummary(null);

        bookRepository.insertBook(book);

        try {
            String summary = llmService.generateSummary(book);
            if (summary != null && !summary.trim().isEmpty()) {
                bookRepository.updateSummary(book.getIsbn(), summary);
            }
        } catch (Exception ignored) {
            // POST should still succeed even if LLM fails.
        }

        return book;
    }

    /**
     * Updates an existing book after validating the path ISBN and request body.
     *
     * The ISBN in the request body must match the ISBN in the path. If the book
     * does not exist, a not-found error is raised. The updated record is then
     * fetched again from the repository and returned.
     *
     * @param pathIsbn ISBN supplied in the request path
     * @param request request payload containing updated book fields
     * @return updated book object
     */
    public Book updateBook(String pathIsbn, UpdateBookRequest request) {
        bookValidator.validatePathIsbn(pathIsbn);
        bookValidator.validateUpdateRequest(request);

        String trimmedPathIsbn = pathIsbn.trim();
        String bodyIsbn = request.getIsbn().trim();

        // Ensure clients do not attempt to update one book while referencing another in the body.
        if (!trimmedPathIsbn.equals(bodyIsbn)) {
            throw new BadRequestException("ISBN in path and body must match.");
        }

        Book existingBook = bookRepository.findByIsbn(trimmedPathIsbn)
                .orElseThrow(() -> new NotFoundException("Book not found."));

        existingBook.setTitle(request.getTitle().trim());
        existingBook.setAuthor(request.getAuthor().trim());
        existingBook.setDescription(request.getDescription().trim());
        existingBook.setGenre(request.getGenre().trim());
        existingBook.setPrice(request.getPrice());
        existingBook.setQuantity(request.getQuantity());

        bookRepository.updateBook(existingBook);

        // Re-read the book from the database so the returned object reflects persisted state.
        return bookRepository.findByIsbn(trimmedPathIsbn)
                .orElseThrow(() -> new NotFoundException("Book not found."));
    }

    /**
     * Retrieves a book by ISBN.
     *
     * If the book exists but does not yet have a summary, this method attempts
     * to generate one and persist it. Retrieval still succeeds even if summary
     * generation fails.
     *
     * @param isbn ISBN of the requested book
     * @return matching book object
     */
    public Book getBookByIsbn(String isbn) {
        bookValidator.validatePathIsbn(isbn);

        Book book = bookRepository.findByIsbn(isbn.trim())
                .orElseThrow(() -> new NotFoundException("Book not found."));

        // Lazily generate and persist a summary when the book does not already have one.
        if (book.getSummary() == null || book.getSummary().trim().isEmpty()) {
            try {
                String summary = llmService.generateSummary(book);
                if (summary != null && !summary.trim().isEmpty()) {
                    bookRepository.updateSummary(book.getIsbn(), summary);
                    book.setSummary(summary);
                }
            } catch (Exception ignored) {
                // GET should still succeed even if LLM fails.
            }
        }

        return book;
    }
}