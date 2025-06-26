package com.example.attendancesystem.organization.service;

import com.example.attendancesystem.dto.*;
import com.example.attendancesystem.organization.model.*;
import com.example.attendancesystem.organization.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MenuService {
    
    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    // Category Management
    
    public List<CategoryDto> getCategoriesByOrganization(String entityId) {
        logger.debug("Getting categories for organization: {}", entityId);
        List<Category> categories = categoryRepository.findActiveByOrganizationEntityId(entityId);
        return categories.stream().map(this::convertToCategoryDto).collect(Collectors.toList());
    }
    
    public List<CategoryDto> getCategoriesWithItemsByOrganization(String entityId) {
        logger.debug("Getting categories with items for organization: {}", entityId);
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        List<Category> categories = categoryRepository.findCategoriesWithItems(organization);
        return categories.stream().map(this::convertToCategoryDtoWithItems).collect(Collectors.toList());
    }
    
    public CategoryDto createCategory(String entityId, CategoryDto categoryDto) {
        logger.info("Creating category '{}' for organization: {}", categoryDto.getName(), entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        // Check if category name already exists
        if (categoryRepository.existsByNameAndOrganization(categoryDto.getName(), organization)) {
            throw new RuntimeException("Category with name '" + categoryDto.getName() + "' already exists");
        }
        
        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setImageUrl(categoryDto.getImageUrl());
        category.setDisplayOrder(categoryDto.getDisplayOrder() != null ? categoryDto.getDisplayOrder() : 0);
        category.setIsActive(categoryDto.getIsActive() != null ? categoryDto.getIsActive() : true);
        category.setOrganization(organization);
        
        Category savedCategory = categoryRepository.save(category);
        logger.info("Category '{}' created with ID: {}", savedCategory.getName(), savedCategory.getId());
        
        return convertToCategoryDto(savedCategory);
    }
    
    public CategoryDto updateCategory(String entityId, Long categoryId, CategoryDto categoryDto) {
        logger.info("Updating category {} for organization: {}", categoryId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Category category = categoryRepository.findByIdAndOrganization(categoryId, organization)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Check if new name conflicts with existing categories
        if (!category.getName().equals(categoryDto.getName()) && 
            categoryRepository.existsByNameAndOrganizationAndIdNot(categoryDto.getName(), organization, categoryId)) {
            throw new RuntimeException("Category with name '" + categoryDto.getName() + "' already exists");
        }
        
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setImageUrl(categoryDto.getImageUrl());
        category.setDisplayOrder(categoryDto.getDisplayOrder());
        category.setIsActive(categoryDto.getIsActive());
        
        Category savedCategory = categoryRepository.save(category);
        logger.info("Category '{}' updated", savedCategory.getName());
        
        return convertToCategoryDto(savedCategory);
    }
    
    public void deleteCategory(String entityId, Long categoryId) {
        logger.info("Deleting category {} for organization: {}", categoryId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Category category = categoryRepository.findByIdAndOrganization(categoryId, organization)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Check if category has items
        long itemCount = itemRepository.countByCategory(category);
        if (itemCount > 0) {
            throw new RuntimeException("Cannot delete category with existing items. Please delete or move items first.");
        }
        
        categoryRepository.delete(category);
        logger.info("Category '{}' deleted", category.getName());
    }
    
    // Item Management
    
    public List<ItemDto> getItemsByOrganization(String entityId) {
        logger.debug("Getting items for organization: {}", entityId);
        List<Item> items = itemRepository.findAvailableByOrganizationEntityId(entityId);
        return items.stream().map(this::convertToItemDto).collect(Collectors.toList());
    }
    
    public List<ItemDto> getItemsByCategory(String entityId, Long categoryId) {
        logger.debug("Getting items for category {} in organization: {}", categoryId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Category category = categoryRepository.findByIdAndOrganization(categoryId, organization)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        List<Item> items = itemRepository.findByCategoryAndIsActiveTrueAndIsAvailableTrueOrderByDisplayOrderAscNameAsc(category);
        return items.stream().map(this::convertToItemDto).collect(Collectors.toList());
    }
    
    public ItemDto createItem(String entityId, ItemDto itemDto) {
        logger.info("Creating item '{}' for organization: {}", itemDto.getName(), entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Category category = categoryRepository.findByIdAndOrganization(itemDto.getCategoryId(), organization)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Check if item name already exists in category
        if (itemRepository.existsByNameAndCategory(itemDto.getName(), category)) {
            throw new RuntimeException("Item with name '" + itemDto.getName() + "' already exists in this category");
        }
        
        Item item = new Item();
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        item.setPrice(itemDto.getPrice());
        item.setImageUrl(itemDto.getImageUrl());
        item.setDisplayOrder(itemDto.getDisplayOrder() != null ? itemDto.getDisplayOrder() : 0);
        item.setIsActive(itemDto.getIsActive() != null ? itemDto.getIsActive() : true);
        item.setIsAvailable(itemDto.getIsAvailable() != null ? itemDto.getIsAvailable() : true);
        item.setCategory(category);
        item.setOrganization(organization);
        
        Item savedItem = itemRepository.save(item);
        logger.info("Item '{}' created with ID: {}", savedItem.getName(), savedItem.getId());
        
        return convertToItemDto(savedItem);
    }
    
    public ItemDto updateItem(String entityId, Long itemId, ItemDto itemDto) {
        logger.info("Updating item {} for organization: {}", itemId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Item item = itemRepository.findByIdAndOrganization(itemId, organization)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        
        // If category is being changed, validate new category
        if (itemDto.getCategoryId() != null && !itemDto.getCategoryId().equals(item.getCategory().getId())) {
            Category newCategory = categoryRepository.findByIdAndOrganization(itemDto.getCategoryId(), organization)
                    .orElseThrow(() -> new RuntimeException("New category not found"));
            item.setCategory(newCategory);
        }
        
        // Check if new name conflicts with existing items in the category
        if (!item.getName().equals(itemDto.getName()) && 
            itemRepository.existsByNameAndCategoryAndIdNot(itemDto.getName(), item.getCategory(), itemId)) {
            throw new RuntimeException("Item with name '" + itemDto.getName() + "' already exists in this category");
        }
        
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        item.setPrice(itemDto.getPrice());
        item.setImageUrl(itemDto.getImageUrl());
        item.setDisplayOrder(itemDto.getDisplayOrder());
        item.setIsActive(itemDto.getIsActive());
        item.setIsAvailable(itemDto.getIsAvailable());
        
        Item savedItem = itemRepository.save(item);
        logger.info("Item '{}' updated", savedItem.getName());
        
        return convertToItemDto(savedItem);
    }
    
    public void deleteItem(String entityId, Long itemId) {
        logger.info("Deleting item {} for organization: {}", itemId, entityId);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        Item item = itemRepository.findByIdAndOrganization(itemId, organization)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        
        itemRepository.delete(item);
        logger.info("Item '{}' deleted", item.getName());
    }
    
    // Conversion methods
    
    private CategoryDto convertToCategoryDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setImageUrl(category.getImageUrl());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setIsActive(category.getIsActive());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }
    
    private CategoryDto convertToCategoryDtoWithItems(Category category) {
        CategoryDto dto = convertToCategoryDto(category);
        List<ItemDto> items = category.getItems().stream()
                .filter(item -> item.getIsActive() && item.getIsAvailable())
                .map(this::convertToItemDto)
                .collect(Collectors.toList());
        dto.setItems(items);
        return dto;
    }
    
    private ItemDto convertToItemDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setPrice(item.getPrice());
        dto.setImageUrl(item.getImageUrl());
        dto.setDisplayOrder(item.getDisplayOrder());
        dto.setIsActive(item.getIsActive());
        dto.setIsAvailable(item.getIsAvailable());
        dto.setCategoryId(item.getCategory().getId());
        dto.setCategoryName(item.getCategory().getName());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }
}
