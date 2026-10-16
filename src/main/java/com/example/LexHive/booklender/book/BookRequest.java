package com.example.LexHive.booklender.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public final class BookRequest {
    @Size(max = 255)
    @NotBlank
    private final String title;
    @Size(max = 255)
    @NotBlank
    private final String author;
    @Size(max = 17)
    @NotBlank
    private final String isbn;
    @Min(0)
    @NotNull
    private final Integer totalCopies;

    public BookRequest(
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 255) String author,
            @NotBlank @Size(max = 17) String isbn,
            @NotNull @Min(0) Integer totalCopies
    ) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.totalCopies = totalCopies;
    }

    @Size(max = 255)
    @NotBlank
    public String title() {
        return title;
    }

    @Size(max = 255)
    @NotBlank
    public String author() {
        return author;
    }

    @Size(max = 17)
    @NotBlank
    public String isbn() {
        return isbn;
    }

    @Min(0)
    @NotNull
    public Integer totalCopies() {
        return totalCopies;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BookRequest) obj;
        return Objects.equals(this.title, that.title) &&
                Objects.equals(this.author, that.author) &&
                Objects.equals(this.isbn, that.isbn) &&
                Objects.equals(this.totalCopies, that.totalCopies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, isbn, totalCopies);
    }

    @Override
    public String toString() {
        return "BookRequest[" +
                "title=" + title + ", " +
                "author=" + author + ", " +
                "isbn=" + isbn + ", " +
                "totalCopies=" + totalCopies + ']';
    }

}
