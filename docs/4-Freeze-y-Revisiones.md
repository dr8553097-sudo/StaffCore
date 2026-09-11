# 🧊 4. Sistema Anti-Bypass de Freeze

El módulo de **Freeze** está diseñado para inmovilizar a sospechosos de trampas (hacks/cheats) durante una revisión (SS / Screen Share).

---

## 🛡️ Protecciones Anti-Bypass Activas

Cuando un jugador es congelado:
1. **Inmovilización total:** Bloqueo de movimiento, salto, teletransporte y daño por caída.
2. **Efectos visuales inmersivos:** Efecto de viñeta de congelación de nieve polvo (`POWDER_SNOW`) y sonido ambiente.
3. **Bloqueo de interacción:**
   - No puede abrir cofres, hornos ni inventarios.
   - No puede soltar ítems (`Q`) ni moverlos de su inventario.
   - No puede montar entidades (caballos, botes, vagonetas).
   - No puede disparar flechas, lanzar perlas de ender ni usar tridentes con propulsión acuática (Riptide).
   - No puede volar con Elytras.
4. **Persistencia al reconectar:** Si el jugador se desconecta para evadir la revisión, al volver a entrar seguirá automáticamente congelado y se notificará a los moderadores.

---

## 💬 Canales y Claim de Freeze

- **/freeze <jugador> [motivo]:** Congela o descongela a la víctima.
- **/freeze claim <jugador>:** El moderador toma propiedad exclusiva de la revisión.
- **Freeze Chat:** Mensajes directos y privados entre el staff a cargo y el sospechoso para coordinar la revisión por Discord o AnyDesk sin interferencias del chat global.
