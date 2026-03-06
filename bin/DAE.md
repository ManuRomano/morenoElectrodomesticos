# Documento de Análisis de Especificaciones (DAE)
## Moreno Electrodomésticos — Gestión de Inventario

---

## 1. Core Identity
| Atributo | Valor |
|---|---|
| Tipo de app | Desktop nativa (JavaFX 21) |
| Política de datos | Local-First (H2 FILE) |
| Autenticación | Sin login |
| Plataforma objetivo | Windows 10/11 (64-bit) |

---

## 2. Arquitectura

```
ElectrodomesticosApp (javafx.application.Application)
    └─ init()  →  SpringApplication.run(SpringBootConfig)
    └─ start() →  SpringFXMLLoader carga main.fxml
                     └─ MainController  (Spring @Component)
                           ├─ ElectrodomesticoService  (@Service)
                           │    └─ ElectrodomesticoRepository  (JpaRepository)
                           │         └─ H2 File DB  (./db/inventario)
                           └─ PdfService  (@Service)
                                └─ iText 8  →  PDF A4 con etiquetas A6
```

**Integración Spring + JavaFX:** `SpringFXMLLoader` envuelve `FXMLLoader`
con `setControllerFactory(context::getBean)`, permitiendo `@Autowired`/constructor
injection en todos los controllers FXML.

---

## 3. Pila Tecnológica

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 (LTS) |
| Framework backend | Spring Boot | 3.2.3 |
| Persistencia | Spring Data JPA + Hibernate | (boot-managed) |
| Base de datos | H2 Database (FILE mode) | (boot-managed) |
| Interfaz | JavaFX | 21.0.2 |
| Diseño UI | FXML + CSS | — |
| Generación PDF | iText 8 | 8.0.3 |
| Utilidades | Lombok | (boot-managed) |
| Build | Maven | 3.9+ |

---

## 4. Modelo de Datos

### Entidad `Electrodomestico`
```java
@Entity @Table(name = "electrodomesticos")
Long   id                         // PK auto-increment
String tipo                       // NOT NULL  (Frigorífico, Lavadora, …)
String marca                      // NOT NULL  (Samsung, LG, …)
String modelo                     // NOT NULL
BigDecimal precio                 // NOT NULL  (10,2)
String clasificacionEnergetica    // A, A+, A++, A+++, B, C, …
String dimensiones                // "595×595×2010mm"
String especificacionesPrincipales // VARCHAR(500)
```

---

## 5. Requisitos Funcionales

### 5.1 CRUD con TableView
- `TableColumn<Electrodomestico, Boolean>` con `CheckBoxTableCell` → selección múltiple independiente del foco de fila.
- Formulario lateral (panel derecho): New / Save / Update / Delete.
- Al pulsar una fila, los datos se cargan en el formulario para edición.

### 5.2 Filtros reactivos
- `ComboBox` **Tipo** + `ComboBox` **Marca** en la barra superior.
- Al cambiar Tipo → se actualiza la lista de Marcas disponibles.
- Cualquier cambio de filtro dispara `recargarDesdeInicio()` → consulta paginada.

### 5.3 Scroll infinito
- Listener en el `ScrollBar` vertical de `TableView`.
- Cuando `value >= 0.95` y existen más páginas → `cargarPagina(currentPage+1, false)` acumula filas.
- Tamaño de página: 25 registros.

### 5.4 Generación PDF — Etiquetas A6 en A4
```
┌──────────────────┬──────────────────┐
│   Etiqueta 0     │   Etiqueta 1     │  ← superior
│  (0, A6_H)       │  (A6_W, A6_H)    │
├──────────────────┼──────────────────┤
│   Etiqueta 2     │   Etiqueta 3     │  ← inferior
│  (0, 0)          │  (A6_W, 0)       │
└──────────────────┴──────────────────┘
         A4 = 595.28 × 841.89 pt
         A6 = 297.64 × 420.94 pt
```
- Líneas de corte punteadas en el centro.
- Contenido por etiqueta: Marca (bold 14pt), Modelo (11pt), tabla 2col con Precio/Clase/Dimensiones/Tipo, bloque de specs (7.5pt itálica).

---

## 6. Estructura del Proyecto

```
src/main/java/com/moreno/electrodomesticos/
├── MainLauncher.java                  ← entry point (evita restricción JavaFX/classpath)
├── ElectrodomesticosApp.java          ← javafx.application.Application
├── SpringBootConfig.java              ← @SpringBootApplication
├── config/
│   └── SpringFXMLLoader.java          ← DI bridge Spring↔FXML
├── controller/
│   └── MainController.java            ← lógica UI completa
├── model/
│   └── Electrodomestico.java          ← entidad JPA
├── repository/
│   └── ElectrodomesticoRepository.java
├── service/
│   ├── ElectrodomesticoService.java   ← CRUD + paginación
│   ├── DataInitializerService.java    ← datos demo al arrancar
│   └── PdfService.java                ← etiquetas A6 en A4
└── util/
    └── AlertHelper.java               ← diálogos JavaFX

src/main/resources/
├── application.properties
├── fxml/main.fxml
└── css/styles.css
```

---

## 7. Instrucciones de Compilación y Ejecución

```bash
# Compilar y empaquetar
mvn clean package -DskipTests

# Ejecutar con el plugin de JavaFX
mvn javafx:run

# O ejecutar el JAR directamente (requiere JRE 21 con JavaFX en el modulepath)
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/electrodomesticos-1.0.0.jar
```

> **Base de datos:** Se crea automáticamente en `./db/inventario.mv.db` al primer arranque.
