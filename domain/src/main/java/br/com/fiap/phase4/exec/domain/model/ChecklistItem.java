package br.com.fiap.phase4.exec.domain.model;

import java.time.Instant;
import java.util.Objects;

public record ChecklistItem(String task, boolean completed, Instant completedAt) {
    public ChecklistItem {
        Objects.requireNonNull(task, "Checklist task description cannot be null");
    }

    public static ChecklistItem of(String task, boolean completed) {
        return new ChecklistItem(task, completed, completed ? Instant.now() : null);
    }
}
