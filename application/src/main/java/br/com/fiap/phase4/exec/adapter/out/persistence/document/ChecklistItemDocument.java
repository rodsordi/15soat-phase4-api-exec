package br.com.fiap.phase4.exec.adapter.out.persistence.document;

import java.time.Instant;

public record ChecklistItemDocument(
        String task,
        boolean completed,
        Instant completedAt
) {}
