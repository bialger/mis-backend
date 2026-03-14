-- Пользователи и роли: Employee, Specialty, Role, Permission + связующие таблицы

CREATE TABLE employee (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(50),
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    digital_signature BYTEA,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE specialty (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    description TEXT
);

CREATE TABLE permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255),
    description TEXT
);

CREATE TABLE employee_specialty (
    employee_id UUID NOT NULL REFERENCES employee(id),
    specialty_id UUID NOT NULL REFERENCES specialty(id),
    PRIMARY KEY (employee_id, specialty_id)
);

CREATE TABLE employee_branch (
    employee_id UUID NOT NULL REFERENCES employee(id),
    branch_id UUID NOT NULL REFERENCES branch(id),
    PRIMARY KEY (employee_id, branch_id)
);

CREATE TABLE employee_role (
    employee_id UUID NOT NULL REFERENCES employee(id),
    role_id UUID NOT NULL REFERENCES role(id),
    PRIMARY KEY (employee_id, role_id)
);

CREATE TABLE role_permission (
    role_id UUID NOT NULL REFERENCES role(id),
    permission_id UUID NOT NULL REFERENCES permission(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE employee_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employee(id),
    permission_id UUID NOT NULL REFERENCES permission(id),
    is_granted BOOLEAN NOT NULL
);
