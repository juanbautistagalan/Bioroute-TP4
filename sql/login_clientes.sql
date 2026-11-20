-- Cuentas de acceso de clientes al portal web (email + contrasena).
-- Idempotente: se puede ejecutar sobre una base existente.
USE bioroute_db;

CREATE TABLE IF NOT EXISTS cuentas_cliente (
  id_cliente INT PRIMARY KEY,
  email VARCHAR(100) NOT NULL UNIQUE,
  hash_contrasena VARCHAR(255) NOT NULL,
  sal VARCHAR(255) NOT NULL,
  fecha_alta DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente)
);
