# ⚙️ 7. Configuración Avanzada y Colores HEX

StaffCore permite personalizar cada aspecto estético, mensaje y comportamiento del plugin.

---

## 🎨 Soporte de Colores HEX / RGB

Puedes usar colores HEX modernos en cualquier mensaje, título o ítem dentro de `config.yml`, `ui.yml` o los archivos de idioma:

- **Formato estándar:** `&#RRGGBB` (ejemplo: `&#00FFAA`)
- **Formato Adventure:** `<#RRGGBB>` (ejemplo: `<#3498db>`)
- **Códigos tradicionales de Minecraft:** `&0` a `&f`, `&l` (negrita), `&o` (cursiva), etc.

---

## 🌐 Configuración Multi-Idioma

En `config.yml`:
```yaml
language:
  # Idioma por defecto para nuevos jugadores (en, es, fr, pt)
  default: en
  
  # Si es true, /stafflang actualizará también el idioma global
  stafflang-updates-default: false
  
  # Idiomas disponibles en /plugins/StaffCore/lang/
  available:
    - en
    - es
    - fr
    - pt
```

### Cambio de idioma en el juego:
- `/stafflang es` — Cambia tu idioma personal a Español.
- `/stafflang en` — Switch to English.
