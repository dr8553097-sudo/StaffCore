# 🚀 1. Instalación y Requisitos

En esta sección aprenderás cómo instalar y poner en marcha **StaffCore** en tu servidor.

---

## 📋 Requisitos del Sistema

| Requisito | Versión Mínima | Recomendado |
|---|---|---|
| **Java** | Java 21 | Eclipse Temurin 21 o Java 25 |
| **Plataforma de Servidor** | Paper 1.21.x / Purpur 1.21.x | Paper 1.21.1+ (Build 92+) |
| **Plugin de Permisos** | Cualquiera con soporte Vault/Bukkit | **LuckPerms v5.4+** |

> ⚠️ **Importante:** StaffCore utiliza características nativas modernas de la API de Paper (componentes Adventure, efectos visuales de Glowing y partículas avanzadas), por lo que **Java 21+ es obligatorio**.

---

## 📥 Pasos de Instalación

1. **Descarga el plugin:**
   - Descarga la última versión de **StaffCore-1.3.0.jar** desde [Modrinth](https://modrinth.com/plugin/staffcore-dafealru) o [GitHub Releases](https://github.com/dr8553097-sudo/StaffCore/releases).

2. **Copia el archivo en tu servidor:**
   - Sube el archivo `StaffCore-1.3.0.jar` dentro de la carpeta `/plugins/` de tu servidor.

3. **Inicia o recarga el servidor:**
   - Inicia tu servidor o ejecuta `plugman load StaffCore` si usas PlugManX.
   - En la consola verás el banner de bienvenida confirmando la compatibilidad:
     ```
     [StaffCore] ----------------------------------------
     [StaffCore] StaffCore | Modern Moderation Suite
     [StaffCore] Version: 1.3.0
     [StaffCore] Supported Version: Yes (Native 1.21.x / 26.x)
     [StaffCore] ----------------------------------------
     ```

4. **Estructura generada:**
   En `/plugins/StaffCore/` se crearán los siguientes archivos:
   - `config.yml` — Configuración principal de módulos, tiempos y formato.
   - `ui.yml` — Personalización de slots, nombres, lore e ítems de los menús e inventarios.
   - `lang/` — Carpeta con los archivos de idioma (`es.yml`, `en.yml`, `fr.yml`, `pt.yml`).
   - `data/` — Base de datos interna de sanciones, notas, historiales y tickets.
