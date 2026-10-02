package br.com.fiap.phase4.exec.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemDocument {

    private String task;
    private boolean completed;
    private Instant completedAt;
}
