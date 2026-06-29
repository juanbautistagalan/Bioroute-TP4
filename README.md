# Bioroute-TP4
# BioRoute

Sistema de gestión logística y trazabilidad de alimentos orgánicos desarrollado como prototipo integrador para el Seminario de Práctica de Informática (Módulo 4) de la Universidad Siglo 21.

El sistema permite administrar productos, clientes, pedidos y el ciclo completo de estados logísticos de una distribuidora mayorista y minorista de alimentos orgánicos certificados. La aplicación está hecha en Java con persistencia real sobre MySQL y aplica el patrón DAO para separar la lógica de acceso a datos del resto del sistema.


## Sobre el proyecto

BioRoute parte de una problemática concreta: una distribuidora que trabajaba con planillas de Excel compartidas por correo y pedidos tomados por WhatsApp, lo cual generaba errores de stock, falta de trazabilidad y demoras en la atención al cliente. El prototipo apunta a resolver esa problemática centralizando la información en una base de datos relacional y dejando registro de cada movimiento.

El alcance del prototipo no es el de un sistema productivo completo, sino un módulo operacional que integra los conceptos centrales del módulo: análisis de negocio, patrones de diseño, POO, persistencia con JDBC, manejo de excepciones, transacciones, archivos y autenticación segura.


## Funcionalidades

- Inicio de sesión con verificación de contraseña mediante hash SHA-256 con sal.
- Registro, listado y búsqueda de productos por nombre.
- Registro y listado de clientes.
- Creación de pedidos con validación de stock y transacción JDBC.
- Actualización de estados logísticos con historial.
- Consulta de trazabilidad completa por pedido.
- Consulta de productos con stock por debajo del mínimo.
- Ordenamiento de productos por nivel de stock.
- Registro de auditoría en archivo local.
- Prueba de conexión a la base de datos.


## Tecnologías

- Java 17 o superior.
- MySQL 8.x.
- JDBC mediante el conector oficial `mysql-connector-j`.
- Sin frameworks adicionales: el proyecto usa JDBC plano para mostrar el manejo manual de conexiones, transacciones y excepciones.


## Estructura del proyecto

```
bioroute-tp4/
  src/bioroute/
    Main.java
    config/
      ConexionBD.java
    dao/
      CrudRepository.java
      UsuarioDAO.java
      ProductoDAO.java
      ClienteDAO.java
      PedidoDAO.java
      EstadoPedidoDAO.java
    exception/
      DatosException.java
      ValidacionException.java
    modelo/
      Usuario.java
      Administrativo.java
      Repartidor.java
      Producto.java
      Cliente.java
      Pedido.java
      DetallePedido.java
      EstadoPedido.java
    util/
      HashUtil.java
      Validador.java
      AuditoriaArchivo.java
  sql/
    bioroute_db.sql
  lib/
    (acá va el conector JDBC descargado de MySQL)
  logs/
    (auditoria-bioroute.log se genera automáticamente)
  docs/
    GALAN-JUANBAUTISTA-AP4.pdf
  README.md
  INSTRUCCIONES.md
```


## Requisitos previos

- JDK 17 o superior instalado y configurado en el PATH.
- MySQL Server 8.x instalado y corriendo.
- MySQL Workbench (opcional, para administrar la base con interfaz gráfica).
- Conector JDBC de MySQL descargado desde https://dev.mysql.com/downloads/connector/j/ (versión Platform Independent).


## Instalación

### 1. Clonar el repositorio

```
git clone https://github.com/Juanigalan/BioRoute-Logistica.git
cd BioRoute-Logistica
```

### 2. Cargar la base de datos

Abrir MySQL Workbench, conectarse a la instancia local y ejecutar el script `sql/bioroute_db.sql`. El script crea la base `bioroute_db` con las seis tablas necesarias y carga datos iniciales: dos usuarios de prueba, tres clientes y cuatro productos.

### 3. Agregar el conector JDBC

Descargar `mysql-connector-j-X.X.X.jar` y copiarlo dentro de la carpeta `lib/`. Sin este archivo, Java no puede conectarse a MySQL.

### 4. Configurar credenciales de conexión

Editar el archivo `src/bioroute/config/ConexionBD.java` y reemplazar el valor de la constante `PASSWORD` por la contraseña real del usuario `root` de MySQL local.

```java
private static final String PASSWORD = "tu_password_de_mysql";
```


## Ejecución

Desde la raíz del proyecto:

```
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp "out;lib\mysql-connector-j-9.7.0.jar" bioroute.Main
```

En Linux o Mac, reemplazar `;` por `:` y `\` por `/` en el classpath.

Una vez que el sistema inicia, se solicita usuario y contraseña.


## Usuarios de prueba

| Usuario | Contraseña | Perfil |
|---|---|---|
| admin01 | admin123 | Administrativo |
| repartidor01 | reparto123 | Repartidor |

Las contraseñas reales no están almacenadas en la base. Lo que se guarda es el hash SHA-256 combinado con una sal aleatoria por usuario, codificado en Base64. Esto se verifica en `UsuarioDAO` usando la utilidad `HashUtil`.


## Modelo de datos

La base contiene seis tablas relacionadas:

- `usuarios`: credenciales y perfil. Almacena hash y sal por usuario.
- `clientes`: datos de contacto de los compradores.
- `productos`: catálogo con precio, stock actual y stock mínimo.
- `pedidos`: cabecera con cliente, usuario que tomó el pedido y estado actual.
- `detalle_pedido`: productos incluidos en cada pedido con cantidad y subtotal.
- `estado_pedido`: historial de cambios de estado con fecha, usuario y observación.

Las relaciones están definidas con claves foráneas. Los estados de pedido están restringidos por enumeración: Pendiente, En preparacion, En ruta, Entregado, Cancelado.


## Patrón de diseño

El proyecto usa el patrón DAO (Data Access Object). Esta decisión se tomó luego de comparar con MVC y Repository. DAO ofrece la mejor relación entre simplicidad y mantenibilidad para un prototipo con JDBC plano, sin agregar capas innecesarias para una aplicación de consola.

Cada entidad tiene su propio DAO. Los DAOs de `Producto` y `Cliente` implementan la interfaz genérica `CrudRepository<T>`, que define las operaciones básicas de creación y listado. Los DAOs de `Pedido` y `EstadoPedido` no la implementan porque su modelo de operación es transaccional y requiere parámetros adicionales que no encajan en el contrato genérico.


## Aplicación de POO

El código aplica los cuatro pilares de la programación orientada a objetos.

- Encapsulamiento: los atributos del paquete `modelo` son privados y se accede a ellos mediante getters y setters. Los setters incluyen validaciones cuando corresponde.
- Herencia: la clase abstracta `Usuario` define atributos y métodos comunes. `Administrativo` y `Repartidor` heredan de ella y especializan el menú y los permisos.
- Polimorfismo: el método `mostrarMenu()` se redefine en cada subclase. En el método `demostrarPolimorfismo()` de `Main` se usa una colección `List<Usuario>` que invoca el método correcto según el tipo real del objeto en tiempo de ejecución.
- Abstracción: la interfaz genérica `CrudRepository<T>` define un contrato común que implementan los DAOs concretos, lo que permite tratarlos de forma uniforme.


## Manejo de excepciones

El proyecto define dos excepciones personalizadas:

- `DatosException`: se lanza ante errores de conexión, ejecución de consultas o transacciones JDBC. Las `SQLException` originales se encadenan como causa para no perder el detalle técnico.
- `ValidacionException`: se lanza cuando el usuario ingresa datos inválidos por consola (texto vacío, número mal formateado, estado no permitido).

Ambas se capturan en el bucle principal de `Main`, mostrando un mensaje legible al usuario y, si aplica, el detalle técnico.


## Transacciones

La creación de un pedido se ejecuta dentro de una transacción JDBC. El flujo es el siguiente:

1. Se desactiva el auto-commit con `setAutoCommit(false)`.
2. Se valida el stock disponible para cada producto del pedido.
3. Se inserta la cabecera del pedido en la tabla `pedidos`.
4. Se insertan los detalles en `detalle_pedido` y se descuenta el stock en `productos`.
5. Se registra el estado inicial en `estado_pedido`.
6. Si todas las operaciones resultan exitosas, se ejecuta `commit()`.
7. Si cualquier paso falla, se ejecuta `rollback()` y se lanza una `DatosException`.

Esto evita inconsistencias como que se descuente stock sin haber registrado el pedido.


## Seguridad

El sistema implementa los controles mínimos esperables en un prototipo académico:

- Las contraseñas se almacenan como hash SHA-256 combinado con una sal aleatoria de 16 bytes por usuario. La sal y el hash se codifican en Base64 para facilitar el almacenamiento en MySQL.
- El acceso al sistema requiere autenticación previa.
- Cada perfil tiene permisos diferenciados controlados por el método `puedeEjecutar(opcion)`.
- Las operaciones críticas quedan registradas en el archivo de auditoría.

Como deuda técnica reconocida, no se implementa bloqueo por intentos fallidos ni cifrado de la base. Estos puntos quedan previstos para una versión productiva.


## Auditoría en archivo

La clase `AuditoriaArchivo` registra eventos relevantes en `logs/auditoria-bioroute.log`. Cada línea incluye timestamp, tipo de evento y descripción. Se registran inicio y cierre de sesión, alta de productos y clientes, creación de pedidos y cambios de estado. Esto cumple con el uso opcional de archivos solicitado por la consigna y permite reconstruir la actividad del sistema sin depender de la base de datos.


## Documentación

El informe técnico completo del trabajo práctico está en `docs/GALAN-JUANBAUTISTA-AP4.pdf`. Contiene el análisis de negocio (Business Model Canvas y FODA), la aplicación del Proceso Unificado de Desarrollo, los requerimientos funcionales y no funcionales, los diagramas UML, el plan de pruebas, la matriz de trazabilidad y las evidencias de ejecución.


## Limitaciones conocidas y próximos pasos

El prototipo no incluye interfaz gráfica, reportes en PDF, integraciones externas ni notificaciones por correo o WhatsApp. Estos puntos quedan como evolución futura. Las líneas de trabajo previstas son:

- Migrar la interfaz de consola a JavaFX o Swing.
- Exponer una API REST con Spring Boot para integraciones externas.
- Generar reportes en PDF de pedidos y stock.
- Implementar bloqueo por intentos fallidos de login.
- Dockerizar el sistema para facilitar el despliegue.
- Migrar la persistencia a una nube administrada.


## Autor

Juan Bautista Galán

Universidad Siglo 21 - Seminario de Práctica de Informática

La Plata, Buenos Aires, Argentina
