package com.trading;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimpleTest {
    
    @Test
    void testAddition() {
        assertEquals(4, 2 + 2);
        System.out.println("✅ Тест сложения пройден");
    }
    
    @Test
    void testString() {
        String str = "Hello";
        assertTrue(str.length() > 0);
        System.out.println("✅ Тест строки пройден");
    }
}