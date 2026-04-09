export interface GameSummary {
  id: number
  name: string
  yearPublished: number | null
  bggRank: number | null
  avgRating: number | null
  complexity: number | null
  minPlayers: number | null
  maxPlayers: number | null
  thumbnailUrl: string | null
}

export interface GameDetail extends GameSummary {
  description: string | null
  playTimeMin: number | null
  playTimeMax: number | null
}

export interface GamesPage {
  content: GameSummary[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface GameFilters {
  name?: string
  minPlayers?: number
  maxPlayers?: number
  minComplexity?: number
  maxComplexity?: number
  maxRank?: number
  sort?: 'rank' | 'rating' | 'complexity' | 'name'
  sortDir?: 'asc' | 'desc'
  page?: number
  size?: number
}
