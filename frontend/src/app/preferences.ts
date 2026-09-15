import { useSyncExternalStore } from 'react';

export interface Preferences {
  density: 'comfortable' | 'compact';
  reduceMotion: boolean;
}

export const PREFERENCES_KEY = 'vantora_display_preferences';
const defaults: Preferences = { density: 'comfortable', reduceMotion: false };

function readPreferences(): Preferences {
  try {
    const stored = JSON.parse(localStorage.getItem(PREFERENCES_KEY) ?? 'null');
    return {
      density: stored?.density === 'compact' ? 'compact' : 'comfortable',
      reduceMotion: stored?.reduceMotion === true,
    };
  } catch { return defaults; }
}

let current = readPreferences();
const listeners = new Set<() => void>();
let saved = true;

export function updatePreferences(patch: Partial<Preferences>) {
  current = { ...current, ...patch };
  try {
    localStorage.setItem(PREFERENCES_KEY, JSON.stringify(current));
    saved = true;
  } catch { saved = false; }
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  const onStorage = (event: StorageEvent) => {
    if (event.key === PREFERENCES_KEY || event.key === null) {
      current = readPreferences();
      saved = true;
      listener();
    }
  };
  window.addEventListener('storage', onStorage);
  return () => {
    listeners.delete(listener);
    window.removeEventListener('storage', onStorage);
  };
}

export function usePreferences() {
  const preferences = useSyncExternalStore(subscribe, () => current);
  return { preferences, saved, updatePreferences };
}

const motionQuery = '(prefers-reduced-motion: reduce)';
function subscribeToMotion(listener: () => void) {
  const media = window.matchMedia(motionQuery);
  media.addEventListener('change', listener);
  return () => media.removeEventListener('change', listener);
}

export function useReducedMotion() {
  const { preferences } = usePreferences();
  const deviceReducedMotion = useSyncExternalStore(subscribeToMotion, () => window.matchMedia(motionQuery).matches);
  return preferences.reduceMotion || deviceReducedMotion;
}
