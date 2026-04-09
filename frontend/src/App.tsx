import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowsePage } from './pages/BrowsePage'
import { GameDetailPage } from './pages/GameDetailPage'

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 5 * 60 * 1000 } },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<BrowsePage />} />
          <Route path="/games/:id" element={<GameDetailPage />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
