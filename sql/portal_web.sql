-- =====================================================================
-- BioRoute - Usuario tecnico "portal-web"
-- Ejecutar sobre una base bioroute_db ya existente (creada con una version
-- anterior de bioroute_db.sql). Es idempotente: si el usuario ya existe no hace nada.
--
-- Los pedidos creados desde el portal web quedan registrados con este usuario
-- (columna id_usuario de pedidos y estado_pedido). El hash 'SIN-LOGIN' nunca
-- coincide con un SHA-256, por lo que el usuario no puede iniciar sesion en la consola.
-- =====================================================================

USE bioroute_db;

INSERT INTO usuarios (nombre_usuario, hash_contrasena, sal, perfil)
SELECT 'portal-web', 'SIN-LOGIN', 'AAAAAAAAAAAAAAAAAAAAAA==', 'Administrativo'
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE nombre_usuario = 'portal-web');

SELECT id_usuario, nombre_usuario, perfil FROM usuarios WHERE nombre_usuario = 'portal-web';
