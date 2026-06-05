# Script YAML Schema Design

The script YAML is organized as `project -> characters -> episodes -> scenes -> dialogues`.

- `project` keeps high-level metadata for listing, display, generation settings, and export.
- `characters` is centralized so scenes can reference stable character IDs instead of repeating character descriptions.
- `episodes` matches the normal production structure of short dramas, web dramas, and serialized adaptations.
- `scenes` keeps each shootable unit explicit: location, time, characters, action, and dialogue.
- `dialogues` stores speaker and line separately, making later dialogue polishing and consistency checks easier.

This structure is intentionally small for the MVP. It is strict enough for validation and editing, but it does not force characters, episodes, or scenes into separate database tables before the core flow is proven.
