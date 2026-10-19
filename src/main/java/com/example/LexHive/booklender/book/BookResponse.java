package com.example.LexHive.booklender.book;

import java.util.Objects;

public final class BookResponse {
    private final Long id;
    private final String title;
    private final String author;
    private final String isbn;
    private final int totalCopies;
    private final int availableCopies;

    public BookResponse(
            Long id,
            String title,
            String author,
            String isbn,
            int totalCopies,
            int availableCopies
    ) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getTotalCopies(),
                book.getAvailableCopies());
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        BookResponse responseObj = (BookResponse) obj;
        return Objects.equals(this.id, responseObj.id) &&
                Objects.equals(this.title, responseObj.title) &&
                Objects.equals(this.author, responseObj.author) &&
                Objects.equals(this.isbn, responseObj.isbn) &&
                this.totalCopies == responseObj.totalCopies &&
                this.availableCopies == responseObj.availableCopies;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, author, isbn, totalCopies, availableCopies);
    }

    @Override
    public String toString() {
        return "BookResponse[" +
                "id=" + id + ", " +
                "title=" + title + ", " +
                "author=" + author + ", " +
                "isbn=" + isbn + ", " +
                "totalCopies=" + totalCopies + ", " +
                "availableCopies=" + availableCopies + ']';
    }


}
