# Apocalipsis

Supervivencia con desastres dinámicos, misiones diarias y un árbol de habilidades que crece contigo. Incluye tutorial amable para nuevos jugadores y eventos narrativos especiales.

## Contexto de la serie
- **El Observador** es la voz que guía y reacciona: una consciencia fragmentada que recuerda eventos pasados (Eco de Brasas, Eco de Sombras, Susurro de Piedra Rota) y ahora busca respuestas en el End.
- **Eventos Eco previos** dejaron memorias: fuego, sombras y piedra rota; el nuevo contenido conecta esas memorias y abre preguntas sobre lo que escapó del End.
- **Tono**: supervivencia con misterio progresivo; la historia avanza a través de mensajes breves en juego, no con lecturas largas.

## Evento destacado: El Camino al End (mini-evento 2-3h)
- **Meta cooperativa**: reunir 40 Fragmentos del Vacío para revelar un portal incompleto (cliffhanger hacia la Apertura del End).
- **Fases**: Anomalias (exploracion diurna), Resonancia (atardecer tormentoso, tension creciente), Revelacion (noche, slow falling y secuencia final).
- **Anomalias (7 tipos)**: Normal, Inestable (Enderman y bonus por rapidez), Eco de Brasas/Sombras/Piedra (referencias a eventos previos), Oculta (casi invisible), Antigua (muy rara, habilita puzzle).
- **Mini-eventos cada 8-12 min**: eco de brasas, sombras, piedra; Resonancia que ilumina anomalias; Observacion (dialogos). Rompen la rutina y dan anomalias bonus.
- **Desafio Caza**: al progreso global se activa reto de 3 anomalias en 5 minutos, con fragmentos y PS extra si se completa.
- **Puzzle Antigua**: coloca 4 bloques-eco (Netherrack, Sculk, Deepslate, End Stone) en patron; otorga fragmentos extra y narrativa especial.
- **Clima progresivo**: dia tranquilo -> atardecer tormentoso -> noche con gravedad alterada.

## Para jugadores (sin leer código)
- **Objetivo:** Sobrevive mientras el mundo lanza desastres periódicos. Completa misiones, mejora habilidades y coopera.
- **Desastres:** Llegan en ciclos; avisan con barra y mensajes. Algunos ejemplos: meteoros, tormentas, invasiones. Prepárate y refúgiate.
- **Misiones diarias:** Al entrar cada día recibes misiones; si las dejas sin completar pierdes algo de XP al día siguiente.
- **Habilidades:** Gasta tu XP en un árbol de perks (defensa, movilidad, detección de recursos, invocaciones). Muchas son activables con `/toggle` o comandos rápidos.
- **Eventos narrativos:** Activaciones puntuales (Eco de Brasas, Eco de Sombras, Susurro Piedra Rota, Camino al End, Navidad) con recompensas y ambientación.
- **Tutorial y dificultad progresiva:** Los recién llegados tienen 4 fases de dificultad que suben poco a poco; reciben buffs suaves y tips en pantalla.

### Comandos útiles
- `/menu` abre el menú principal.
- `/recompensa` cobra tus recompensas pendientes.
- `/habilidades` abre el árbol de habilidades.
- `/mochila` y `/echest` accesos rápidos a inventario virtual y ender chest.
- `/waypoint [set]` crea/usa tu waypoint personal.
- `/coords` muestra tus coordenadas.
- `/toggle <habilidad>` activa o desactiva habilidades que lo permiten.
- `/misionestuto` ve hitos de tutorial/onboarding.
- `/carta` envía carta (evento navideño); `/cartas` admins.

## Para administradores
- Ciclo de desastres auto o manual (bloqueado si hay pocos jugadores, cooldown o safe-mode por TPS/TNT). BossBar única y contador en scoreboard/tab.
- Tutorial espera 5 min, luego registra al jugador y aplica buffs según fase. Dificultad progresa 0–4h hasta igualar la global.
- Misiones: asignación diaria, castigos pendientes para offline, puntos de supervivencia y rangos permanentes.
- Árbol de habilidades: XP como moneda, niveles y toggles persistentes; utilidades de exploración y combate.
 - Eventos narrativos vigentes: Camino al End con mini-eventos, desafio y puzzle Antigua; otros eventos Eco ya integrados.

## Instalación rápida
1) Coloca el JAR en `plugins/` de Paper 1.21.8. 2) Arranca una vez para generar configs. 3) Ajusta YAML en `plugins/Apocalipsis/` y reinicia.

## Requisitos técnicos
- Java 21; Maven 3.9+; Paper 1.21.8.
- Build local: `mvn clean package -DskipTests` → JAR sombreado en `target/Apocalipsis-<version>.jar` (v1.22.43).

## Archivos que verás
- Config: `config.yml`, `desastres.yml`, `eventos.yml`, `misiones_new.yml`, `rangos.yml`, `recompensas.yml`, `evasiones.yml`, `protecciones.yml`, `skills.yml`, `tutorial.yml`, `stream_features.yml`, `rangos_permanentes.yml`, `navidad.yml`.
- Datos persistentes: `mission_data.yml`, `skill_data.yml`, `tutorial_*.yml`, `state.yml` (estado global del ciclo).

## Código (referencia rápida)
- Principal: [src/main/java/me/apocalipsis/Apocalipsis.java](src/main/java/me/apocalipsis/Apocalipsis.java)
- Desastres: [src/main/java/me/apocalipsis/disaster/](src/main/java/me/apocalipsis/disaster/)
- Misiones: [src/main/java/me/apocalipsis/missions/](src/main/java/me/apocalipsis/missions/)
- Habilidades: [src/main/java/me/apocalipsis/skills/](src/main/java/me/apocalipsis/skills/)
- Tutorial/dificultad: [src/main/java/me/apocalipsis/tutorial/](src/main/java/me/apocalipsis/tutorial/)
- Declaración de comandos: [src/main/resources/plugin.yml](src/main/resources/plugin.yml)

## Licencia
MIT. Ver [LICENSE](LICENSE).
