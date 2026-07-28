CREATE TABLE IF NOT EXISTS field_employee_assignments (
    id UUID PRIMARY KEY,
    field_id UUID NOT NULL REFERENCES fields(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_field_employee_assignment UNIQUE (field_id, employee_id)
);

CREATE INDEX IF NOT EXISTS idx_field_employee_assignments_employee ON field_employee_assignments(employee_id);
CREATE INDEX IF NOT EXISTS idx_field_employee_assignments_field ON field_employee_assignments(field_id);
