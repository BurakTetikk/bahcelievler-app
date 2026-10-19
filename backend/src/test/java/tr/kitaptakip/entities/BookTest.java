package tr.kitaptakip.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BookTest {
    @Test
    void normalizesIsbnByRemovingSpacesAndHyphens() {
        Book book = new Book("Dune", "Frank Herbert", "978-0-441-17271-9", null, 1965, "Science Fiction", null);

        assertThat(book.getIsbn()).isEqualTo("9780441172719");
    }

    @Test
    void turnsBlankIsbnIntoNull() {
        Book book = new Book("Dune", "Frank Herbert", " -  ", null, 1965, null, null);

        assertThat(book.getIsbn()).isNull();
    }

    @Test
    void archivesWithoutDeletingTheRecord() {
        Book book = new Book("Dune", "Frank Herbert", null, null, 1965, null, null);

        book.archive();

        assertThat(book.isActive()).isFalse();
    }
}
