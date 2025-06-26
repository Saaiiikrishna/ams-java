package com.example.attendancesystem.organization.controller;

import com.example.attendancesystem.dto.CategoryDto;
import com.example.attendancesystem.dto.ItemDto;
import com.example.attendancesystem.organization.model.FeaturePermission;
import com.example.attendancesystem.organization.service.MenuService;
import com.example.attendancesystem.organization.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuController {
    
    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);
    
    @Autowired
    private MenuService menuService;

    @Autowired
    private PermissionService permissionService;

    /**
     * Helper method to check menu management permission
     */
    private ResponseEntity<?> checkMenuPermission(String entityId) {
        if (!permissionService.hasPermission(entityId, FeaturePermission.MENU_MANAGEMENT)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Menu management permission required."));
        }
        return null;
    }
    
    // Category Management Endpoints
    
    @GetMapping("/categories")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getCategories() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            // Check permission
            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            List<CategoryDto> categories = menuService.getCategoriesByOrganization(entityId);
            return ResponseEntity.ok(categories);

        } catch (Exception e) {
            logger.error("Failed to get categories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/categories/with-items")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getCategoriesWithItems() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            List<CategoryDto> categories = menuService.getCategoriesWithItemsByOrganization(entityId);
            return ResponseEntity.ok(categories);

        } catch (Exception e) {
            logger.error("Failed to get categories with items: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            CategoryDto createdCategory = menuService.createCategory(entityId, categoryDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);

        } catch (Exception e) {
            logger.error("Failed to create category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/categories/{categoryId}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryDto categoryDto) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            CategoryDto updatedCategory = menuService.updateCategory(entityId, categoryId, categoryDto);
            return ResponseEntity.ok(updatedCategory);
            
        } catch (Exception e) {
            logger.error("Failed to update category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> deleteCategory(@PathVariable Long categoryId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            menuService.deleteCategory(entityId, categoryId);
            return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to delete category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // Item Management Endpoints
    
    @GetMapping("/items")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getItems() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            List<ItemDto> items = menuService.getItemsByOrganization(entityId);
            return ResponseEntity.ok(items);
            
        } catch (Exception e) {
            logger.error("Failed to get items: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/categories/{categoryId}/items")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> getItemsByCategory(@PathVariable Long categoryId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            List<ItemDto> items = menuService.getItemsByCategory(entityId, categoryId);
            return ResponseEntity.ok(items);
            
        } catch (Exception e) {
            logger.error("Failed to get items by category: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/items")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> createItem(@Valid @RequestBody ItemDto itemDto) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            ItemDto createdItem = menuService.createItem(entityId, itemDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
            
        } catch (Exception e) {
            logger.error("Failed to create item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> updateItem(@PathVariable Long itemId, @Valid @RequestBody ItemDto itemDto) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            ItemDto updatedItem = menuService.updateItem(entityId, itemId, itemDto);
            return ResponseEntity.ok(updatedItem);
            
        } catch (Exception e) {
            logger.error("Failed to update item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('ENTITY_ADMIN')")
    public ResponseEntity<?> deleteItem(@PathVariable Long itemId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String entityId = ((com.example.attendancesystem.security.CustomUserDetails) auth.getPrincipal()).getEntityAdmin().getOrganization().getEntityId();

            ResponseEntity<?> permissionCheck = checkMenuPermission(entityId);
            if (permissionCheck != null) return permissionCheck;

            menuService.deleteItem(entityId, itemId);
            return ResponseEntity.ok(Map.of("message", "Item deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Failed to delete item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
