package com.enterprise.inventory.domain;

/**
 * Product types with different attribute schemas.
 * This demonstrates why MongoDB is a good fit — each type has different fields.
 */
public enum ProductType {
    ELECTRONICS,    // ram, cpu, storage, screen
    CLOTHING,       // size, color, material
    BOOK,           // isbn, author, pages
    FURNITURE,      // dimensions, weight, material
    FOOD            // expirationDate, weight, ingredients
}
