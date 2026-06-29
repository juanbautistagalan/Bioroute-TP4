-- =====================================================================
-- BioRoute - Script de base de datos
-- Trabajo Practico Nro. 4 - Seminario de Practica de Informatica
-- Universidad Siglo 21
-- =====================================================================

DROP DATABASE IF EXISTS bioroute_db;
CREATE DATABASE bioroute_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bioroute_db;

-- ---------------------------------------------------------------------
-- Tablas
-- ---------------------------------------------------------------------

CREATE TABLE usuarios (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
  hash_contrasena VARCHAR(255) NOT NULL,
  sal VARCHAR(255) NOT NULL,
  perfil ENUM('Administrativo', 'Repartidor') NOT NULL
);

CREATE TABLE clientes (
  id_cliente INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  telefono VARCHAR(20),
  direccion VARCHAR(200) NOT NULL,
  email VARCHAR(100)
);

CREATE TABLE productos (
  id_producto INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  categoria VARCHAR(50),
  precio_unitario DECIMAL(10,2) NOT NULL,
  stock_actual INT NOT NULL DEFAULT 0,
  stock_minimo INT NOT NULL DEFAULT 0
);

CREATE TABLE pedidos (
  id_pedido INT AUTO_INCREMENT PRIMARY KEY,
  id_cliente INT NOT NULL,
  id_usuario INT NOT NULL,
  fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
  estado_actual ENUM('Pendiente', 'En preparacion', 'En ruta', 'Entregado', 'Cancelado')
    NOT NULL DEFAULT 'Pendiente',
  FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE TABLE detalle_pedido (
  id_detalle INT AUTO_INCREMENT PRIMARY KEY,
  id_pedido INT NOT NULL,
  id_producto INT NOT NULL,
  cantidad INT NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
  FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

CREATE TABLE estado_pedido (
  id_estado INT AUTO_INCREMENT PRIMARY KEY,
  id_pedido INT NOT NULL,
  estado ENUM('Pendiente', 'En preparacion', 'En ruta', 'Entregado', 'Cancelado') NOT NULL,
  fecha_hora DATETIME DEFAULT CURRENT_TIMESTAMP,
  id_usuario INT NOT NULL,
  observacion VARCHAR(250),
  FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- ---------------------------------------------------------------------
-- Datos iniciales
-- Usuarios de prueba:
--   admin01       /  admin123       (perfil Administrativo)
--   repartidor01  /  reparto123     (perfil Repartidor)
-- ---------------------------------------------------------------------

INSERT INTO usuarios (nombre_usuario, hash_contrasena, sal, perfil) VALUES
('admin01',
 'ADYpaERjYqbSJyMGs83pUyu1B5q31H+MEbixwba65+M=',
 '25frZp1wW8dazEcj2VQk+Q==',
 'Administrativo'),
('repartidor01',
 'VF0SZ6zY9+wBoU/hhh/vXVO9FLSYsL+DsLfgVDca+PQ=',
 'vxMcmlRcZJBpCsZIyibbbQ==',
 'Repartidor');

INSERT INTO clientes (nombre, telefono, direccion, email) VALUES
('Almacen Verde',    '3515551001', 'Av. Colon 1200, Cordoba', 'compras@almacenverde.com'),
('BioMarket Centro', '3515551002', 'San Martin 340, Cordoba',  'pedidos@biomarket.com'),
('Dietetica Natura', '2215551003', 'Calle 7 Nro 850, La Plata','contacto@natura.com');

INSERT INTO productos (nombre, categoria, precio_unitario, stock_actual, stock_minimo) VALUES
('Miel organica',            'Endulzantes', 2500.00, 50, 10),
('Yerba organica',           'Infusiones',  1800.00,  8, 12),
('Aceite de oliva organico', 'Almacen',     5200.00, 20,  5),
('Granola natural',          'Cereales',    1600.00, 30, 10);

-- ---------------------------------------------------------------------
-- Verificacion
-- ---------------------------------------------------------------------
SELECT 'Usuarios cargados:' AS info;
SELECT id_usuario, nombre_usuario, perfil FROM usuarios;

SELECT 'Productos cargados:' AS info;
SELECT * FROM productos;

SELECT 'Clientes cargados:' AS info;
SELECT * FROM clientes;
