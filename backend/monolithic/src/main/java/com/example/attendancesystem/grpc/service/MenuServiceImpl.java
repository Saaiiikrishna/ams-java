package com.example.attendancesystem.subscriber.grpc;

import com.example.attendancesystem.grpc.menu.*;
import com.example.attendancesystem.dto.CategoryDto;
import com.example.attendancesystem.dto.ItemDto;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.service.MenuService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Menu Service Implementation for gRPC
 * Complete implementation using existing MenuService
 */
@GrpcService
public class MenuServiceImpl extends MenuServiceGrpc.MenuServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(MenuServiceImpl.class);

    @Autowired
    private MenuService menuService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Override
    public void createCategory(CreateCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
        logger.info("Creating category: {}", request.getName());

        try {
            // Validate organization
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getOrganizationId());
            if (organizationOpt.isEmpty()) {
                CategoryResponse response = CategoryResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Create category DTO
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setName(request.getName());
            categoryDto.setDescription(request.getDescription());
            categoryDto.setDisplayOrder(request.getSortOrder());

            // Create category using existing service
            CategoryDto createdCategory = menuService.createCategory(organizationOpt.get().getEntityId(), categoryDto);

            // Convert to gRPC response
            Category grpcCategory = convertToGrpcCategory(createdCategory);

            CategoryResponse response = CategoryResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Category created successfully")
                    .setCategory(grpcCategory)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error creating category", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create category: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getCategory(GetCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
        logger.info("Getting category ID: {}", request.getId());

        try {
            // For now, we'll implement a simple approach since getCategoryById doesn't exist
            // We'll get all categories and find the one we need
            List<CategoryDto> allCategories = menuService.getCategoriesByOrganization("default");
            CategoryDto categoryDto = allCategories.stream()
                    .filter(cat -> cat.getId().equals(request.getId()))
                    .findFirst()
                    .orElse(null);

            if (categoryDto == null) {
                CategoryResponse response = CategoryResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Category not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Convert to gRPC response
            Category grpcCategory = convertToGrpcCategory(categoryDto);

            CategoryResponse response = CategoryResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Category retrieved successfully")
                    .setCategory(grpcCategory)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error getting category", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get category: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateCategory(UpdateCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
        logger.info("Menu service - updateCategory called for ID: {}", request.getId());
        
        try {
            CategoryResponse response = CategoryResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - category update pending")
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.error("Error in updateCategory", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update category: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteCategory(DeleteCategoryRequest request, StreamObserver<DeleteResponse> responseObserver) {
        logger.info("Menu service - deleteCategory called for ID: {}", request.getId());
        
        try {
            DeleteResponse response = DeleteResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - category deletion pending")
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.error("Error in deleteCategory", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to delete category: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listCategories(ListCategoriesRequest request, StreamObserver<ListCategoriesResponse> responseObserver) {
        logger.info("Listing categories for organization: {}", request.getOrganizationId());

        try {
            // Validate organization
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getOrganizationId());
            if (organizationOpt.isEmpty()) {
                ListCategoriesResponse response = ListCategoriesResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .setTotalCount(0)
                        .setPage(request.getPage())
                        .setSize(request.getSize())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Get categories using existing service
            List<CategoryDto> categories = menuService.getCategoriesByOrganization(organizationOpt.get().getEntityId());

            // Convert to gRPC response
            List<Category> grpcCategories = categories.stream()
                    .map(this::convertToGrpcCategory)
                    .collect(Collectors.toList());

            ListCategoriesResponse response = ListCategoriesResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Categories retrieved successfully")
                    .addAllCategories(grpcCategories)
                    .setTotalCount(categories.size())
                    .setPage(request.getPage())
                    .setSize(request.getSize())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error listing categories", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to list categories: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createItem(CreateItemRequest request, StreamObserver<ItemResponse> responseObserver) {
        logger.info("Creating item: {}", request.getName());

        try {
            // Validate organization
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getOrganizationId());
            if (organizationOpt.isEmpty()) {
                ItemResponse response = ItemResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Organization not found")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Create item DTO
            ItemDto itemDto = new ItemDto();
            itemDto.setName(request.getName());
            itemDto.setDescription(request.getDescription());
            itemDto.setPrice(BigDecimal.valueOf(request.getPrice()));
            itemDto.setCategoryId(request.getCategoryId());
            itemDto.setImageUrl(request.getImageUrl());
            itemDto.setDisplayOrder(request.getSortOrder());

            // Create item using existing service
            ItemDto createdItem = menuService.createItem(organizationOpt.get().getEntityId(), itemDto);

            // Convert to gRPC response
            Item grpcItem = convertToGrpcItem(createdItem);

            ItemResponse response = ItemResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Item created successfully")
                    .setItem(grpcItem)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error creating item", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to create item: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getItem(GetItemRequest request, StreamObserver<ItemResponse> responseObserver) {
        logger.info("Menu service - getItem called for ID: {}", request.getId());

        try {
            ItemResponse response = ItemResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - item retrieval pending")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error in getItem", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get item: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateItem(UpdateItemRequest request, StreamObserver<ItemResponse> responseObserver) {
        logger.info("Menu service - updateItem called for ID: {}", request.getId());

        try {
            ItemResponse response = ItemResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - item update pending")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error in updateItem", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to update item: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteItem(DeleteItemRequest request, StreamObserver<DeleteResponse> responseObserver) {
        logger.info("Menu service - deleteItem called for ID: {}", request.getId());

        try {
            DeleteResponse response = DeleteResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - item deletion pending")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error in deleteItem", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to delete item: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listItems(ListItemsRequest request, StreamObserver<ListItemsResponse> responseObserver) {
        logger.info("Menu service - listItems called for organization: {}", request.getOrganizationId());

        try {
            ListItemsResponse response = ListItemsResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - item listing pending")
                    .setTotalCount(0)
                    .setPage(request.getPage())
                    .setSize(request.getSize())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error in listItems", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to list items: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getPublicMenu(GetPublicMenuRequest request, StreamObserver<PublicMenuResponse> responseObserver) {
        logger.info("Menu service - getPublicMenu called for organization: {}", request.getOrganizationId());
        
        try {
            PublicMenuResponse response = PublicMenuResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - public menu retrieval pending")
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.error("Error in getPublicMenu", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get public menu: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listItemsByCategory(ListItemsByCategoryRequest request, StreamObserver<ListItemsResponse> responseObserver) {
        logger.info("Menu service - listItemsByCategory called for category: {}", request.getCategoryId());

        try {
            ListItemsResponse response = ListItemsResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - items by category listing pending")
                    .setTotalCount(0)
                    .setPage(request.getPage())
                    .setSize(request.getSize())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error in listItemsByCategory", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to list items by category: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getPublicCategory(GetPublicCategoryRequest request, StreamObserver<CategoryResponse> responseObserver) {
        logger.info("Menu service - getPublicCategory called for ID: {}", request.getId());

        try {
            CategoryResponse response = CategoryResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - public category retrieval pending")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error in getPublicCategory", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get public category: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getPublicItem(GetPublicItemRequest request, StreamObserver<ItemResponse> responseObserver) {
        logger.info("Menu service - getPublicItem called for ID: {}", request.getId());

        try {
            ItemResponse response = ItemResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - public item retrieval pending")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error in getPublicItem", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get public item: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void searchMenuItems(SearchMenuItemsRequest request, StreamObserver<ListItemsResponse> responseObserver) {
        logger.info("Menu service - searchMenuItems called with query: {}", request.getSearchQuery());

        try {
            ListItemsResponse response = ListItemsResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Menu service not implemented yet - menu item search pending")
                    .setTotalCount(0)
                    .setPage(request.getPage())
                    .setSize(request.getSize())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error in searchMenuItems", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to search menu items: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // Conversion methods
    private Category convertToGrpcCategory(CategoryDto categoryDto) {
        return Category.newBuilder()
                .setId(categoryDto.getId())
                .setName(categoryDto.getName())
                .setDescription(categoryDto.getDescription() != null ? categoryDto.getDescription() : "")
                .setSortOrder(categoryDto.getDisplayOrder() != null ? categoryDto.getDisplayOrder() : 0)
                .setActive(categoryDto.getIsActive() != null ? categoryDto.getIsActive() : true)
                .setOrganizationId(0) // Not available in DTO, using default
                .setCreatedAt("") // Not available in DTO
                .setUpdatedAt("") // Not available in DTO
                .build();
    }

    private Item convertToGrpcItem(ItemDto itemDto) {
        return Item.newBuilder()
                .setId(itemDto.getId())
                .setName(itemDto.getName())
                .setDescription(itemDto.getDescription() != null ? itemDto.getDescription() : "")
                .setPrice(itemDto.getPrice() != null ? itemDto.getPrice().doubleValue() : 0.0)
                .setImageUrl(itemDto.getImageUrl() != null ? itemDto.getImageUrl() : "")
                .setSortOrder(itemDto.getDisplayOrder() != null ? itemDto.getDisplayOrder() : 0)
                .setActive(itemDto.getIsActive() != null ? itemDto.getIsActive() : true)
                .setAvailable(itemDto.getIsAvailable() != null ? itemDto.getIsAvailable() : true)
                .setCategoryId(itemDto.getCategoryId() != null ? itemDto.getCategoryId() : 0)
                .setOrganizationId(0) // Not available in DTO, using default
                .setCreatedAt("") // Not available in DTO
                .setUpdatedAt("") // Not available in DTO
                .build();
    }
}
