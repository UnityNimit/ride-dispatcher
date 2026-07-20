/**
 * sockjs-client expects Node's `global`. Browsers only have `globalThis` / `window`.
 */
(globalThis as typeof globalThis & { global?: typeof globalThis }).global = globalThis;
