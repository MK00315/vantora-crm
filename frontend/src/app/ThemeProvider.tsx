import { useEffect, type PropsWithChildren } from 'react';

const THEME_STORAGE_KEY = 'vantora_theme_v2';

export function ThemeProvider({ children }: PropsWithChildren) {
  useEffect(() => {
    document.documentElement.classList.remove('dark');
    document.documentElement.style.colorScheme = 'light';
    localStorage.setItem(THEME_STORAGE_KEY, 'light');
    const themeColor = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (themeColor) themeColor.content = '#f7f9fc';
  }, []);

  return children;
}
