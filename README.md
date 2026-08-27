# CronoSlot 3.0.1

Clean build prepared from the working CronoSlot camera base and the 3.0 UI.

This version fixes the Java compile errors found after merging:
- Removes duplicate `Models.java` model classes.
- Keeps the individual `Pilot`, `Car`, `Track`, `Session` and `Lap` classes as the single source of truth.
- Aligns `Car` with the mandatory car name used by the UI/database.
- Aligns `Pilot.remotes` with the multiple-mando design (`List<String>`).
- Adds `Db.laps(long)` required by the Excel exporter.
- Keeps the camera/detection implementation from the working camera version.
- Keeps the GitHub Actions build workflow.
