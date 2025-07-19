package com.devsuperior.dscatalog.services;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.devsuperior.dscatalog.controllers.ProductController;
import com.devsuperior.dscatalog.dto.CategoryDTO;
import com.devsuperior.dscatalog.dto.ProductDTO;
import com.devsuperior.dscatalog.entities.Category;
import com.devsuperior.dscatalog.entities.Product;
import com.devsuperior.dscatalog.projections.ProductProjection;
import com.devsuperior.dscatalog.repositories.CategoryRepository;
import com.devsuperior.dscatalog.repositories.ProductRepository;
import com.devsuperior.dscatalog.services.exceptions.DataBaseException;
import com.devsuperior.dscatalog.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dscatalog.util.Utils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductDTO> findAllPaged(String categoryId, String name, Pageable pageable) {

        // Prepara o filtro por categorias
        List<Long> categoryIds = new ArrayList<>();
        if (!"0".equals(categoryId)) {
            categoryIds = Arrays.stream(categoryId.split(","))
                    .map(Long::parseLong)
                    .toList();
        }

        // Busca paginada por projeções
        Page<ProductProjection> page = repository.searchProducts(categoryIds, name.trim(), pageable);

        // Pega os IDs para buscar os produtos completos com categorias
        List<Long> productIds = page.map(ProductProjection::getId).toList();
        List<Product> entities = repository.searchProductsWithCategories(productIds);

        // Reorganiza os produtos completos mantendo a ordem original
        entities = (List<Product>) Utils.replace(page.getContent(), entities);

        // Mapeia para DTOs e adiciona links HATEOAS manualmente
        List<ProductDTO> dtos = entities.stream().map(product -> {
            ProductDTO dto = new ProductDTO(product, product.getCategories());

            // Link para este próprio recurso (self)
            dto.add(linkTo(methodOn(ProductController.class)
                    .findAll(categoryId, name, pageable))
                    .withSelfRel());

            // Link para buscar produtos por Id
            dto.add(linkTo(methodOn(ProductController.class)
                    .findById(product.getId()))
                    .withRel("Get products by Id"));

            return dto;
        }).toList();

        // 6. Retorna a página com os DTOs com HATEOAS
        return new PageImpl<>(dtos, page.getPageable(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        Product product = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Recurso não localizado"));
        ProductDTO dto = new ProductDTO(product, product.getCategories());

                dto.add(linkTo(methodOn(ProductController.class).findById(id)).withSelfRel());
                dto.add(linkTo(methodOn(ProductController.class).findAll(null, null, null)).withRel("All products"));

        try {
            dto.add(linkTo(methodOn(ProductController.class).update(id, null)).withRel("Update product"));
            dto.add(linkTo(methodOn(ProductController.class).delete(id)).withRel("Delete product"));
        }
        catch (Exception e) {}

        return dto;
    }

    @Transactional
    public ProductDTO insert (ProductDTO dto) {
        Product product = new Product();
        copyDtoToEntity(dto, product);
        product = repository.save(product);
        return new ProductDTO(product)
                .add(linkTo(methodOn(ProductController.class).findById(product.getId())).withRel("Get product by Id"));
    }

    @Transactional
    public ProductDTO update(Long id, ProductDTO dto) {
        try {
            Product product = repository.getReferenceById(id);
            copyDtoToEntity(dto, product);
            product = repository.save(product);
            return new ProductDTO(product)
                    .add(linkTo(methodOn(ProductController.class).findById(product.getId())).withRel("Get product by Id"));
        }
        catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Id não localizado: " + id);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Id não localizado: " + id);
        }
        try {
            repository.deleteById(id);
        }
        catch (DataIntegrityViolationException e) {
            throw new DataBaseException("Violação de integridade");
        }
    }

    private void copyDtoToEntity(ProductDTO dto, Product product) {
        product.setName(dto.getName());
        product.setDescription((dto.getDescription()));
        product.setPrice(dto.getPrice());
        product.setImgUrl(dto.getImgUrl());
        product.setDate(dto.getDate());

        product.getCategories().clear();
        for (CategoryDTO catDto : dto.getCategories()) {
            Category category = categoryRepository.getReferenceById(catDto.getId());
            product.getCategories().add(category);
        }
    }

}
