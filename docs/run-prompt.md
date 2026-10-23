# One prompt to run all of the work

Copy the text below into a new Claude Code session for this repository (cloud or local).

---

You are continuing the Simple TV Launcher project. Do all of the work in `docs/tasks.md` (T01–T27)
in this run, and don't stop to ask me questions.

1. First read `CLAUDE.md`, `docs/sow.md`, `docs/tasks.md`, `docs/requirements.md`,
   `docs/ui-spec.md`, `docs/tech-stack.md`, `.claude/skills/README.md` and `docs/progress.md`.
   Start with the first unchecked task in `docs/tasks.md`: if the run is interrupted, the next
   one continues from the same place.
2. Do the tasks strictly in order. Before each one, load the skills and read the documents listed
   in its "Read" item.
3. After each task: the build and test gate is green → tick it in `docs/tasks.md` → add a line to
   `docs/progress.md` → commit. Push to the working branch after every milestone (M0–M5).
4. If something is undecided, take the default from the documents, record it in the "Decisions to
   review" section of `docs/progress.md`, and carry on. Stop only if a task cannot be done at all:
   then describe the blocker in `docs/progress.md` and move on to the next task that doesn't
   depend on it.
5. Draw new screens (T06) as mockups and implement them right away. Running this prompt is my
   approval; I'll look at the renders afterwards.
6. In the cloud, install the Android SDK with the script from T01; that is allowed. If Maven Central
   or the Gradle plugin portal answers 429, retry after a pause.
7. Open every recorded screenshot (Roborazzi) and compare it with its mockup in `design/mockups/`.
8. Don't break the principles in `CLAUDE.md`: no ads and no tracking, nothing runs in the
   background, stable library versions only, every text in all six languages.
9. Reply to me in the language I write in. Code, commits and documentation are in English.
10. At the end, send a short report: what was done, what didn't work and why, the size of the
    release APK, where to download the debug APK from CI, the "Decisions to review" list, and what
    to check on the TV (the "On the TV" section of `docs/tasks.md`).
