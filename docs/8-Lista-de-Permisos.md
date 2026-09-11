# 🔐 8. Matriz Completa de Permisos

A continuación se detalla la lista completa de nodos de permisos para configurar en **LuckPerms**.

---

## 👑 Permisos Globales / Wildcards

| Nodo | Descripción |
|---|---|
| `staffcore.*` | Acceso absoluto a todas las funciones y comandos del plugin. |
| `staffcore.admin` | Permiso maestro de administrador (incluye configuración, reload y sanciones máximas). |
| `staffcore.staff` | Permiso base para miembros del equipo (modo staff, vanish, freeze, inspector, etc.). |

---

## 📌 Permisos Específicos por Módulo

### Modo Staff y Herramientas
- `staffcore.staff` — Usar `/staff`, entrar a modo staff y usar las herramientas.
- `staffcore.vanish` — Usar `/vanish` y modo invisible.
- `staffcore.freeze` — Usar `/freeze` y congelar jugadores.
- `staffcore.inspect` — Inspeccionar inventarios de jugadores.

### Sanciones
- `staffcore.punish.ban` — Usar `/scban` y `/sctempban`.
- `staffcore.punish.banip` — Usar `/scbanip`.
- `staffcore.punish.tempbanip` — Usar `/sctempbanip`.
- `staffcore.punish.unbanip` — Usar `/scunbanip`.
- `staffcore.punish.mute` — Usar `/mute` y `/tempmute`.
- `staffcore.punish.kick` — Usar `/sckick`.
- `staffcore.punish.warn` — Usar `/warn`.

### Comunicación y Auditoría
- `staffcore.chat` — Acceso a `/staffchat`.
- `staffcore.chatmute` — Silenciar el chat global con `/chatmute`.
- `staffcore.helpop` — Recibir y responder mensajes de `/helpop`.
- `staffcore.reports` — Administrar tickets con `/reports`.
- `staffcore.xray` — Recibir alertas de minado sospechoso.
- `staffcore.logs` — Consultar auditorías con `/stafflogs`.
