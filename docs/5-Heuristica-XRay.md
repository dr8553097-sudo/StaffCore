# 💎 5. Detección Heurística de X-Ray

StaffCore incluye un algoritmo de detección heurística pasiva para detectar jugadores utilizando X-Ray o paquetes de recursos transparentes.

---

## 🧠 ¿Cómo Funciona el Algoritmo?

1. **Monitoreo de Ratios Mineral vs. Piedra:**
   - El sistema calcula el porcentaje de minerales valiosos (Diamante, Netherite, Oro, Esmeraldas) picados en relación con los bloques de piedra/pizarra circundantes.
2. **Periodo de Calentamiento (Warmup):**
   - Evita falsos positivos al picar una mena inicial con un número mínimo de bloques previos.
3. **Detección de Ráfagas (Burst Veins):**
   - Si un jugador encuentra múltiples vetas de diamante en una ventana de tiempo inverosímil (ej. 4 vetas separadas en menos de 60 segundos), se emite una alerta prioritaria al chat de staff.

---

## 📢 Gestión de Alertas

- **/xrayalerts on** — Activa las alertas de X-Ray en tu chat.
- **/xrayalerts off** — Desactiva las alertas visuales.
- **/xrayalerts stats <jugador>** — Muestra las estadísticas de minería y ratio calculado de un jugador en vivo.
