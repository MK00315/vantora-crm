import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        ink: {
          50: '#f8f9fa',
          100: '#f1f3f5',
          200: '#e4e7eb',
          300: '#cbd0d6',
          400: '#9aa2ad',
          500: '#66707f',
          600: '#505968',
          700: '#374151',
          800: '#252c38',
          900: '#171d27',
          950: '#0f141d',
        },
        brand: {
          50: '#effcfb',
          100: '#d4f7f3',
          200: '#aaeae4',
          300: '#75d8cf',
          400: '#3cbdb2',
          500: '#179f96',
          600: '#0f7f78',
          700: '#116660',
          800: '#13514d',
          900: '#134441',
          950: '#062927',
        },
        sand: '#f7f8fa',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        soft: '0 1px 2px rgb(15 23 42 / 0.06), 0 1px 3px rgb(15 23 42 / 0.04)',
        lift: '0 16px 40px -20px rgb(15 23 42 / 0.28)',
      },
      animation: {
        'fade-up': 'fadeUp .35s ease-out both',
        'pulse-soft': 'pulseSoft 2s ease-in-out infinite',
      },
      keyframes: {
        fadeUp: {
          from: { opacity: '0', transform: 'translateY(8px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        pulseSoft: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '.55' },
        },
      },
    },
  },
  plugins: [],
} satisfies Config;
