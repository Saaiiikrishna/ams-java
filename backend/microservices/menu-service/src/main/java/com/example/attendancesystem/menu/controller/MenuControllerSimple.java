package com.example.attendancesystem.menu.controller;

import com.example.attendancesystem.shared.dto.CategoryDto;
import com.example.attendancesystem.shared.dto.ItemDto;
import com.example.attendancesystem.menu.service.MenuService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Simplified Menu Controller for microservice
 * Handles menu management operations without complex security
 */
@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuControllerSimple {
    
    private static final Logger logger = LoggerFactory.getLogger(MenuControllerSimple.class);
    
    @Autowired
    private MenuService menuService;
    
    // Category Management Endpoints
    
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories(@RequestParam String entityId) {
        try {
            List<CategoryDto> categories = menuService.getCategoriesByOrganization(entityId);
            return ResponseEntity.ok(categories);

        } catch (Exception e) {
            logger.error("Failed to get categories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get categories"));
        }
    }
    
    @GetMapping("/categories/with-items")
    public ResponseEntity<?> getCategoriesWithItems(@RequestParam String entityId) {
        try {
            List<CategoryDto> categories = menuService.getCategoriesWithItemsByOrganization(entityId);
            return ResponseEntity.ok(categories);

        } catch (Exception e) {
            logger.error("Failed to get categories with items: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get categories with items"));
        }
    }
    
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestParam String entityId, @Valid @RequestBody CategoryDto categoryDto) {
        try {
            CategoryDto createdCategory = menuService.createCategory(entityId, categoryDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);

        } catch (Exception e) {
            logger.error("Failed to create category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create category"));
        }
    }
    
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<?> updateCategory(@RequestParam String entityId, @PathVariable Long categoryId, @Valid @RequestBody CategoryDto categoryDto) {
        try {
            CategoryDto updatedCategory = menuService.updateCategory(entityId, categoryId, categoryDto);
            return ResponseEntity.ok(updatedCategory);
            
        } catch (Exception e) {
            logger.error("Failed to update category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update category"));
        }
    }
    
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<?> deleteCategory(@RequestParam String entityId, @PathVariable Long categoryId) {
        try {
            menuService.deleteCategory(entityId, categoryId);
            return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to delete category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete category"));
        }
    }
    
    // Item Management Endpoints
    
    @GetMapping("/items")
    public ResponseEntity<?> getItems(@RequestParam String entityId) {
        try {
            List<ItemDto> items = menuService.getItemsByOrganization(entityId);
            return ResponseEntity.ok(items);
            
        } catch (Exception e) {
            logger.error("Failed to get items: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get items"));
        }
    }
    
    @GetMapping("/categories/{categoryId}/items")
    public ResponseEntity<?> getItemsByCategory(@RequestParam String entityId, @PathVariable Long categoryId) {
        try {
            List<ItemDto> items = menuService.getItemsByCategory(entityId, categoryId);
            return ResponseEntity.ok(items);
            
        } catch (Exception e) {
            logger.error("Failed to get items by category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get items by category"));
        }
    }
    
    @PostMapping("/items")
    public ResponseEntity<?> createItem(@RequestParam String entityId, @Valid @RequestBody ItemDto itemDto) {
        try {
            ItemDto createdItem = menuService.createItem(entityId, itemDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
            
        } catch (Exception e) {
            logger.error("Failed to create item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create item"));
        }
    }
    
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateItem(@RequestParam String entityId, @PathVariable Long itemId, @Valid @RequestBody ItemDto itemDto) {
        try {
            ItemDto updatedItem = menuService.updateItem(entityId, itemId, itemDto);
            return ResponseEntity.ok(updatedItem);
            
        } catch (Exception e) {
            logger.error("Failed to update item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update item"));
        }
    }
    
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> deleteItem(@RequestParam String entityId, @PathVariable Long itemId) {
        try {
            menuService.deleteItem(entityId, itemId);
            return ResponseEntity.ok(Map.of("message", "Item deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to delete item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete item"));
        }
    }
}
