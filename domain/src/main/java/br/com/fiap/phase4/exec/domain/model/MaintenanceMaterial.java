package br.com.fiap.phase4.exec.domain.model;

import java.util.Objects;

public record MaintenanceMaterial(
        String sku,
        String name,
        int quantity
) {
    public MaintenanceMaterial {
        Objects.requireNonNull(sku, "SKU is required");
        Objects.requireNonNull(name, "Name is required");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    public static MaintenanceMaterial of(String sku, String name, int quantity) {
        return new MaintenanceMaterial(sku, name, quantity);
    }
}
