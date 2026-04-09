import { useQuery } from '@tanstack/react-query'
import { fetchGames } from '../api/games'
import type { GameFilters } from '../types/game'

export function useGames(filters: GameFilters) {
  return useQuery({
    queryKey: ['games', filters],
    queryFn: () => fetchGames(filters),
    placeholderData: prev => prev,
  })
}
