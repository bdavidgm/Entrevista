#!/usr/bin/env python3
"""Generate exhaustive Spanish interview answers for IDs 56-110."""

import os

BASE = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app/src/main/assets/answers",
)


def fmt(concepto, detalle, cuando, ejemplo, consejo):
    return (
        f"Concepto:\n{concepto}\n\n"
        f"Explicación detallada:\n{detalle}\n\n"
        f"Cuándo usarlo:\n{cuando}\n\n"
        f"Ejemplo:\n{ejemplo}\n\n"
        f"Consejo para entrevista:\n{consejo}\n"
    )


ANSWERS = {
    56: fmt(
        "La integración de Jetpack Compose en una aplicación basada en Views permite adoptar UI declarativa de forma incremental, sin reescribir toda la app. Se apoya en ComposeView para incrustar Composables en XML y en AndroidView para insertar Views clásicas dentro del árbol Compose. Es el camino oficial recomendado por Google para migraciones graduales.",
        """En proyectos Android con años de XML, Fragments y ViewBinding, Compose no exige un reemplazo total de un día para otro. Habilitas Compose en el módulo (buildFeatures.compose = true), añades un ComposeView en el layout XML y desde Kotlin llamas setContent { } para definir la UI declarativa de esa zona. ComposeView actúa como host del Composition: gestiona el árbol Composable, el tema MaterialTheme y las recomposiciones cuando cambia el estado observado.

En la dirección opuesta, el Composable AndroidView(factory, update) crea una View tradicional dentro de Compose. El factory recibe Context y devuelve la instancia; el bloque update opcional sincroniza estado de Compose hacia la View en cada recomposición relevante. Es el patrón estándar para MapView, WebView, AdView o cualquier SDK que aún no tenga binding Compose.

Aspectos que debes dominar en entrevista: alineación de temas (MaterialTheme en Compose coherente con Theme.AppCompat en XML, o androidx.compose.material3.MdcTheme), estrategia de composición (ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed evita fugas al destruir la vista), y una única fuente de verdad para el estado (ViewModel compartido entre Fragment XML y Composable vía StateFlow o parámetros).

En Fragment puedes usar ComposeView en onCreateView o el DSL compose { } de androidx.fragment:fragment-compose. En Activity, setContent en ComponentActivity reemplaza setContentView por completo en pantallas ya migradas; en pantallas híbridas, mantén setContentView(R.layout...) y solo el contenedor es ComposeView.

Riesgos: anidar muchos AndroidView degrada rendimiento (cada uno es una isla fuera del árbol de recomposición ligero), duplicar estado entre View y Compose provoca bugs sutiles, y olvidar el tema hace que los colores no coincidan con el resto de la app. La migración por feature (una pantalla, un flujo) reduce riesgo frente al big bang.""",
        "Úsalo cuando el equipo debe seguir entregando features en Views pero quiere Compose en pantallas nuevas o complejas (formularios, listas animadas). También cuando una librería solo ofrece View. Evita mezclar decenas de AndroidView en una misma pantalla Compose si existe alternativa nativa.",
        """// res/layout/activity_main.xml
// <androidx.compose.ui.platform.ComposeView android:id="@+id/compose_host" ... />

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<ComposeView>(R.id.compose_host).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                MaterialTheme { ProductListScreen() }
            }
        }
    }
}

@Composable
fun EmbeddedWeb(url: String) {
    AndroidView(
        factory = { context -> WebView(context) },
        update = { webView -> webView.loadUrl(url) }
    )
}""",
        "Menciona migración incremental, ComposeView, AndroidView, ViewCompositionStrategy y tema compartido. Si preguntan por estado compartido: ViewModel + StateFlow. No digas que hay que reescribir toda la app; Google documenta explícitamente interoperabilidad bidireccional.",
    ),
    57: fmt(
        "rememberSaveable es un Composable que conserva estado a través de recomposiciones y también de cambios de configuración (rotación) y muerte de proceso, usando el mecanismo de Bundle de Android. Extiende remember con persistencia automática mediante Saver o tipos ya soportados.",
        """remember solo sobrevive mientras el Composable permanece en el árbol de composición; si la Activity se recrea por rotación, el estado en remember se pierde a menos que lo restaures manualmente. rememberSaveable delega en rememberSaveableStateSaver y en SavedStateRegistry: serializa el valor en un Bundle asociado al mismo mecanismo que onSaveInstanceState.

Los tipos soportados por defecto incluyen primitivos, String, Parcelable, Serializable (con matices), y listas/mapas de tipos compatibles. Para tipos personalizados defines un Saver con save/restore o usas mapSaver/listSaver. Es el equivalente Compose de guardar estado de UI en el Bundle sin escribir código boilerplate en la Activity.

Diferencia con ViewModel: rememberSaveable es para estado de UI efímero de pantalla (texto de búsqueda, scroll, expansión de acordeón); ViewModel es para estado de negocio que debe sobrevivir rotación con semántica más rica y tests. Pueden coexistir: el ViewModel expone datos de dominio y rememberSaveable guarda posición de lista o tab seleccionado.

Con process death (sistema mata la app en background), rememberSaveable puede restaurar si el usuario vuelve por recents; no sustituye persistencia en disco (Room, DataStore). Para listas grandes no serialices todo en Bundle — guarda solo IDs o índices.

En formularios multi-paso, combinar rememberSaveable con navegación evita perder borradores en rotación sin inflar el ViewModel.""",
        "Úsalo para estado de UI local que debe sobrevivir rotación: texto de TextField, índice de pager, filtros temporales. No lo uses para datos sensibles, listas enormes ni fuente única de verdad de negocio (ViewModel + repositorio).",
        """@Composable
fun SearchBar() {
    var query by rememberSaveable { mutableStateOf("") }
    TextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Buscar") }
    )
}

// Tipo personalizado
@Parcelize
data class Filter(val category: String) : Parcelable

@Composable
fun FilterChip() {
    var filter by rememberSaveable { mutableStateOf(Filter("all")) }
}""",
        "Compara con remember y ViewModel; menciona Saver para tipos custom y límite de tamaño del Bundle. Pregunta trampa: '¿Guarda en disco?' — No, solo Bundle/estado restaurable; persistencia real es DataStore/Room.",
    ),
    58: fmt(
        "Scaffold es un Composable de Material Design que proporciona la estructura esqueleto de una pantalla: slots para topBar, bottomBar, floatingActionButton, drawer y el contenido principal con padding y snackbarHost integrado. Unifica el layout típico de app Android en un solo componente declarativo.",
        """Antes de Compose, CoordinatorLayout + AppBarLayout + FAB + BottomNavigation requerían XML anidado y comportamiento manual del inset system. Scaffold de material3 (o material) encapsula las convenciones: el contenido recibe PaddingValues del innerPadding para respetar barras y notch; snackbarHost gestiona cola de Snackbar con coroutineScope.

Cada slot es opcional y es un @Composable lambda: topBar = { TopAppBar(...) }, bottomBar = { NavigationBar { ... } }, floatingActionButton = { ExtendedFloatingActionButton(...) }. El parámetro containerColor y contentColor alinean con el tema. Para Navigation Drawer, slot drawerContent envuelve ModalNavigationDrawer o usa Scaffold junto con drawer state externo.

Scaffold no sustituye la lógica de navegación ni ViewModel; solo organiza UI. Debes pasar el padding al contenido hijo (Modifier.padding(innerPadding)) o muchos diseños quedarán debajo de la status bar incorrectamente. Con edge-to-edge (Android 15+), combina Scaffold con WindowInsets y consumeInsets en hijos si necesitas control fino.

En entrevistas comparan Scaffold con Column + TopAppBar manual: Scaffold aplica semantics, animaciones de FAB al scroll en algunos patrones, y consistencia Material. Variantes: BottomSheetScaffold, permanent drawer en tablets.""",
        "En casi toda pantalla principal Material3 con barra superior, navegación inferior o FAB. Evita forzar Scaffold en diálogos simples o pantallas fullscreen inmersivas (video, cámara) donde el esqueleto sobra.",
        """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Inicio") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigate("create") }) {
                Icon(Icons.Default.Add, contentDescription = "Crear")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            items(20) { Text("Item $it", Modifier.padding(16.dp)) }
        }
    }
}""",
        "Menciona innerPadding obligatorio, slots composables y snackbarHost. Si preguntan edge-to-edge: WindowInsets + Scaffold. No confundas Scaffold con Navigation host (NavHost va dentro del contenido).",
    ),
    59: fmt(
        "Navigation Compose es la integración del Navigation Component con Jetpack Compose: define un grafo de rutas (@Composable destinations) con NavHost, NavController y argumentos tipados, gestionando back stack y deep links de forma declarativa sin Fragments.",
        """NavController en Compose se obtiene con rememberNavController() y se asocia al NavHost(startDestination, route graph). Cada destino es un composable(route) { backStackEntry -> ... }. Los argumentos se declaran en la ruta ("detail/{id}") y se leen con backStackEntry.arguments o navArgument + NavType.

Ventajas frente a XML navigation: rutas en Kotlin, argumentos con tipo seguro, animaciones de transición con enterTransition/exitTransition, y nested navigation (graphs anidados para flujos login vs main). Integración con ViewModel: hiltViewModel() scoped al backStackEntry o navGraph permite ViewModels por destino.

Deep links: navDeepLink { uriPattern = "https://app.example/item/{id}" }. Para bottom navigation con varias tabs, patrón recomendado: un NavHost por tab o NavigationBar con restoreState y saveState en navigate() para no perder estado al cambiar tab.

Single-Activity architecture encaja naturalmente: una Activity con setContent { AppNav() }. Para pasar resultados entre pantallas, Navigation 2.8+ ofrece type-safe routes con Kotlin Serialization o callbacks con popBackStack + savedStateHandle.

Errores comunes: múltiples NavController, olvidar popUpTo al login tras auth, y navegar desde LaunchedEffect sin comprobar lifecycle. Testing: prueba rutas con createComposeRule y NavHostController.""",
        "Apps Compose-first con una Activity, flujos multi-pantalla, deep links y argumentos. Si la app es 100% Fragments legacy sin migrar, Navigation Component XML puede seguir; migra NavHost cuando migres pantallas.",
        """@Composable
fun AppNav() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onOpen = { id ->
                navController.navigate("detail/$id")
            })
        }
        composable(
            route = "detail/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("itemId") ?: return@composable
            DetailScreen(itemId = id, onBack = { navController.popBackStack() })
        }
    }
}""",
        "Domina NavHost, rutas con argumentos, popUpTo/inclusive para auth, y ViewModel scoped al back stack entry. Pregunta típica: bottom nav sin recrear — launchSingleTop + restoreState + saveState.",
    ),
    60: fmt(
        "El patrón ViewModel en Compose separa la lógica de presentación y el estado observable de la UI declarativa: el Composable solo lee estado y envía eventos; el ViewModel sobrevive cambios de configuración y expone StateFlow o uiState para recomposición vía collectAsStateWithLifecycle.",
        """En Compose no hay "vínculo mágico": conectas ViewModel con viewModel() o hiltViewModel() dentro del Composable (o lo recibes desde Activity/NavBackStackEntry). El ViewModel expone un único UiState inmutable (data class) o StateFlow; la UI hace val state by viewModel.uiState.collectAsStateWithLifecycle().

Eventos de una sola vez (snackbar, navegación) no deben ser State permanente — usa Channel, SharedFlow con replay 0, o patrones de evento consumido para evitar repetición en rotación. Entrada del usuario: funciones onEvent(UserAction) en el ViewModel que actualizan estado con copy().

hiltViewModel() requiere @HiltViewModel y navegación o Activity @AndroidEntryPoint. El scope del ViewModel es el NavBackStackEntry o Activity según dónde se solicite; dos pantallas del mismo graph pueden compartir ViewModel con navGraphViewModels.

Testing: ViewModel con coroutines test dispatcher sin Compose; UI con ComposeTestRule mockeando ViewModel fake. No pongas Context ni Composable references en ViewModel (memory leak y untestable).

State hoisting vs ViewModel: hoisting para componentes reutilizables sin lógica; ViewModel cuando hay coroutines, repository, o supervivencia de configuración. derivedStateOf en UI para derivar filtros del state expuesto.""",
        "Siempre que la pantalla tenga lógica, IO, o estado que sobreviva rotación. Componentes puros sin red ni persistencia pueden usar solo remember y callbacks hoisted.",
        """@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val repo: ProductRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    fun load() = viewModelScope.launch {
        _uiState.update { it.copy(loading = true) }
        runCatching { repo.getProducts() }
            .onSuccess { list -> _uiState.update { it.copy(loading = false, items = list) } }
            .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
    }
}

@Composable
fun ProductListScreen(vm: ProductListViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    when {
        state.loading -> CircularProgressIndicator()
        else -> LazyColumn { items(state.items) { Text(it.name) } }
    }
}""",
        "Enfatiza UiState inmutable, eventos one-shot, hiltViewModel scope, y que ViewModel no conoce Composable. Comparación con MVP/MVI encaja bien como seguimiento.",
    ),
}

# Due to length limits, continue in second write with remaining IDs
print(f"Partial: {len(ANSWERS)} answers defined")
