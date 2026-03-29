package pt.isel.api.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageInputTest {

    @Test
    void Constructor_NullValues_SetsDefaults() {
        PageInput page = new PageInput(null, null);

        assertThat(page.limit()).isEqualTo(50);
        assertThat(page.offset()).isEqualTo(0);
    }

    @Test
    void Constructor_NegativeValues_SetsDefaults() {
        PageInput page = new PageInput(-10, -5);

        assertThat(page.limit()).isEqualTo(50);
        assertThat(page.offset()).isEqualTo(0);
    }

    @Test
    void Constructor_ExceedsMaxLimit_SetsMaxLimit() {
        PageInput page = new PageInput(200, 10);

        assertThat(page.limit()).isEqualTo(100);
        assertThat(page.offset()).isEqualTo(10);
    }

    @Test
    void Constructor_ValidValues_SetsValues() {
        PageInput page = new PageInput(25, 5);

        assertThat(page.limit()).isEqualTo(25);
        assertThat(page.offset()).isEqualTo(5);
    }
}