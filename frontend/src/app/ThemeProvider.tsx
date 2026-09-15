import { useEffect, type PropsWithChildren } from 'react';
import { usePreferences } from './preferences';

const THEME_STORAGE_KEY = 'vantora_theme_v2';

export function ThemeProvider({ children }: PropsWithChildren) {
  const { preferences } = usePreferences();
  useEffect(() => {
    document.documentElement.dataset.density = preferences.density;
    document.documentElement.dataset.reduceMotion = String(preferences.reduceMotion);
  }, [preferences]);
  useEffect(() => {
    document.documentElement.classList.remove('dark');
    document.documentElement.style.colorScheme = 'light';
    try { localStorage.setItem(THEME_STORAGE_KEY, 'light'); } catch { /* Appearance still applies when storage is unavailable. */ }
    const themeColor = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (themeColor) themeColor.content = '#f7f8fa';
  }, []);

  return children;
}
