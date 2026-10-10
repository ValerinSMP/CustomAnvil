# CustomAnvil Valerin — Leaf 26.2

Entrega 2026-10-09: 2.2.0-valerin.1, variante nativa Paper/Leaf.
No instalada ni activada en Custom, Sync o Test.

## Historial y procedencia

- Se leyeron las instrucciones de personal y del proyecto antes de modificar.
- Rama inicial valerin, HEAD 1c3ee487c426e0b906c45b5743359c964daa609e.
  Único archivo ajeno: DEVELOPMENT_HANDOFF.md sin seguimiento.
- Se recuperó el historial completo mediante git fetch --unshallow github y se
  consultaron directamente las ramas de alexcrea/CustomAnvil.
- Integración --ff-only: 31 commits hasta upstream v2.x.x,
  40a7f34aab5e6f692e17bac87192a6e82e4d77b5.
  Es la punta de desarrollo 2.2.0 observada, no el tag estable 2.1.5.
- No hay commits exclusivos de valerin frente a upstream en el punto inicial.
  valerin-legacy-1.16.3 (45fe037a925dbe3ae21c3bf1af4bd1d91aa3376b)
  también es un ancestro upstream. No se encontraron ampliaciones de código
  exclusivas de Valerin en esas ramas. Las funciones pedidas son upstream;
  las configuraciones y esta adaptación constituyen el ámbito Valerin.
- Ambos JARs productivos en disco, CustomAnvil-1.16.3.jar, tienen SHA-256
  4571184ae4e6601cb8769e152ef99f8ed359202fbf826bb9be60cd4ff385e37c.
  Su plugin.yml declara 1.16.3 y Kotlin 2.1.0; el parser inspeccionado mediante
  javap coincide con el legacy: getBukkitVersion, split por guion,
  readVersionFromString e Integer.parseInt sin protección.
- La fuente inicial de valerin es 2.1.2 y no corresponde directamente a ese
  binario. El JAR no contiene un identificador de commit propio. La evidencia
  es consistente con la línea legacy, pero no demuestra reproducción binaria
  exacta ni permite atribuirle un commit solo por el nombre.
- En Sync, latest.log confirma la excepción en UpdateUtils.readVersionFromString
  durante DependencyManager / GuiTesterSelector. No se afirmó lifecycle actual
  de ambos nodos a partir de un archivo en disco.
- Handoff original intacto y sin seguimiento, SHA-256:
  c063a1636ec300841c98a4cdc34698bdf1e183c3b28d7a61135437325dacb3e4.

## Funciones y cambios

| Función | Procedencia | Conservación / validación |
|---|---|---|
| Fusión, límites, conflictos y restricciones | Upstream | Suite y preparación real del yunque en Leaf |
| Reparación por objeto/material y costes | Upstream | Suite, API y reparación por diamante en Leaf |
| Renombrado, códigos, hexadecimal y MiniMessage | Upstream | Suite, permisos/escape; rename y color nativos |
| Lore por papel/libro, costes y permisos | Upstream | Nuevas pruebas de libro/permisos; helpers reales en Leaf |
| EcoEnchants moderno/legacy | Upstream | 81 wrappers modernos, lookup y llamadas de restricciones con 2026.27 |
| EcoItems, identidad y materiales | Upstream, adaptado aquí | Siete ítems 2026.27 y namespace ajeno rechazado |
| API, callbacks, GUI, recetas y bypass | Upstream | Código preservado, suite de API y callbacks de preparación |

Cambios propios de esta entrega:

1. Versión de Minecraft desde la API nativa Server.getMinecraftVersion en
   Paper/Leaf. Acceso reflectivo para conservar la API Spigot mínima upstream.
   Fallback a versión Bukkit numérica o marcador (MC: ...) en servidores antiguos.
   Metadatos desconocidos producen un error claro, no una versión 0 silenciosa.
   El parser general de versiones de configuraciones no cambia.
2. EcoItems: lookup por ID completo o interno, verificando siempre que item.id
   corresponda exactamente al NamespacedKey pedido. El ensayo previo detectó
   el fallo upstream con 2026.27; el candidato final pasa la misma prueba.
3. Migración sweeping/sweeping_edge: conserva límites y costes explícitos,
   respeta valores modernos existentes y evita que defaults implícitos
   sustituyan personalizaciones o aliases antiguos. Tres regresiones pasan.
4. Build nativo limitado y nueve pruebas nuevas de versión, migración, colores
   y lore. No se cambian mecánicas por la adaptación de toolchain.

Se mantienen LICENSE, CREDITS, estructura Gradle y contratos upstream.
No se sustituyó la fuente por un JAR oficial ni se modificaron proveedores Eco.

## Compilación

Desde personal:

    cd /root/development/projects/forks/upstream/CustomAnvil
    bash tools/build-personal-native.sh

El script llama exclusivamente a /root/development/tools/run-personal-build.sh,
con JDK 25, un worker, sin daemon persistente ni paralelo, cgroup 2 CPU / 4 GiB /
cero swap, prioridad baja y timeout del ejecutor.

Upstream ya declara toolchain 25. Se conservan Java/Kotlin 21, options.release=21
para Java y api-version: 1.21. Se verificaron classfiles 65 para el plugin y
ninguna clase base del offline JAR por encima de 65. No se probó runtime Java 21.

La variante usa -Psubprojects.reobfnms=: incluye nms-common y nms-paper, sin las
implementaciones Spigot obfuscadas antiguas. Sus fuentes y la selección
predeterminada siguen presentes; no se validó aquí la build completa Spigot.

-Ppaperweight.experimental.genSources=false evita fuentes opcionales del IDE.
La opción se verificó en Paperweight beta.23. El servidor mapeado necesario para
compilar sigue generado/resuelto. Los intentos con fuentes IDE excedieron el
cgroup o heap del decompilador; se detuvo solo la build propia.
No se instalaron JDK adicionales ni se elevaron límites.

Kotlin compila dentro de Gradle, sin daemon separado. Se conserva la advertencia
upstream de Shadow sobre KotlinModuleMetadataTransformer / DuplicatesStrategy;
no se observó fallo de carga en los ensayos.

## Pruebas y límites

- 150 tests JUnit/MockBukkit: 0 fallos, 0 errores, 0 omitidos.
  Incluyen API/config/fusión upstream y nuevas regresiones.
  LoreEditTests upstream estaba comentado; se añadieron pruebas ejecutables.
- Leaf 26.2 build 124, JDK 25, offline candidato final: ensayo nativo y ensayo
  con eco 2026.28, libreforge 2026.27, EcoEnchants 2026.27 y EcoItems 2026.27.
- Ambos arrancaron y terminaron con exit 0 y probe PASS. Metadata real:
  Minecraft 26.2; Bukkit 26.2.build.124-alpha. PaperPacketManager sin ProtocolLib,
  GUI nativa y AnvilView reales. Paquetes de habilidades, Sharpness IV a V,
  PrepareAnvilEvent, coste nativo, callbacks CATreatAnvilResultEvent,
  reparación por material, rechazo de Protection sobre espada, rename/color,
  conservación del input y helpers append/remove de lore por libro.
- Fixtures sintéticas desde defaults públicos del JAR legacy, nunca desde
  configuraciones privadas. Verificados límite 250, rename 1, color false,
  clave desconocida y migración 1.15.6 a 1.18.0.
- Servidores temporales como nobody, PrivateNetwork, ProtectHome/ProtectSystem,
  cgroup 2 CPU / 4 GiB / cero swap. Unidades inactivas al terminar; bot healthy.
- Logs con fallos esperables de consultas de actualización/auth por aislamiento
  de red. No se cargaron EcoSkills/EcoJobs/EcoPets: sus enchants no forman parte
  de los 81 registrados en la fixture.
- Jugador sintético y paquetes capturados sin cliente. Preparación del evento y
  helpers no prueban aceptación por cliente, click/extracción completa, consumo
  de XP/material en juego, transferencia, Bedrock, UI/dialog o configuraciones
  privadas. No hay gameplay humano probado.
- Eco 2026.37 es dependencia de compilación upstream; el runtime probado usa
  las versiones instaladas 2026.27/2026.28, no todas las versiones posibles.

Los scripts tools/leaf-smoke aceptan bootstrap cacheado de Leaf y el inventario
sanitizado de JARs de laboratorio. No descargan/copían mundos, bases reales ni
configuraciones privadas:

    /root/development/tools/run-personal-build.sh "$REPO" python3 tools/leaf-smoke/compile-probe.py "$LEAF_BOOTSTRAP" "$LAB_BASELINE"
    python3 tools/leaf-smoke/run-isolated.py "$LEAF_BOOTSTRAP" "$LAB_BASELINE" "$ARTIFACTS"

La ejecución aislada es una acción explícita de pruebas, fuera de la build
normal. Necesita root en personal para crear la unidad aislada. No ejecutarla
sobre un volumen productivo.

## Configuraciones y migración futura

No se editó ni copió ninguna configuración productiva. Custom y Sync tienen
config.yml distintos; los otros cuatro YAML observados coinciden.
Conservar cada conjunto por nodo; no sincronizarlos ni sustituirlos por defaults.

En un despliegue separado, respaldar los cinco YAML y el JAR activo. El migrador
upstream añade campos faltantes y normaliza versiones/aliases; esta entrega
corrige la conservación de sweeping. Al guardar YAML puede cambiar el formato
y perder comentarios; comparar valores. No editar manualmente configVersion ni
lowMinecraftVersion para evitar el migrador.

No hace falta migrar BD ni ítems de mundos por este cambio. Las funciones nuevas
de diálogo/coste monetario siguen siendo opcionales y no se activan por esta
entrega. Configuraciones privadas, instalación y lifecycle requieren una tarea
posterior autorizada.

## Artefactos

Personal: /root/development/artifacts/CustomAnvil/2.2.0-valerin.1/.

- CustomAnvil-2.2.0-valerin.1-offline.jar, variante probada:
  a487c4aa8c30dae146c09a876c7de9214b165876491c4c4d2f1d882671ca0b05
- CustomAnvil-2.2.0-valerin.1.jar, con librerías externas, compilada pero no
  ensayada como artefacto final:
  49f0b5605fa35968498730e4547a2405640149c2964410d61952b3f222c05d99

SHA256SUMS, manifest.json, build.log, unit-test-results y smoke-results.json
conservan evidencias, incluido el fallo previo de EcoItems y su corrección.

## Publicación Git

Código validado en commit local 8ec1417. El push HTTPS desde personal falló
por falta de autenticación; HomeSV tampoco tiene sesión gh configurada.
Se conserva la entrega local; la rama remota valerin sigue en 1c3ee48.
No se copiarán ni cambiarán credenciales para resolverlo.
