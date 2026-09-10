#
integrantes: 
Daimerson Smith Avila
Frank Erick Joel Diaz
Cesar Andres Castellanos Velandia 

# mvc

Proyecto de ejemplo de **Arquitectura MVC con Quarkus** para
Desarrollo Orientado a Servicios (2026-2).

Aplicación **CRUD completo (las 4 operaciones)** de productos y clientes, organizada en
las tres capas del patrón MVC (**Modelo · Vista · Controlador**), con **Bootstrap** en la
interfaz y **MySQL (XAMPP)** como base de datos.

## Taller - Clientes, stock y relación entre tablas

Este proyecto parte de la base de la Semana 4 y agrega lo pedido en el taller de la
Semana 5:

1. **Stock en productos**: la entidad `Product` ahora tiene el campo `stock`
   (entero, no nulo, validado como `>= 0` en `ProductController`). Se muestra en
   `index.html`, `create.html` y `edit.html`.
2. **Entidad `Client`** (tabla `clients`) con `id`, `name`, `email`, repositorio
   Panache (`ClientRepository`) y CRUD completo en `ClientController` bajo la ruta
   `/clientes`, con validación de formato de correo.
3. **Relación muchos-a-muchos** `Client` ⇄ `Product` mediante `@ManyToMany` y la
   tabla intermedia `client_products` (`client_id`, `product_id`). El listado de
   clientes muestra la columna **Productos** (`{client.purchasedCount}`) y el
   formulario de edición (`clientes/edit.html`) presenta un checklist de todos los
   productos; al actualizar, el controlador sincroniza la colección con
   `addProduct` / `removeProduct`.

## Cómo crear el proyecto (extensiones en el pom)

Al crear el proyecto Quarkus no vienen todas las dependencias por defecto; hay que añadir
las extensiones de **REST, Qute, Hibernate ORM, Panache y MySQL**. Bootstrap **no** va en
el pom: se carga por CDN en los HTML (`<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/...">`).

**Opción A – code.quarkus.io:** en el campo *Extensions* marca:

| Extensión seleccionada | Artefacto en el pom | Rol |
|------------------------|---------------------|-----|
| REST | `quarkus-rest` | Controlador |
| REST Qute | `quarkus-rest-qute` | Vista (plantillas HTML) |
| Hibernate ORM | `quarkus-hibernate-orm` | Modelo (entidad JPA) |
| Hibernate ORM with Panache | `quarkus-hibernate-orm-panache` | Repositorio (CRUD) |
| JDBC Driver - MySQL | `quarkus-jdbc-mysql` | Conexión a MySQL (XAMPP) |

> Nota: **CDI** (`quarkus-arc`) viene incluido automáticamente como dependencia de las otras
> extensiones, no hace falta marcarlo.

**Opción B - comando Maven** (crea el proyecto con esas extensiones):

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.38.3:create \
  -DprojectGroupId=usta -DprojectArtifactId=mvc \
  -Dextensions="rest,rest-qute,hibernate-orm,hibernate-orm-panache,jdbc-mysql"
```

**Configuración de la base de datos** que debe quedar en `src/main/resources/application.properties`:

```properties
quarkus.datasource.db-kind=mysql
quarkus.datasource.username=root
quarkus.datasource.password=
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/mvc
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.database.version-check.enabled=false
```

> XAMPP usa **MariaDB** (que se reporta como versión 5.5.5 por compatibilidad); por eso se
> desactiva la verificación de versión de Hibernate, si no el arranque falla.

## Estructura del proyecto (MVC)

```
src/main/java/usta/
├── model/                        ← MODELO
│   ├── Product.java                  Entidad JPA (tabla products, incluye stock)
│   ├── ProductRepository.java        Repositorio con Panache (CRUD)
│   ├── Client.java                   Entidad JPA (tabla clients, relación @ManyToMany)
│   └── ClientRepository.java         Repositorio con Panache (CRUD)
└── controller/                   ← CONTROLADOR
    ├── ProductController.java        Rutas /productos (JAX-RS)
    └── ClientController.java         Rutas /clientes (JAX-RS)

src/main/resources/
├── application.properties         ← conexión a MySQL
└── templates/                    ← VISTA (Bootstrap + plantillas Qute)
    ├── index.html                     Listado de productos (READ)
    ├── create.html                    Formulario de creación de producto (CREATE)
    ├── edit.html                      Formulario de edición de producto (UPDATE)
    └── clientes/
        ├── index.html                 Listado de clientes (con columna Productos)
        ├── form.html                  Formulario de creación de cliente (CREATE)
        └── edit.html                  Formulario de edición + checklist de productos (UPDATE)
```

## Las 4 operaciones CRUD

| Operación | Ruta (URL)              | Acción del controlador |
|-----------|-------------------------|------------------------|
| **C**reate | `POST /productos/guardar`    | `save()`   |
| **R**ead   | `GET /productos`             | `list()`   |
| **U**pdate | `POST /productos/actualizar` | `update()` |
| **D**elete | `GET /productos/eliminar/{id}` | `delete()` |

Lo mismo aplica para clientes bajo `/clientes` (`ClientController`):
`GET /clientes`, `GET /clientes/nuevo`, `POST /clientes/guardar`,
`GET /clientes/editar/{id}`, `POST /clientes/actualizar`,
`GET /clientes/eliminar/{id}`.

## Cómo ejecutarlo

Requisitos: JDK 21, Maven (o `./mvnw`) y **XAMPP con MySQL corriendo**.

1. Iniciar Apache y MySQL desde el panel de XAMPP.
2. Crear la base de datos (en phpMyAdmin o en la consola de MySQL):

   ```sql
   CREATE DATABASE mvc;
   ```

3. Ejecutar la aplicación:

   ```shell script
   ./mvnw quarkus:dev
   ```

4. Abrir en el navegador:

   * Listado de productos: <http://localhost:8080/productos>
   * Crear producto: <http://localhost:8080/productos/nuevo>
   * Editar producto: <http://localhost:8080/productos/editar/1>
   * Listado de clientes: <http://localhost:8080/clientes>
   * Crear cliente: <http://localhost:8080/clientes/nuevo>
   * Editar cliente (asignar productos): <http://localhost:8080/clientes/editar/1>

La tabla `products` se crea automáticamente (Hibernate ORM).

## Flujo de una petición

1. El usuario abre `http://localhost:8080/productos`.
2. El **Controlador** `ProductController.list()` recibe la petición.
3. El **Controlador** consulta al **Modelo** (`ProductRepository` → MySQL).
4. El **Controlador** entrega los datos a la **Vista** (`index.html` con Bootstrap).
5. La **Vista** genera el HTML que el usuario ve.

## Empaguetar la aplicación

```shell script
./mvnw package
```

Genera `target/quarkus-app/quarkus-run.jar` y se ejecuta con:

```shell script
java -jar target/quarkus-app/quarkus-run.jar
```
