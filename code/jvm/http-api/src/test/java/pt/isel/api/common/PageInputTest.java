package pt.isel.api.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageInputTest {

    @Test
    void testDefaultValues() {
        PageInput page = new PageInput(null, null);
        assertEquals(50, page.limit());
        assertEquals(0, page.offset());
    }

    @Test
    void testNegativeValues() {
        PageInput page = new PageInput(-10, -5);
        assertEquals(50, page.limit());
        assertEquals(0, page.offset());
    }

    @Test
    void testMaxLimit() {
        PageInput page = new PageInput(200, 10);
        assertEquals(100, page.limit());
        assertEquals(10, page.offset());
    }

    @Test
    void testValidValues() {
        PageInput page = new PageInput(25, 5);
        assertEquals(25, page.limit());
        assertEquals(5, page.offset());
    }
}