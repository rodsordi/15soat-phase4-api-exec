package br.com.fiap.phase4.exec.domain.model;

public enum ExecutionStatus {
    QUEUED,
    DIAGNOSIS,
    IN_REPAIR,
    WAITING_PARTS,
    QUALITY_CHECK,
    COMPLETED,
    FAILED
}
