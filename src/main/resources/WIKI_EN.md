# StaffCore - Full Wiki (EN)

## 1. Project vision
StaffCore is a Paper moderation suite focused on being professional, configurable, and stable.
The goal is to let server teams moderate effectively without constant code edits.

Core priorities:
1. Clear experience for new admins.
2. Wide customization from `config.yml`.
3. Operational safety to reduce staff mistakes.
4. Compatibility with external moderation plugins.

## 2. Credits
Main author and project credit: **Dafealru**.

Technical support and architecture/config improvements in this iteration: development assistant.

## 3. What StaffCore includes
1. Staff mode with tools.
2. Configurable vanish.
3. Configurable freeze.
4. Player reports.
5. Punishments (warn/mute/kick/ban/tempban/unmute/unban).
6. Internal notes and player history.
7. Staff action logs.
8. Multi-language support.
9. Advanced X-Ray heuristic alerts for suspicious mining.

## 4. Modules and compatibility
You can disable modules if you already use another plugin for that feature.

`features` section in `config.yml`:
```yml
features:
  punishments: true
  reports: true
  freeze: true
  vanish: true
  staff-chat: true
  xray-alerts: true
```

Examples:
1. If you use LiteBans: `punishments: false`.
2. If you use SuperVanish: `vanish: false`.
3. If you use another reports plugin: `reports: false`.

## 5. Main commands
1. `/staff [on|off|menu|speed|flyspeed|reload|health|cleanfreeze|help]`
2. `/staff help [1-4]` (full command list in 4 pages + clickable in-chat navigation)
3. `/staffpanel` (aliases: `/spanel`, `/smenu`)
4. `/vanish` (alias: `/v`)
5. `/freeze <player> <reason>` (freeze), `/freeze <player>` (unfreeze), `/freeze claim <player>` (reassign case)
6. `/staffchat <on|off|message>` (alias: `/sc`)
7. `/chatmute [on|off|toggle|status]` (aliases: `/mutechat`, `/chatsilence`)
8. `/helpop <message>`
9. `/report <player> <reason>`
10. `/reports [close <id>]`
11. `/notes <player> <list|add|remove> ...`
12. `/history <player>`
13. `/warn <player> <reason>`
14. `/mute <player> <duration|perm> <reason>`
15. `/unmute <player>`
16. `/sckick <player> <reason>`
17. `/scban <player> <reason>`
18. `/sctempban <player> <duration> <reason>`
19. `/scunban <player>`
20. `/stafflogs [player|clear]`
21. `/stafflang <en|es|fr|pt>`
22. `/xrayalerts [on|off|toggle|module <on|off>|stats <player>]` (alias: `/xalerts`)

## 5.1 Permissions (node + purpose)
Recommended root:
1. `staffcore.admin`
Purpose: full admin package (includes most internal permissions).

By module:
1. `staffcore.staff` - toggle staff mode.
2. `staffcore.use` - alternative generic staff usage node.
3. `staffcore.vanish` - use vanish and GUI toggle.
4. `staffcore.vanish.see` - see vanished players.
5. `staffcore.freeze` - freeze/unfreeze by command and GUI.
6. `staffcore.freeze.bypass` - bypass frozen command restrictions.
7. `staffcore.staffchat` - use `/staffchat` and quick `@` chat.
8. `staffcore.reports.view` - open/view/close reports.
9. `staffcore.report` - create reports as player.
10. `staffcore.notes` - create/list/remove moderation notes.
11. `staffcore.history` - view punishments and notes history.
12. `staffcore.logs` - view staff action logs.
13. `staffcore.lang` - change language with `/stafflang`.
14. `staffcore.reload` - use `/staff reload`, `/staff health`, `/staff cleanfreeze`.
15. `staffcore.xray.alerts` - receive/manage X-Ray alerts.
16. `staffcore.xray.bypass` - bypass X-Ray heuristic checks.
17. `staffcore.speed` - use `/staff speed` and `/staff flyspeed`.
18. `staffcore.chatmute` - mute/unmute global chat with `/chatmute`.
19. `staffcore.chatmute.bypass` - allows chatting while global chat is muted.
20. `staffcore.update` - receives in-game update notices.

Punishment permissions:
1. `staffcore.punish.warn`
2. `staffcore.punish.mute`
3. `staffcore.punish.unmute`
4. `staffcore.punish.kick`
5. `staffcore.punish.ban`
6. `staffcore.punish.tempban`
7. `staffcore.punish.unban`

Hierarchy protection permissions:
1. `staffcore.protect` - marks player as protected target.
2. `staffcore.override` - allows high rank staff to bypass protection.

## 6. Launch subcommands
1. `/staff reload` - reloads config, languages, and managers.
2. `/staff health` - prints runtime and data-file health summary.
3. `/staff cleanfreeze` - removes offline frozen IDs from `frozen.yml`.

## 7. Data files
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

Reload backups are stored in `backups/` for:
1. `notes.yml`
2. `reports.yml`
3. `punishments.yml`
4. `staff-logs.yml`
5. `frozen.yml`

## 8. Key configuration summary
1. `language` - available languages and default.
2. `staff-mode` - behavior on staff mode enable/disable, including combat/world locks and dropped-item pickup lock.
3. `vanish` - collision, item pickup, mob clear radius, PvP block, world interaction lock, and `vanish.strict-hide-for-all` behavior.
4. `freeze` - allowed commands, reminder cooldown, reconnect behavior, visual response window (`freeze.response-window-seconds`), and private freeze chat bridge.
5. `staff-tools` - action behavior/cooldowns (tool display names and lores are in `lang/*.yml`).
6. `moderation` - hierarchy protection, reason limits, reason templates, and visual kick/ban links (`moderation.links.appeal-url`, `moderation.links.store-url`).
7. `features` - global module toggles.
8. `lang/*.yml` and `ui.yml` - full text/visual customization.
9. `menu.style` - GUI style system (materials/icons/fillers/title prefix, plus glow toggles and presets like `aurora-ops`).
10. `storage.async-save` and `storage.crash-safe` - batched async saves + temp/atomic replacement for safer writes.
11. `chat-moderation` - global chat mute state (`/chatmute`), bypass permission, and blocked-message cooldown.
12. `staff-logs` - retention policy to prevent `staff-logs.yml` from growing too much.
13. `helpop` - quick player-to-staff assistance channel.
14. `reports` - accept/reject workflow with reject reason prompt and timeout.
15. `xray-alerts` - thresholds, burst detector, tracked ores, runtime toggle, plus anti-false-positive whitelist and join warmup.
16. `staff-hud` - ActionBar HUD with TPS, ping, and vanish status.
17. `update-checker` - automatic Spigot version checks and in-game update notices for staff/op.

### 8.1 Log saturation control
Recommended setup:
```yml
staff-logs:
  auto-prune-on-reload: true
  retention-days: 30
  max-entries: 10000
  print-prune-summary: true
```

Meaning:
1. `auto-prune-on-reload`: automatically prunes on startup/reload.
2. `retention-days`: removes entries older than X days.
3. `max-entries`: caps the total number of log entries.
4. `print-prune-summary`: prints how many entries were pruned in console.

## 9. Punishment reason templates
```yml
moderation:
  reason-templates:
    warn: '{reason}'
    mute: '{reason}'
    kick: '{reason}'
    ban: '{reason}'
    tempban: '{reason}'
```

Available placeholders:
1. `{reason}`
2. `{staff}`
3. `{target}`
4. `{duration}`

Where these templates are applied:
1. `/warn`
2. `/mute`
3. `/sckick`
4. `/scban`
5. `/sctempban`
6. Staff panel quick actions (quick warn / quick mute)

Customization tips:
1. Use only `{reason}` for plain vanilla formatting.
2. Include `{staff}` and `{target}` for clearer audit trails.
3. Use `{duration}` on temporary sanctions to avoid ambiguity.
4. Keep templates short for better readability in history, logs, and kick/ban screens.

## 10. Recommended moderation flow
1. Review report or evidence.
2. In report GUI, review target and choose accept/reject.
3. If rejected, write a clear reason in chat so the reporter receives feedback.
4. If needed, apply temporary freeze.
5. Review player history (`/history` and `/notes`).
6. Apply proportional punishment.
7. Add internal note if necessary.

## 11. First-run admin setup
1. Review `language.default` and set your preferred default language.
2. Configure `features` based on your plugin stack.
3. Adjust `staff-hud` format and refresh interval.
4. Tune `freeze.allowed-commands-while-frozen` for your server.
5. Choose vanish visibility mode with `vanish.strict-hide-for-all`.
6. Tune `xray-alerts` thresholds for your mining economy.
7. Review moderation templates under `moderation.reason-templates`.
8. Set `storage.crash-safe.enabled` according to your durability/performance preference.
9. Assign permissions by rank (especially `staffcore.speed` and X-Ray permissions).
10. Open `/staff help` in-game to verify command visibility and translations.

## 12. Quick troubleshooting
1. Module not working: check `features.<module>`.
2. Permission issues: verify permission plugin setup.
3. Missing/broken text: verify `lang/*.yml` and `ui.yml` keys.
4. Unwanted freeze persistence: set `freeze.keep-frozen-on-quit: false` and run `/staff cleanfreeze`.
5. Tool conflicts: check inventory-related plugin conflicts.

## 13. Security and best practices
1. Do not edit production config without backups.
2. Test changes on staging first.
3. Keep release changelog.
4. Maintain a written moderation protocol for staff.
5. Avoid wildcard permissions for untrusted ranks.

## 14. Suggested roadmap
1. Optional Discord webhook integration.
2. CSV export for logs.
3. Extra confirmations for severe punishments.
4. Moderation statistics by period.
5. Permission audit command.

## 15. Project message
StaffCore aims to prove that a first serious plugin can still ship with real quality:
documented, configurable, and ready to scale.

## 16. Creator story
My name is **Dafealru**, I am **15 years old**, and I am from **Colombia**.
This plugin started because I wanted to learn real development by building something useful for live servers, not just small experiments.

What motivated me to create StaffCore:
1. I saw many staff teams needed clear and organized moderation tools.
2. I wanted my first major plugin to have a professional identity.
3. I wanted to learn solid practices in architecture, configuration, and long-term maintenance.

How the creation process went:
1. I started with a simple base for staff mode, vanish, and freeze.
2. Then I added reports, punishments, history, notes, and logs to cover the full moderation workflow.
3. After that, I improved configuration so most behavior could be customized without editing code.
4. Finally, I focused on documentation, multilingual support, and overall user experience for both staff and players.

This project represents my growth as a young developer and my commitment to building useful, stable, and increasingly professional tools.
