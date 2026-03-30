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

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(@RequestBody CreateBookRequest request) {
        Book createdBook = bookService.createBook(request);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, "/books/" + createdBook.getIsbn());

        return new ResponseEntity<>(BookResponse.fromBook(createdBook), headers, HttpStatus.CREATED);
    }

    @PutMapping("/{isbn}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable("isbn") String isbn,
                                                   @RequestBody UpdateBookRequest request) {
        Book updatedBook = bookService.updateBook(isbn, request);
        return ResponseEntity.ok(BookResponse.fromBook(updatedBook));
    }

    @GetMapping("/{isbn}")
    public ResponseEntity<BookDetailsResponse> getBookByIsbn(@PathVariable("isbn") String isbn) {
        Book book = bookService.getBookByIsbn(isbn);
        return ResponseEntity.ok(BookDetailsResponse.fromBook(book));
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDetailsResponse> getBookByAlternativePath(@PathVariable("isbn") String isbn) {
        Book book = bookService.getBookByIsbn(isbn);
        return ResponseEntity.ok(BookDetailsResponse.fromBook(book));
    }
}