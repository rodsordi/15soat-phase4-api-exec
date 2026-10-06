package br.com.fiap.phase4.exec.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceMaterialDocument {

    private String sku;
    private String name;
    private int quantity;
}
