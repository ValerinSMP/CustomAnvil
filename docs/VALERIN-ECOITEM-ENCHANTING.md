# EcoItems y clasificación de encantamientos — 2.2.0-valerin.3

## Evidencia y alcance
Se reportó que sinOP cualquier EcoItem mostraba brevemente el resultado del
encantamiento y luego una X; conOP funcionaba. No se consultó el estado vivo de
permisos ni se reprodujo en Minecraft: pruebas en servidores prohibidas por el
propietario desde2026-10-10. No atribuir la causa productiva solo a permisos.

Fallo confirmado en la fuente: MaterialUtil.customType conserva ecoitems:<id>;
EnchantConflictManager pasaba esa clave a las reglas notAffectedGroups cuyos
grupos de armas/armaduras/herramientas tienen claves minecraft:<material>.
Una espada EcoItem queda fuera de un grupo que permite su material vanilla.
La prueba reproduce el rechazo con clave custom y compara la política corregida.
ca.bypass.fuse puede saltar ese filtro si se concede, pero defaultfalse no implica
queOP lo obtenga automáticamente. No se modifican permisos ni se recomienda
otorgar bypass global. Diferencia OP observada: atribución exacta pendiente.
EcoEnchants también puede recalcular el resultado con sus propios listeners;
este cambio no demuestra que sea la única causa del síntoma productivo.

## Corrección acotada
Solo para un EcoItem realmente reconocido por EcoItemDependencyUtil:
- Resolver elegibilidad del grupo de encantamientos usando item.type.key cuando
  no hay política explícita para su identificador custom.
- Si el identificador aparece en inclusión/exclusión propia o grupos anidados,
  conservar clasificación por ese identificador. Las exclusiones se inspeccionan
  mediante las entradas negadas, no por pertenencia a su conjunto complementario.
- Mantener claves custom para identidad, límites por objeto, reparación, recetas,
  AdditionalTestEnchantment e integraciones Eco. No convertir el ítem a vanilla.
- Objetos noEco/ItemsAdder y materiales vanilla mantienen el flujo anterior.
- No dar bypass, quitar incompatibilidades, límites ni permisos. El reset de
  RepairCost y capXP250 de2.2.0-valerin.2 permanecen en el candidato.

Archivos: AbstractMaterialGroup.enchantmentPolicyType (precedencia recursiva),
EnchantConflictManager.isConflicting (material base solo para Eco reconocido).
No se cambian configuración, dependencias, release21 ni api-version1.21.

## Verificación
161 pruebas automatizadas:154 anteriores+7 de políticaEco (rechazo previo,
armadura/herramientas/material no válido, inclusión y exclusión custom,
exclusión de material base, grupos anidados, claves desconocidas y sinbypass).
Son pruebas con MockBukkit en memoria, sin procesoMinecraft ni red: no prueban
registro realEcoItems, listeners de producción o cliente. Compilación y suites
exclusivamente en personal por ejecutor2CPU/4GiB/noSwap/JDK25.
No ensayosLeaf/Test ni instalaciones, recargas o reinicios. La validación de
servidor de2.2.0-valerin.2 NO se atribuye a este artefacto nuevo.

## Entrega
artifacts/CustomAnvil/2.2.0-valerin.3: JARoffline y online, SHA256SUMS,
manifest, build.log y resultadosJUnit. Ambos compilados; ninguno ensayado en
Minecraft. Copia en stagingHomeSV, sin instalar. GitHub pendiente de credenciales
existentes; no modificar/copiar secretos. No requiere migración deYAML/BD/mundos.
