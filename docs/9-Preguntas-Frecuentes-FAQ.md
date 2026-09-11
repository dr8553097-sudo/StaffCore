# ❓ 9. Preguntas Frecuentes (FAQ)

Respuestas directas a las dudas más comunes sobre StaffCore.

---

### 1. ¿Es compatible con PlugManX / recargas en caliente?
**Sí.** StaffCore 1.3.0 cuenta con apagado seguro (`disableSafely`), cancelando tareas de aura, desvanecimientos e inventarios sin generar errores de tipo `IllegalPluginAccessException`.

### 2. ¿Cómo configuro las alertas de X-Ray para que no avisen por hierro o carbón?
En `config.yml` puedes personalizar la lista de bloques auditados bajo la sección `xray.monitored-blocks` para incluir exclusivamente menas de diamante (`DIAMOND_ORE`, `DEEPSLATE_DIAMOND_ORE`) y escombros ancestrales (`ANCIENT_DEBRIS`).

### 3. ¿El aura brillante (Glowing) causa lag en el cliente o servidor?
No. El efecto de contorno brillante utiliza la función nativa de Minecraft (`player.setGlowing(true)`) que es procesada por la tarjeta gráfica del cliente sin carga adicional para el servidor. Las partículas orbitales están optimizadas mediante fórmulas trigonométricas de bajo consumo.

### 4. ¿Dónde se guardan los datos de sanciones y jugadores?
Todos los registros se guardan en la carpeta `/plugins/StaffCore/data/` utilizando persistencia asíncrona en YAML para garantizar cero caídas de TPS en el servidor principal.

---

<div align="center">

¿Tienes una pregunta no listada? [Abre un issue de soporte en GitHub](https://github.com/dr8553097-sudo/StaffCore/issues/new?template=question.yml)

</div>
