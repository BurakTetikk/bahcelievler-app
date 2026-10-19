package tr.kitaptakip.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DtoValidationTest {
    private static Validator validator;
    private static ValidatorFactory factory;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void rejectsBlankBookTitleAndOversizedAuthor() {
        BookRequest request = new BookRequest(" ", "A".repeat(151), null, null, null, null, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("title", "author");
    }

    @Test
    void rejectsMalformedOptionalBorrowerEmail() {
        BorrowerRequest request = new BorrowerRequest("Ada", "Yılmaz", "invalid-email", null, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email");
    }
}
