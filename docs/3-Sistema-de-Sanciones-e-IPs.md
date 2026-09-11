# 🔨 3. Sistema de Sanciones e IP Bans

StaffCore incluye un motor completo de moderación punitiva con soporte para sanciones por IP, temporales y permanentes.

---

## 🌐 Módulo de Baneos por IP

El baneo por IP bloquea tanto la cuenta del jugador como la dirección IP asociada, impidiendo que ingresen con cuentas secundarias.

### Comandos de IP Ban
| Comando | Sintaxis | Permiso | Descripción |
|---|---|---|---|
| **/scbanip** | `/scbanip <jugador|IP> [motivo]` | `staffcore.punish.banip` | Baneo permanente de IP y cuenta. |
| **/sctempbanip** | `/sctempbanip <jugador|IP> <duración> [motivo]` | `staffcore.punish.tempbanip` | Baneo temporal de IP y cuenta. |
| **/scunbanip** | `/scunbanip <jugador|IP>` | `staffcore.punish.unbanip` | Desbanea la IP y cuenta asociada. |

---

## ⏱️ Formatos de Tiempo Soportados

Para cualquier sanción temporal (`/sctempban`, `/sctempbanip`, `/tempmute`), puedes combinar las siguientes unidades:

- `s` = Segundos (ej: `30s`)
- `m` = Minutos (ej: `15m`)
- `h` = Horas (ej: `12h`)
- `d` = Días (ej: `7d`)
- `w` = Semanas (ej: `2w`)
- `mo` = Meses (ej: `1mo`)
- `y` = Años (ej: `1y`)

**Ejemplos:**
```bash
# Ban temporal de IP por 3 días y 12 horas:
/sctempbanip Hacker123 3d12h Uso de exploits y multi-cuentas

# Silencio temporal por 30 minutos:
/tempmute Spammer99 30m Toxicidad reiterada en chat global
```

---

## 📜 Historial y Auditoría de Sanciones

- `/history <jugador>` — Muestra el historial completo de sanciones recibidas por un jugador (advertencias, mutes, kicks y bans).
- `/notes <jugador> add <nota>` — Añade notas internas para el equipo de moderación.
- `/stafflogs [jugador]` — Registro de acciones ejecutadas por los moderadores.
