# Fusión entre EcoItems distintos — 2.2.0-valerin.4

## Diferencia confirmada contra el JAR productivo
SHA256 legacy:4571184ae4e6601cb8769e152ef99f8ed359202fbf826bb9be60cd4ff385e37c.
Decompilación y javap confirman ItemUtil.canMergeWith:
customType izquierdo == customType derecho OR material izquierdo == material derecho
OR el objeto derecho es un libro encantado. Bytecode offsets30..38 compara
ItemStack.getType y salta a true con if_acmpeq. La revisión inicial omitió esta
diferencia; no inferir equivalencia del repositorio legacy con el JAR.

La fuente2.2.0-valerin.3 conservaba solo igualdad de clavecustom y libro. Dos
EcoItems distintos con igual material no llegaban a doMerge, ni en preparación
ni en extracción. Este bloqueo precede a validar sus encantamientos custom.

## Porte
ItemUtil.canMergeWith delega en canMergeTypes que restituye las tres condiciones.
Ambos listeners usan esta misma función. El resultado sigue partiendo del objeto
izquierdo (DependencyManager.cloneItem), preservando identidad y metadatos;
el derecho se consume según la lógica moderna de extracción ya existente.
No mezclar identidades, habilidades o lore de dos EcoItems: solo los encantamientos
permitidos y la reparación habitual. EcoEnchants y las reglas de conflicto/nivel
pueden rechazar una combinación aunque los materiales sean compatibles.

Dos espadas de diamante con IDsEco distintos: compatibles para intentar fusión.
Espada de diamante y espada de netherite, o espada y hacha: no son compatibles
solo por ser herramientas/armas. IgualIDcustom conserva compatibilidad legacy.
También EcoItem+objeto vanilla de igual material es compatible como en el binario.
Esta restauración reproduce el contrato del JAR, no una regla inventada.
Sin cambios de permisos, configuraciones, dependencias o API mínima.
Mantiene el resetRepairCost, capXP250 y elegibilidadEco de las entregas anteriores.

## Verificación sin servidores
Se añaden seis pruebas puras de las condiciones de compatibilidad: distintos
IDsEco/mismo material; distintos materiales; mismoID; libro derecho; Eco+vanilla;
vanilla. Se ejecuta además la suite previa. No incluye un registroEcoItems real
ni cliente ni servidorMinecraft: prohibición del propietario sigue vigente.
Los ensayosLeaf de versiones anteriores no certifican este candidato.
No instalar, recargar, reiniciar ni cambiar permisos productivos.
Artefactos2.2.0-valerin.4 y evidencias en artifacts/CustomAnvil/2.2.0-valerin.4.
Bytecode de referencia fuera del repo en reviews/CustomAnvil/legacy-1.16.3/
bytecode/io.delilaheve.util.ItemUtil.txt. No hay migración de datos/config necesaria.
