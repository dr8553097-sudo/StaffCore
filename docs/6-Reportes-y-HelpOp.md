# 📋 6. Sistema de Tickets y HelpOp

StaffCore reemplaza los mensajes desorganizados con un sistema de tickets estructurado para atender a los jugadores.

---

## 🚨 Reportes de Jugadores (`/report`)

Los usuarios pueden reportar a infractores de forma sencilla:
```bash
/report <jugador> <motivo>
```

### Características:
- **Cooldown anti-spam:** Evita que los jugadores saturen al equipo con reportes masivos.
- **Notificación en tiempo real:** Los moderadores conectados reciben una alerta sonora y visual en el chat con botones interactivos.

---

## 🗂️ Panel de Gestión de Reportes (`/reports`)

Al ejecutar `/reports`, los miembros del staff abren un menú interactivo donde pueden:
1. **Ver reportes pendientes:** Con nombre del acusado, denunciante, motivo y hora.
2. **Reclamar ticket (Claim):** Asignarse el caso para que otros moderadores sepan quién lo está investigando.
3. **Resolver o Rechazar:** Al cerrar el ticket, se envía un mensaje de agradecimiento o retroalimentación al jugador denunciante.

---

## 🆘 Canal de Emergencia (`/helpop`)

- **Uso:** `/helpop <mensaje>`
- Permite a cualquier jugador enviar una consulta o solicitud de auxilio urgente que solo los miembros con permiso `staffcore.helpop` podrán leer y responder.
