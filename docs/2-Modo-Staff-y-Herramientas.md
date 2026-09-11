# 🕵️ 2. Modo Staff y Herramientas

El **Modo Staff** es el núcleo operativo de StaffCore. Permite a los miembros del equipo aislarse de la jugabilidad estándar para moderar con máxima discreción y eficiencia.

---

## 🔄 Activación y Desactivación

- **Comando:** `/staff` o `/sc`
- **Permiso:** `staffcore.staff`

### ¿Qué ocurre al entrar en Modo Staff?
1. **Guardado automático de inventario:** El inventario del jugador, armadura, barra de experiencia y nivel se guardan de forma segura en memoria/disco.
2. **Asignación del Toolbar:** Se entregan 8 herramientas configuradas en `ui.yml`.
3. **Activación del Modo Vuelo (Flight):** El staff puede volar libremente.
4. **Vanish Opcional:** Si está configurado, el staff pasa automáticamente a modo invisible.
5. **✨ Activación del Aura Brillante (Glowing Outline):**
   - El modelo del personaje adquiere un contorno brillante continuo visible para identificar que está en labores de moderación.
   - Dos hélices de partículas orbitales en espiral (`END_ROD` y `SOUL_FIRE_FLAME`) giran continuamente alrededor del personaje.
6. **HUD de Action Bar:** Muestra en tiempo real TPS, Ping y estado del Vanish.

---

## 🛠️ Herramientas de la Hotbar

| Slot | Ítem | Herramienta | Acción |
|:---:|---|---|---|
| **0** | 🧭 Brújula | **Teletransporte** | Clic izquierdo: Teletransporte aleatorio. Clic derecho: Menú de selección de jugador. |
| **1** | 🧊 Hielo Compacto | **Freeze** | Clic izquierdo/derecho apuntando a un jugador: Congela o descongela inmediatamente. |
| **2** | 📦 Cofre | **Inspector** | Clic apuntando a un jugador: Abre su inventario, armadura, offhand, enderchest y estadísticas de salud/comida en tiempo real. |
| **3** | 👁️ Ojo de Ender | **Modo Espectador** | Clic derecho: Alterna instantáneamente entre modo espectador (noclip para atravesar bloques) y modo creativo/vuelo. |
| **4** | ⭐ Estrella del Nether | **Panel Staff** | Clic derecho: Abre el menú GUI maestro con accesos directos a reportes, sanciones y ajustes. |
| **5** | 🥕 Zanahoria Dorada | **Visión Nocturna** | Clic derecho: Alterna el efecto de visión nocturna infinita y clara para inspeccionar zonas oscuras o cuevas. |
| **7** | 🔮 Tinte Gris/Lima | **Vanish** | Clic derecho: Alterna visibilidad instantánea ante los demás jugadores. |
| **8** | 🔴 Polvo de Redstone | **Salir de Staff** | Clic derecho: Sale del modo staff, cancela el aura y restaura el inventario exacto previo. |

---

## ⚙️ Comandos de Movilidad Staff

- `/staff speed <0-10>` — Modifica tu velocidad de caminata.
- `/staff flyspeed <0-10>` — Modifica tu velocidad de vuelo (ej: `/staff flyspeed 3` para patrullar más rápido).
