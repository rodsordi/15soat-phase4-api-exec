package br.com.fiap.phase4.exec.adapter.out.persistence.document;

public record MaintenanceMaterialDocument(
        String sku,
        String name,
        int quantity
) {}
