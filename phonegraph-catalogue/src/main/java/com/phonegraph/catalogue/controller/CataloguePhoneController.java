package com.phonegraph.catalogue.controller;

import com.phonegraph.catalogue.model.CataloguePhone;
import com.phonegraph.catalogue.repository.CataloguePhoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalogue REST API — GET /phones, GET /phones/{id}, GET /phones/search.
 * Read-only; exposes the existing phone_embeddings data as a catalogue.
 */
@RestController
@RequestMapping("/phones")
@RequiredArgsConstructor
public class CataloguePhoneController {

    private final CataloguePhoneRepository repository;

    @GetMapping
    public List<CataloguePhone> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CataloguePhone> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<CataloguePhone> search(@RequestParam String q) {
        return repository.search(q);
    }

    @GetMapping("/health")
    public String health() {
        return "Catalogue Service is running";
    }
}
