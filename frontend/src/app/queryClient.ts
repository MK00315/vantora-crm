import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 20_000,
      retry: (failureCount, error: any) => error?.response?.status && error.response.status < 500 ? false : failureCount < 2,
      refetchOnWindowFocus: false,
    },
    mutations: { retry: false },
  },
});

export function invalidateWorkspaceData(...resourceKeys: string[]) {
  const keys = [...new Set([...resourceKeys, 'dashboard', 'activities'])];
  return Promise.all(keys.map((key) => queryClient.invalidateQueries({ queryKey: [key] })));
}
