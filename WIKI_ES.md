# StaffCore - Wiki Completa (ES)

## 1. Vision del proyecto
StaffCore es una suite de moderacion para Paper enfocada en servidores hispanohablantes.
La meta es tener un plugin profesional, configurable y estable, que permita moderar bien sin depender de cambios constantes de codigo.

Este proyecto nace como primer plugin serio y por eso prioriza:
1. Claridad para admins nuevos.
2. Configuracion amplia desde `config.yml`.
3. Seguridad operacional para evitar errores de staff.
4. Compatibilidad con otros plugins de moderacion.

## 2. Creditos
Autor base y credito principal: **Dafealru**.

Soporte tecnico y mejoras de arquitectura/configuracion en esta iteracion: asistente de desarrollo.

## 3. Que hace StaffCore
StaffCore centraliza moderacion en modulos:
1. Staff mode con herramientas.
2. Vanish configurable.
3. Freeze configurable.
4. Reportes de jugadores.
5. Sistema de sanciones (warn/mute/kick/ban/tempban/unmute/unban).
6. Notas internas y historial.
7. Logs de staff.
8. Multiidioma.
9. Alertas heuristicas avanzadas de X-Ray para mineria sospechosa.

## 4. Modulos y compatibilidad
Si ya usas otros plugins para ciertas funciones, puedes apagar modulos sin desinstalar StaffCore.

Seccion `features` en `config.yml`:
```yml
features:
  punishments: true
  reports: true
  freeze: true
  vanish: true
  staff-chat: true
  xray-alerts: true
```

Ejemplos:
1. Si usas LiteBans: `punishments: false`.
2. Si usas SuperVanish: `vanish: false`.
3. Si usas plugin externo de reportes: `reports: false`.

## 5. Comandos principales
1. `/staff [on|off|menu|speed|flyspeed|reload|health|cleanfreeze|help]`
2. `/staff help [1-4]` (lista completa en 4 paginas + navegacion por click dentro del chat)
3. `/staffpanel` (alias: `/spanel`, `/smenu`)
4. `/vanish` (alias: `/v`)
5. `/freeze <jugador> <razon>` (congelar), `/freeze <jugador>` (descongelar), `/freeze claim <jugador>` (reasignar caso)
6. `/staffchat <on|off|mensaje>` (alias: `/sc`)
7. `/chatmute [on|off|toggle|status]` (alias: `/mutechat`, `/chatsilence`)
8. `/helpop <mensaje>`
9. `/report <jugador> <razon>`
10. `/reports [close <id>]`
11. `/notes <jugador> <list|add|remove> ...`
12. `/history <jugador>`
13. `/warn <jugador> <razon>`
14. `/mute <jugador> <duracion|perm> <razon>`
15. `/unmute <jugador>`
16. `/sckick <jugador> <razon>`
17. `/scban <jugador> <razon>`
18. `/sctempban <jugador> <duracion> <razon>`
19. `/scunban <jugador>`
20. `/stafflogs [jugador|clear]`
21. `/stafflang <en|es|fr|pt>`
22. `/xrayalerts [on|off|toggle|module <on|off>|stats <jugador>]` (alias: `/xalerts`)

## 5.1 Permisos (nodos + funcion)
Permiso raiz recomendado:
1. `staffcore.admin`
Funcion: paquete completo para administradores (incluye la mayoria de permisos internos).

Permisos por modulo:
1. `staffcore.staff`
Funcion: activar/desactivar staff mode.
2. `staffcore.use`
Funcion: permiso alterno de uso general de staff (compatibilidad con setups antiguos).
3. `staffcore.vanish`
Funcion: ejecutar vanish y usar toggle de vanish en panel.
4. `staffcore.vanish.see`
Funcion: ver jugadores que estan en vanish.
5. `staffcore.freeze`
Funcion: congelar/descongelar por comando y GUI.
6. `staffcore.freeze.bypass`
Funcion: evita bloqueos de comandos por estado frozen.
7. `staffcore.staffchat`
Funcion: usar `/staffchat` y chat rapido con `@`.
8. `staffcore.reports.view`
Funcion: abrir, ver y cerrar reportes.
9. `staffcore.report`
Funcion: crear reportes como jugador.
10. `staffcore.notes`
Funcion: crear/listar/eliminar notas de moderacion.
11. `staffcore.history`
Funcion: consultar historial de sanciones y notas.
12. `staffcore.logs`
Funcion: ver logs de acciones del staff.
13. `staffcore.lang`
Funcion: cambiar idioma con `/stafflang`.
14. `staffcore.reload`
Funcion: usar `/staff reload`, `/staff health`, `/staff cleanfreeze`.
15. `staffcore.xray.alerts`
Funcion: recibir/gestionar alertas de X-Ray.
16. `staffcore.xray.bypass`
Funcion: evitar chequeos heuristicos de X-Ray.
17. `staffcore.speed`
Funcion: usar `/staff speed` y `/staff flyspeed`.
18. `staffcore.chatmute`
Funcion: silenciar/desilenciar el chat global con `/chatmute`.
19. `staffcore.chatmute.bypass`
Funcion: permite hablar mientras el chat global este silenciado.
20. `staffcore.update`
Funcion: recibe avisos de nuevas versiones dentro del juego.

Permisos de sanciones:
1. `staffcore.punish.warn`
Funcion: advertir jugadores.
2. `staffcore.punish.mute`
Funcion: mutear jugadores.
3. `staffcore.punish.unmute`
Funcion: desmutear jugadores.
4. `staffcore.punish.kick`
Funcion: expulsar jugadores.
5. `staffcore.punish.ban`
Funcion: ban permanente.
6. `staffcore.punish.tempban`
Funcion: ban temporal.
7. `staffcore.punish.unban`
Funcion: quitar ban (incluye uso desde consola).

Permisos de proteccion jerarquica:
1. `staffcore.protect`
Funcion: marca jugador como objetivo protegido.
2. `staffcore.override`
Funcion: permite a staff superior ignorar proteccion.

Asignacion sugerida por rango:
1. `Helper`
`staffcore.staff`, `staffcore.use`, `staffcore.staffchat`, `staffcore.reports.view`, `staffcore.history`
2. `Moderador`
Todo lo de Helper + `staffcore.freeze`, `staffcore.notes`, `staffcore.punish.warn`, `staffcore.punish.mute`, `staffcore.punish.unmute`, `staffcore.punish.kick`
3. `Administrador`
`staffcore.admin` + `staffcore.reload` + `staffcore.override`

## 6. Subcomandos de lanzamiento
1. `/staff reload`
Carga configuracion, idiomas y managers.
2. `/staff health`
Muestra estado de archivos criticos y conteos de runtime.
3. `/staff cleanfreeze`
Limpia IDs congeladas offline en `frozen.yml`.

## 7. Estructura de datos
Archivos usados por el plugin:
1. `config.yml`
2. `lang/en.yml`
3. `lang/es.yml`
4. `lang/fr.yml`
5. `lang/pt.yml`
6. `notes.yml`
7. `reports.yml`
8. `punishments.yml`
9. `staff-logs.yml`
10. `frozen.yml`
11. `player-languages.yml`
12. `wiki/WIKI_ES.md`
13. `wiki/WIKI_EN.md`

Respaldos automaticos al hacer reload:
1. `notes-*`
2. `reports-*`
3. `punishments-*`
4. `staff-logs-*`
5. `frozen-*`

Se guardan en carpeta `backups`.

## 8. Configuracion clave (resumen)
1. `language`
Idiomas disponibles y default.
2. `staff-mode`
Comportamiento al entrar/salir de staff mode, incluyendo bloqueos de combate/mundo y bloqueo de pickup de items del suelo.
3. `vanish`
Colision, pickup de items, radio de limpieza de mobs, bloqueo PvP, bloqueo de interaccion/edicion del mundo y modo `vanish.strict-hide-for-all`.
4. `freeze`
Comandos permitidos, cooldown de recordatorio, persistencia al reconectar, ventana visual (`freeze.response-window-seconds`) y chat privado freeze.
5. `staff-tools`
Comportamiento/cooldown de herramientas (nombres y lores en `lang/*.yml`).
6. `moderation`
Proteccion por jerarquia, largo minimo de razones, templates de razon y links visuales para plantillas de kick/ban (`moderation.links.appeal-url`, `moderation.links.store-url`).
7. `features`
Encendido/apagado de modulos completos.
8. `lang/*.yml` y `ui.yml`
Personalizacion completa de textos y visuales.
9. `storage.async-save` y `storage.crash-safe`
Guardado por lotes asincrono + escritura segura con archivo temporal/reemplazo atomico.
10. `chat-moderation`
Control del estado global del chat (`/chatmute`), permiso bypass y cooldown del aviso al jugador.
11. `menu.style`
Tema visual del panel staff (preset `aurora-ops`, brillo de botones, iconos, fillers y prefijo de titulo).
12. `staff-logs`
Control de retencion para evitar saturacion de `staff-logs.yml`.
13. `helpop`
Canal rapido de solicitud de ayuda para usuarios.
14. `reports`
Flujo de aceptar/rechazar con motivo de rechazo y timeout del prompt por chat.
15. `xray-alerts`
Umbrales, detector burst, minerales rastreados, runtime toggle, whitelist anti-falsos positivos y warmup por conexion.
16. `staff-hud`
HUD ActionBar con TPS, ping y estado vanish.
17. `update-checker`
Chequeo automatico de versiones en Spigot y aviso en juego para staff/op.

### 8.1 Control de saturacion de logs
Configuracion recomendada:
```yml
staff-logs:
  auto-prune-on-reload: true
  retention-days: 30
  max-entries: 10000
  print-prune-summary: true
```

Significado:
1. `auto-prune-on-reload`: limpia automaticamente al iniciar/recargar.
2. `retention-days`: elimina logs mas viejos que X dias.
3. `max-entries`: limita el tamano maximo del archivo.
4. `print-prune-summary`: muestra en consola cuantos logs borro.

## 9. Plantillas de razon de sancion
Puedes personalizar formato final de razones:
```yml
moderation:
  reason-templates:
    warn: '{reason}'
    mute: '{reason}'
    kick: '{reason}'
    ban: '{reason}'
    tempban: '{reason}'
```

Placeholders disponibles:
1. `{reason}`
2. `{staff}`
3. `{target}`
4. `{duration}`

Donde se aplican estas plantillas:
1. `/warn`
2. `/mute`
3. `/sckick`
4. `/scban`
5. `/sctempban`
6. Acciones rapidas del panel staff (quick warn / quick mute)

Consejos de personalizacion:
1. Si quieres estilo simple, usa solo `{reason}`.
2. Si quieres auditoria clara, incluye `{staff}` y `{target}`.
3. En sanciones temporales, usa `{duration}` para evitar confusiones.
4. Mantenerlas cortas mejora lectura en historial, logs y pantalla de ban/kick.

## 10. Flujo recomendado de moderacion
1. Revisar reporte o evidencia.
2. En GUI de reportes, revisar objetivo y elegir aceptar/rechazar.
3. Si se rechaza, escribir una razon clara en chat para que el reportero reciba feedback.
4. Si hay sospecha activa, freeze temporal.
5. Revisar historial (`/history` y `/notes`).
6. Aplicar sancion proporcional.
7. Registrar nota interna si aplica.

## 11. Configuracion inicial recomendada
1. Revisar `language.default` y definir el idioma por defecto del servidor.
2. Ajustar `features` segun tu stack de plugins.
3. Personalizar `staff-hud` (formato e intervalo).
4. Ajustar `freeze.allowed-commands-while-frozen` para tu modalidad.
5. Elegir modo de visibilidad vanish con `vanish.strict-hide-for-all`.
6. Afinar umbrales de `xray-alerts` para tu economia/mineria.
7. Revisar templates de sancion en `moderation.reason-templates`.
8. Configurar `storage.crash-safe.enabled` segun tu preferencia de seguridad/rendimiento.
9. Asignar permisos por rango (especialmente `staffcore.speed` y nodos X-Ray).
10. Verificar `/staff help` en juego para confirmar traducciones y comandos visibles.

## 12. Troubleshooting rapido
1. Un modulo "no funciona"
Revisar `features.<modulo>`.
2. Un comando responde sin permiso
Revisar permisos en tu plugin de permisos.
3. Mensajes raros o faltantes
Revisar claves en `lang/*.yml` y `ui.yml`.
4. Freeze persistente no deseado
Poner `freeze.keep-frozen-on-quit: false` y ejecutar `/staff cleanfreeze`.
5. Staff tools se pierden o cambian
Revisar conflictos con otros plugins de inventario.

## 13. Seguridad y buenas practicas
1. No editar config directo en produccion sin backup.
2. Probar cambios en servidor de staging.
3. Guardar changelog de cada release.
4. Mantener protocolo de moderacion escrito para staff.
5. Evitar permisos wildcard a rangos no confiables.

## 14. Roadmap recomendado
1. Integracion opcional con Discord webhook.
2. Export de logs a CSV.
3. Confirmaciones extra para sanciones graves.
4. Estadisticas de moderacion por periodo.
5. Comando de auditoria de permisos.

## 15. Mensaje del proyecto
Este plugin busca demostrar que un primer proyecto puede salir con calidad real: bien documentado, configurable y preparado para crecer.

## 16. Historia del creador
Mi nombre es **Dafealru**, tengo **15 años** y soy de **Colombia**.
Este plugin nacio porque queria aprender desarrollo real creando algo util para servidores, no solo hacer pruebas pequenas.

Lo que me animo a crear StaffCore:
1. Ver que muchos equipos de staff necesitaban herramientas claras y ordenadas.
2. Construir un plugin propio con identidad profesional desde mi primera gran version.
3. Aprender buenas practicas de arquitectura, configuracion y mantenimiento.

Como fue mi proceso de creacion:
1. Empece con una base simple para staff mode, vanish y freeze.
2. Luego agregue reportes, sanciones, historial, notas y logs para tener flujo completo de moderacion.
3. Despues mejore la configuracion para que casi todo se pudiera ajustar sin tocar codigo.
4. Finalmente trabaje en la documentacion, el multiidioma y la experiencia de uso para staff y jugadores.

Este proyecto representa mi crecimiento como desarrollador joven y mi compromiso con crear herramientas utiles, estables y cada vez mas profesionales.

