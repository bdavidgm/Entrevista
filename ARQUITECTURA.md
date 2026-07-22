# Arquitectura del proyecto Entrevista

Documento de referencia para entender el proyecto **sin leer el código fuente**. Describe propósito, estructura, almacenamiento, parsers, scripts y flujos de la app.

---

## 1. ¿Qué es esta app?

**Entrevista** es una aplicación Android de estudio para entrevistas técnicas de **Kotlin / Android**. Ofrece:

- Un catálogo de **258 preguntas** organizadas por categoría.
- Búsqueda sobre el enunciado, la categoría y el **texto completo de la respuesta**.
- Vista de detalle con concepto, ejemplo de código, consejo de entrevista y (opcional) una explicación profunda del ejemplo en Kotlin.
- Guardar preguntas para más tarde y “continuar donde lo dejaste”.
- Copiar al portapapeles pregunta, respuesta o código (botón o pulsación larga).

Todo el contenido editorial está en español. No hay backend ni API remota: la app es 100 % offline.

---

## 2. Stack tecnológico

| Área | Elección |
|------|----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Build | Gradle (Kotlin DSL) + Version Catalog |
| AGP / Kotlin | 8.12.3 / 2.0.21 |
| minSdk / target / compile | 26 / 36 / 36 |
| Paginación de listas | AndroidX Paging 3 |
| Persistencia de usuario | SharedPreferences |
| Base de datos / red | **No** hay Room, DataStore en uso, ni llamadas HTTP |
| DI / Navigation Compose / ViewModel | **No** se usan |

Módulo único: `:app` (`applicationId` / `namespace`: `com.bdavidgm.entrevista`).

---

## 3. Vista de pájaro (cómo encajan las piezas)

```
MainActivity
  │
  ├─ Inicializa InterviewAnswersData (acceso a assets)
  ├─ UserPreferences (SharedPreferences)
  └─ Navegación en memoria: List | Saved | Detail
        │
        ├─ List  → filtra QuestionSummary + Paging
        ├─ Saved → IDs guardados → summaries
        └─ Detail → summary + answers/{id}.txt
                      └─ InterviewAnswerParser
                            └─ InterviewAnswerBody
                                  ├─ texto + AnswerCodeBlock
                                  └─ diálogo ExplicaciónKotlin
                                        (segmentos prose / code)
```

No hay NavHost ni ViewModels. La pantalla actual es un `sealed interface` en `MainActivity`; el estado de UI vive en `remember` / `mutableStateOf` y en los `StateFlow` de preferencias.

---

## 4. Estructura de paquetes

Paquete raíz: `com.bdavidgm.entrevista`

| Paquete / zona | Responsabilidad |
|----------------|-----------------|
| `MainActivity` | Entrada, tema, routing List/Saved/Detail, drawer |
| `data` | Modelos, catálogo de preguntas, carga de respuestas, prefs, paging, repositorio |
| `ui.screens` | Lista, detalle, guardados |
| `ui.navigation` | Drawer (Continuar / Guardado / Todas) |
| `ui.components` | Parser y render de respuestas, tarjetas de pregunta |
| `ui.theme` | Colores, tipografía, tema Material 3 |
| `ui.util` | Portapapeles + Toast |
| `scripts/` (raíz del repo) | Generadores Python **offline** de contenido (no forman parte del APK) |

---

## 5. Capa de datos: qué se guarda y dónde

Hay **dos mundos** de información, bien separados:

### 5.1 Contenido editorial (empaquetado en el APK)

| Qué | Dónde | Cómo se identifica |
|-----|--------|--------------------|
| Metadatos de pregunta (id, categoría, enunciado) | Código Kotlin: `InterviewSummariesData` | Lista en memoria, IDs **1…258** |
| Cuerpo de la respuesta | Assets: `app/src/main/assets/answers/{id}.txt` | Un archivo por ID (`1.txt` … `258.txt`) |

- Las preguntas **no** viven en assets: solo las respuestas.
- El repositorio (`InterviewRepository`) es la fachada: summaries, `getAnswer(id)`, prev/next, búsqueda y filtrado.
- Constante clave: `MAX_QUESTION_ID = 258`, `PAGE_SIZE = 30`.

**Carga de una respuesta:** se abre `assets/answers/{id}.txt` y se lee el texto completo. No hay JSON ni base de datos de contenido.

**Búsqueda:** primero filtra por enunciado/categoría en summaries; además escanea los textos de respuesta en assets (en un dispatcher de IO/Default) y une los IDs coincidentes.

### 5.2 Estado del usuario (dispositivo)

`UserPreferences` → SharedPreferences llamado `user_preferences`:

| Clave | Significado |
|-------|-------------|
| `last_question_id` | Última pregunta abierta desde la lista / navegación prev-next (“Continuar…”) |
| `saved_question_ids` | Conjunto de IDs guardados (“Guardar para ver después”) |

Se expone con `StateFlow`. Abrir una pregunta desde **Guardados** no actualiza la posición de “continuar”.

---

## 6. Formato de las respuestas (el “contrato” del contenido)

Cada archivo `answers/{id}.txt` es texto plano con secciones marcadas por **líneas cabecera**. El parser solo reconoce de forma especial tres marcadores; el resto del texto (p. ej. `Concepto:`) se queda en el bloque de prosa anterior al código.

### 6.1 Marcadores que entiende la UI

| Marcador (línea completa) | Rol |
|---------------------------|-----|
| `Ejemplo:` | Empieza el bloque de código |
| `ExplicaciónKotlin:` | Empieza la explicación detallada del ejemplo (solo diálogo; no se muestra bajo el código en el detalle) |
| `Consejo:` o `Consejo para entrevista:` | Empieza el consejo final (se muestra debajo del código) |

### 6.2 Forma típica de un archivo

```
Concepto:
…texto conceptual…

Ejemplo:
…snippet de código…

ExplicaciónKotlin:
…prosa con fragmentos entre backticks…

Consejo:
…tips para la entrevista…
```

Algunas respuestas antiguas/largas también incluyen secciones como `Explicación detallada:` y `Cuándo usarlo:` **antes** del ejemplo; el parser no las trata aparte: van dentro de la prosa previa al código.

### 6.3 Resultado del parseo

El parser produce conceptualmente:

1. **Prosa antes del código** — concepto (y secciones opcionales previas).
2. **Código** — todo lo que hay entre `Ejemplo:` y el siguiente marcador de explicación/consejo (o fin de archivo).
3. **Explicación Kotlin** — cuerpo de `ExplicaciónKotlin:` (para el diálogo).
4. **Prosa después** — desde la línea `Consejo…` hasta el final.

Si no hay `Ejemplo:` (o el código queda vacío), toda la respuesta se muestra como texto plano, sin bloque de código.

---

## 7. Cómo se identifica y muestra el código

Hay **dos niveles** de identificación de código.

### 7.1 Nivel 1 — el ejemplo de la respuesta

Algoritmo (resumido) de `InterviewAnswerParser.parse`:

1. Normalizar saltos de línea.
2. Buscar la línea `Ejemplo:`.
3. El código llega hasta la primera de: `ExplicaciónKotlin:`, `Consejo…`, o fin de archivo.
4. Recortar líneas en blanco al inicio/final del fragmento.
5. Ese string se pinta en el componente **`AnswerCodeBlock`**: superficie con borde, cabecera **“Kotlin”**, botón copiar e ícono de info (abre el diálogo de explicación).

### 7.2 Nivel 2 — citas de código dentro de `ExplicaciónKotlin`

En las explicaciones, los fragmentos van entre **backticks** (`` `...` ``). Otro algoritmo (`parseExplanationSegments`) hace:

1. Partir el texto por el carácter backtick.
2. Los segmentos “impares” son candidatos a código.
3. Una heurística (`isCodeLineSnippet`) decide:
   - **Línea / expresión de código** → se renderiza con el **mismo** `AnswerCodeBlock` (sin ícono de info, porque ya estás en el diálogo).
   - **Token corto** (identificador, `apply`, `Fragment`, `companion object`, `:`, etc.) → se deja en el párrafo con tipografía monoespaciada.

**Criterios orientativos de “parece línea de código”:**

- Contiene caracteres de sintaxis como `(){}=<>[]`, o
- Tiene un `.` y longitud ≥ 5 (llamadas/cualificados), o
- Contiene `::`, o
- Es una frase larga con puntuación de código / palabra inicial de declaración Kotlin (`fun`, `class`, `val`, `private`, `override`, …).

Así el diálogo mezcla párrafos legibles con bloques visuales iguales al del ejemplo principal.

---

## 8. Pantallas y navegación

| Pantalla | Qué hace |
|----------|----------|
| **Lista** | Chips de categoría, búsqueda, lista paginada de tarjetas |
| **Detalle** | Enunciado + respuesta parseada; Anterior / Guardar / Siguiente |
| **Guardados** | Solo las preguntas marcadas |
| **Drawer** | Continuar (última pregunta), Guardado, Todas las preguntas |

El detalle se muestra a pantalla completa (fuera del shell del drawer). Al cerrar o volver, se restaura Lista o Guardados según de dónde se abrió.

---

## 9. Scripts de Python: para qué existen

En `scripts/` hay generadores **offline** usados al **crear o regenerar** archivos de respuesta en lote. **No se ejecutan en la app** ni se empaquetan en el APK.

| Script | Intención |
|--------|-----------|
| `generate_answers_56_110.py` | Generar respuestas españolas en un rango de IDs (plantilla con Concepto / Explicación detallada / Cuándo usarlo / Ejemplo / Consejo para entrevista) |
| `generate_answers_223_236.py` | Igual para otro rango de IDs |

Escriben (o escribían) directamente en `app/src/main/assets/answers/{id}.txt`.

### Importante sobre el estado actual

- Estos scripts son **herramientas de contenido legacy**. La plantilla que generan **no incluye** `ExplicaciónKotlin:` (esa sección se añadió después en los assets).
- Pueden estar incompletos o desfasados respecto al mapa real ID ↔ tema (el catálogo vivo está en `InterviewSummariesData` + los `.txt` actuales).
- **Fuente de verdad del producto:** assets actuales + summaries en Kotlin. Los scripts sirven para entender el historial de generación o para ediciones masivas futuras, no para “cómo corre la app”.

---

## 10. Categorías de preguntas

Las categorías son un enum con nombre visible en español. Ejemplos de grupos: Kotlin básico/avanzado/OOP, Compose, Jetpack, corrutinas, ciclo de vida Android, APIs Android, arquitectura, rendimiento, seguridad, testing, build/Gradle, UI/UX, ruta de aprendizaje Android, temas técnicos (ADB, etc.), generales.

El filtrado por categoría es local sobre la lista de summaries.

---

## 11. Algoritmos relevantes (resumen operativo)

| Algoritmo | Dónde conceptualmente | Entrada → salida |
|-----------|----------------------|------------------|
| Parseo de respuesta | Parser de respuestas | Texto del `.txt` → prosa / código / explicación / consejo |
| Segmentación de explicación | Mismo módulo UI | Texto de `ExplicaciónKotlin` → lista de prosa anotada + bloques de código |
| Clasificación backtick | Heurística `isCodeLineSnippet` | Snippet → bloque visual vs monoespaciado inline |
| Filtrado + búsqueda | Repositorio | Query + categoría → lista de `QuestionSummary` |
| Paginación | `SummaryPagingSource` | Lista filtrada en memoria → páginas de tamaño 30 |
| Prev / next | Repositorio | `id ± 1` acotado a 1…258 |

---

## 12. Pruebas

| Qué | Rol |
|-----|-----|
| `InterviewAnswerParserTest` | Cobertura real del parseo de marcadores, variantes de Consejo, y clasificación de backticks |
| Tests de plantilla (unit / instrumented) | Placeholders del proyecto Android |

No hay suite grande de UI Compose tests.

---

## 13. Funcionalidades notables (comportamiento de producto)

- Cabecera **Kotlin** en el bloque de código del ejemplo.
- Ícono de información → diálogo “Explicación del ejemplo (Kotlin)” con scroll (altura máxima ~420 dp).
- En el diálogo, las líneas citadas con backticks “fuertes” reutilizan el mismo componente visual de código.
- Copia al portapapeles con feedback Toast.
- Lista con chips: volver a tocar la categoría seleccionada la desactiva.
- Tema Material 3 claro/oscuro (sin dynamic color por defecto en la configuración actual del tema).

---

## 14. Mapa mental de archivos clave

| Ruta | Por qué importa |
|------|-----------------|
| `app/src/main/java/.../MainActivity.kt` | Orquestación y navegación |
| `.../data/InterviewRepository.kt` | API de datos de la app |
| `.../data/InterviewSummariesData.kt` | Catálogo de las 258 preguntas |
| `.../data/InterviewAnswersData.kt` | Lectura de assets |
| `.../data/UserPreferences.kt` | Continuar + guardados |
| `.../ui/components/InterviewAnswerContent.kt` | Parser + UI de respuesta/código/diálogo |
| `app/src/main/assets/answers/*.txt` | Contenido de las respuestas |
| `scripts/*.py` | Generación masiva histórica de respuestas |
| `app/src/test/.../InterviewAnswerParserTest.kt` | Contrato del parser |
| `gradle/libs.versions.toml` | Versiones de dependencias |

---

## 15. Cómo extender el contenido (guía práctica)

Para **añadir una pregunta nueva** sin romper la arquitectura:

1. Asignar el siguiente ID (`259` si el máximo sigue siendo 258) y actualizar `MAX_QUESTION_ID`.
2. Añadir el `QuestionSummary` correspondiente en `InterviewSummariesData`.
3. Crear `app/src/main/assets/answers/{id}.txt` respetando el contrato de secciones (`Concepto` / `Ejemplo` / `ExplicaciónKotlin` / `Consejo`).
4. En la explicación Kotlin, envolver en backticks los fragmentos que deban verse como código; usar backticks también para tokens cortos que deban ir en monoespaciado.

Para **regenerar muchas respuestas**, se puede escribir o adaptar un script en `scripts/` que escriba los `.txt`, teniendo en cuenta el marcador `ExplicaciónKotlin:` que la UI actual espera.

---

## 16. Decisiones de diseño (por qué está así)

- **Assets de texto en lugar de BD:** el contenido es estático, versionado con el APK y fácil de editar/diffear.
- **Summaries en código:** evita un segundo formato; la lista es pequeña (258) y cabe en memoria.
- **Parser por marcadores de línea:** permite enriquecer el contenido sin cambiar el modelo de datos ni tocar Kotlin por cada pregunta.
- **Sin ViewModel/Navigation Compose:** app pequeña de tres pantallas; el estado cabe en `MainActivity` + preferencias.
- **Scripts Python fuera del APK:** generación de contenido editorial, no lógica de runtime.

---

*Documento generado para el repositorio Entrevista. Si el número de preguntas o el formato de assets cambia, actualizar las secciones 5, 6 y 15.*
