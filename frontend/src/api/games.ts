import type { GameDetail, GameFilters, GamesPage } from '../types/game'

const BASE = '/api/games'

export async function fetchGames(filters: GameFilters): Promise<GamesPage> {
  const params = new URLSearchParams()
  if (filters.name) params.set('name', filters.name)
  if (filters.minPlayers != null) params.set('minPlayers', String(filters.minPlayers))
  if (filters.maxPlayers != null) params.set('maxPlayers', String(filters.maxPlayers))
  if (filters.minComplexity != null) params.set('minComplexity', String(filters.minComplexity))
  if (filters.maxComplexity != null) params.set('maxComplexity', String(filters.maxComplexity))
  if (filters.maxRank != null) params.set('maxRank', String(filters.maxRank))
  if (filters.sort) params.set('sort', filters.sort)
  if (filters.sortDir) params.set('sortDir', filters.sortDir)
  if (filters.page != null) params.set('page', String(filters.page))
  if (filters.size != null) params.set('size', String(filters.size))

  const res = await fetch(`${BASE}?${params}`)
  if (!res.ok) throw new Error(`Failed to fetch games: ${res.status}`)
  return res.json()
}

export async function fetchGame(id: number): Promise<GameDetail> {
  const res = await fetch(`${BASE}/${id}`)
  if (!res.ok) throw new Error(`Game not found: ${id}`)
  return res.json()
}
