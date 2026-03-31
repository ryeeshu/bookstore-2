package edu.cmu.bookstore.controller;

import edu.cmu.bookstore.model.Book;
import edu.cmu.bookstore.model.BookDetailsResponse;
import edu.cmu.bookstore.model.BookResponse;
import edu.cmu.bookstore.model.request.CreateBookRequest;
import edu.cmu.bookstore.model.request.UpdateBookRequest;
import edu.cmu.bookstore.service.BookService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller responsible for handling all book-related HTTP endpoints.
 *
 * This controller exposes endpoints for:
 * - creating a new book
 * - updating an existing book
 * - fetching book details by ISBN
 *
 * The controller delegates all business logic to {@link BookService}
 * and is responsible only for request handling and response construction.
 */
@RestController
@RequestMapping("/books")
public class BookController {

    /**
     * Service layer dependency used to perform book-related operations.
     */
    private final BookService bookService;

    /**
     * Creates a new controller instance with the required service dependency.
     *
     * @param bookService service used for book operations
     */
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Creates a new book.
     *
     * This endpoint accepts a request body containing the book information,
     * delegates creation to the service layer, and returns:
     * - HTTP 201 Created
     * - a Location header pointing to the created resource
     * - a response body with the created book summary
     *
     * @param request request payload containing the new book data
     * @return HTTP response with created book information
     */
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@RequestBody CreateBookRequest request) {
        // Create the book using the service layer.
        Book createdBook = bookService.createBook(request);

        // Build the Location header so clients know where the created resource can be fetched.
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/books/" + createdBook.getIsbn());

        // Return the created resource with HTTP 201 status.
        return new ResponseEntity<>(BookResponse.fromBook(createdBook), headers, HttpStatus.CREATED);
    }

    /**
     * Updates an existing book identified by its ISBN.
     *
     * The ISBN is taken from the path, and the updated values are provided
     * in the request body. The updated book is returned in the response.
     *
     * @param isbn ISBN of the book to update
     * @param request request payload containing fields to update
     * @return HTTP response with updated book information
     */
    @PutMapping("/{isbn}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable("isbn") String isbn,
                                                   @RequestBody UpdateBookRequest request) {
        // Delegate update logic to the service layer.
        Book updatedBook = bookService.updateBook(isbn, request);

        // Return the updated book with HTTP 200 OK.
        return ResponseEntity.ok(BookResponse.fromBook(updatedBook));
    }

    /**
     * Retrieves detailed information for a book by ISBN.
     *
     * This endpoint uses the standard /books/{isbn} path and returns
     * a detailed response representation of the book.
     *
     * @param isbn ISBN of the requested book
     * @return HTTP response containing detailed book information
     */
    @GetMapping("/{isbn}")
    public ResponseEntity<BookDetailsResponse> getBookByIsbn(@PathVariable("isbn") String isbn) {
        // Fetch the book from the service layer.
        Book book = bookService.getBookByIsbn(isbn);

        // Return detailed book information with HTTP 200 OK.
        return ResponseEntity.ok(BookDetailsResponse.fromBook(book));
    }

    /**
     * Retrieves detailed information for a book by ISBN using an alternative path.
     *
     * This endpoint exists to support the alternate route /books/isbn/{isbn}.
     * It behaves the same as {@link #getBookByIsbn(String)}.
     *
     * @param isbn ISBN of the requested book
     * @return HTTP response containing detailed book information
     */
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDetailsResponse> getBookByAlternativePath(@PathVariable("isbn") String isbn) {
        // Fetch the book from the service layer.
        Book book = bookService.getBookByIsbn(isbn);

        // Return detailed book information with HTTP 200 OK.
        return ResponseEntity.ok(BookDetailsResponse.fromBook(book));
    }
}