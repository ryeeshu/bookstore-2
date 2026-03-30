package edu.cmu.bookstore.repository;

import edu.cmu.bookstore.model.Book;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository class responsible for database operations related to books.
 *
 * This class uses {@link JdbcTemplate} to interact with the underlying
 * relational database and performs create, read, and update operations
 * on the books table.
 */
@Repository
public class BookRepository {

    /**
     * Spring JDBC helper used to execute SQL queries and updates.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Maps a database row from the books table to a {@link Book} object.
     */
    private final RowMapper<Book> bookRowMapper = (rs, rowNum) -> {
        Book book = new Book();
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setDescription(rs.getString("description"));
        book.setGenre(rs.getString("genre"));
        book.setPrice(rs.getBigDecimal("price"));
        book.setQuantity(rs.getInt("quantity"));
        book.setSummary(rs.getString("summary"));
        return book;
    };

    /**
     * Constructs the repository with the required JDBC dependency.
     *
     * @param jdbcTemplate JDBC helper for executing SQL statements
     */
    public BookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Checks whether a book with the given ISBN already exists.
     *
     * @param isbn ISBN to check
     * @return true if a matching book exists, otherwise false
     */
    public boolean existsByIsbn(String isbn) {
        String sql = "SELECT COUNT(*) FROM books WHERE isbn = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, isbn);
        return count != null && count > 0;
    }

    /**
     * Inserts a new book record into the books table.
     *
     * @param book book entity containing the values to insert
     */
    public void insertBook(Book book) {
        String sql = """
                INSERT INTO books (isbn, title, author, description, genre, price, quantity, summary)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getGenre(),
                book.getPrice(),
                book.getQuantity(),
                book.getSummary()
        );
    }

    /**
     * Updates the editable fields of an existing book identified by ISBN.
     *
     * The summary field is intentionally not updated here because it is
     * managed separately by the summary update method.
     *
     * @param book book entity containing the updated values
     * @return number of rows updated
     */
    public int updateBook(Book book) {
        String sql = """
                UPDATE books
                SET title = ?, author = ?, description = ?, genre = ?, price = ?, quantity = ?
                WHERE isbn = ?
                """;

        return jdbcTemplate.update(
                sql,
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getGenre(),
                book.getPrice(),
                book.getQuantity(),
                book.getIsbn()
        );
    }

    /**
     * Updates only the summary field of a book.
     *
     * This method is useful when the summary is generated asynchronously
     * after the initial book record has already been created.
     *
     * @param isbn ISBN of the book to update
     * @param summary generated summary text
     * @return number of rows updated
     */
    public int updateSummary(String isbn, String summary) {
        String sql = "UPDATE books SET summary = ? WHERE isbn = ?";
        return jdbcTemplate.update(sql, summary, isbn);
    }

    /**
     * Finds a book by its ISBN.
     *
     * @param isbn ISBN of the requested book
     * @return an {@link Optional} containing the book if found,
     *         or an empty Optional if no matching book exists
     */
    public Optional<Book> findByIsbn(String isbn) {
        String sql = """
                SELECT isbn, title, author, description, genre, price, quantity, summary
                FROM books
                WHERE isbn = ?
                """;

        List<Book> books = jdbcTemplate.query(sql, bookRowMapper, isbn);

        // Return an empty Optional when no matching book exists.
        if (books.isEmpty()) {
            return Optional.empty();
        }

        // ISBN is expected to be unique, so return the first matching record.
        return Optional.of(books.get(0));
    }
}