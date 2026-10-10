# Penalización acumulada Valerin — 2.2.0-valerin.2

## Evidencia y contrato
La revisión autorizada del binario productivo 1.16.3 confirmó dos opciones y
sanitizeResultRepairCost ausentes tanto en legacy45fe037 como en entrega8ec1417.
SHA256 del binario: 4571184ae4e6601cb8769e152ef99f8ed359202fbf826bb9be60cd4ff385e37c.
Evidencia fuera del repositorio en personal:
/root/development/reviews/CustomAnvil/legacy-1.16.3/REVIEW.md, decompiled/ y bytecode/.
No atribuir equivalencia del binario al historial o a su nombre.

- reset_result_repair_cost: false por defecto; true en configuraciones existentes
  de Custom/Sync observadas. No modificar esos archivos para el porte.
- reset_result_repair_cost_value: 0 por defecto; rango0..1000, inválidos vuelven0.
- Actúa exclusivamente sobre Repairable.repairCost del resultado. No altera el
  coste de XP de la operación en curso ni las penalizaciones PDC exclusivas.
- Calcular penalización y ejecutar callbacks antes del reset: un objeto antiguo
  puede aportar penalización a la operación actual, aunque salga con valor0.
- No modifica entradas, lore, encantamientos, durabilidad, identidades Eco o PDC.
- Límites de XP, permisos, restricciones e integraciones permanecen vigentes;
  no existe una promesa de acciones gratuitas o de ausencia de restricciones.

## Implementación
ConfigOptions lee claves con el lock actual de ConfigHolder.DEFAULT.
AnvilXpUtil.sanitizeResultRepairCost aplica helper null-safe.
AnvilMergeLogic lo invoca sobre el resultado final después de callbacks/cálculo
para renombrado, fusión, recetas y reparación; para lore, después de validar que
hay un cambio útil. Se ejecuta tanto durante preparación como durante el recálculo
en InventoryClickEvent. Rutas bypass siguen bajo control de sus integraciones.
AnvilResultListener también sanitiza salidas regeneradas de recetas.
El porte cubre la copia entregada de UNIT_REPAIR: el binario antiguo solo
sanitizaba el preview y reconstruía sin reset al extraer.

La prueba de extracción encontró un fallo adicional heredado de2.x:
onCustomCraft en supervivencia consumía ingredientes/coste y luego retornaba
sin entregar el resultado, por un else-if ligado a la condición equivocada.
Se separó el clic MIDDLE creativo del manejo normal: si el craft normal tiene
éxito, se entrega el resultado; las verificaciones de destino/XP preceden consumo.
No se sustituye la arquitectura moderna por listeners legacy.

## Configuración y migración
Conservar config.yml completo y las dos opciones explícitas existentes. No requiere
migrar ítems de mundos ni bases de datos. No cambia metadatos en objetos guardados:
solo resultados de operaciones procesadas con la opción activa.
Instalaciones sin las claves mantienen defaultsfalse/0 y comportamiento upstream.
limit_repair_cost:true / limit_repair_value:250 mantienen el cap de operaciones
ordinarias. Recetas personalizadas conservan su contrato de coste propio.
JDK25 compila; bytecode21 y api-version1.21 sin elevación.

## Verificación y límites
154 pruebas automatizadas, incluida configuración, rango, desactivación, null,
meta y entradas/PDC conservados. Ensayo aislado en Leaf26.2build124 con y sin
EcoEnchants/EcoItems: consultar resultados finales en artifacts/CustomAnvil/2.2.0-valerin.2.
El probe usa API/NMS reales y eventos programáticos con jugador sintético y
captura de paquetes; verifica objetos entregados, XP/materiales y preview.
No hay cliente de red real ni validación humana, Bedrock, datos privados, runtime
Java21 o variante con bibliotecas externas. No equivale a gameplay humano.
No se instala ni activa en Custom, Sync ni Test; bot personal intacto.
La autenticación GitHub seguía pendiente al inicio: no copiar/modificar credenciales.
