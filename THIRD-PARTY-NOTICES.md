# Third-party notices

## Shortest Path route engine and data

Better Map includes a package-relocated copy of the Shortest Path pathfinding engine and its
collision, transport, destination, and league data from
[Skretzo/shortest-path](https://github.com/Skretzo/shortest-path).

- Upstream revision: `6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a` (2026-09-02)
- License: BSD 2-Clause
- Copyright: (c) 2021-2026 Skretzo and Shortest Path contributors
- Java sources: `src/main/java/com/bettermap/pathfinding/`
- Data and full license text: `src/main/resources/com/bettermap/pathfinding/`

The copy is pinned; installed Shortest Path plugin updates do not replace it. In external-settings mode, Better Map sends
destinations to an enabled Shortest Path plugin through its public plugin-message API and reads
its saved routing settings. Better Map routing mode uses only its own settings and does not send
destinations to the external plugin. The two route calculations remain independent. Better Map is not
affiliated with or endorsed by the Shortest Path project.
