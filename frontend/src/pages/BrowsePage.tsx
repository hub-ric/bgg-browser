import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { GameFilters } from '../types/game'
import { FilterSidebar } from '../components/FilterSidebar'
import { GameList } from '../components/GameList'
import { useGames } from '../hooks/useGames'

function paramsToFilters(params: URLSearchParams): GameFilters {
  return {
    name: params.get('name') ?? undefined,
    minPlayers: params.has('minPlayers') ? +params.get('minPlayers')! : undefined,
    maxPlayers: params.has('maxPlayers') ? +params.get('maxPlayers')! : undefined,
    minComplexity: params.has('minComplexity') ? +params.get('minComplexity')! : undefined,
    maxComplexity: params.has('maxComplexity') ? +params.get('maxComplexity')! : undefined,
    maxRank: params.has('maxRank') ? +params.get('maxRank')! : undefined,
    sort: (params.get('sort') as GameFilters['sort']) ?? 'rank',
    sortDir: (params.get('sortDir') as GameFilters['sortDir']) ?? 'asc',
    page: params.has('page') ? +params.get('page')! : 0,
    size: 20,
  }
}

function filtersToParams(filters: GameFilters): Record<string, string> {
  const p: Record<string, string> = {}
  if (filters.name) p.name = filters.name
  if (filters.minPlayers != null) p.minPlayers = String(filters.minPlayers)
  if (filters.maxPlayers != null) p.maxPlayers = String(filters.maxPlayers)
  if (filters.minComplexity != null) p.minComplexity = String(filters.minComplexity)
  if (filters.maxComplexity != null) p.maxComplexity = String(filters.maxComplexity)
  if (filters.maxRank != null) p.maxRank = String(filters.maxRank)
  if (filters.sort && filters.sort !== 'rank') p.sort = filters.sort
  if (filters.sortDir && filters.sortDir !== 'asc') p.sortDir = filters.sortDir
  if (filters.page && filters.page > 0) p.page = String(filters.page)
  return p
}

export function BrowsePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const filters = paramsToFilters(searchParams)
  const { data, isLoading, isError } = useGames(filters)

  const handleFiltersChange = (newFilters: GameFilters) => {
    setSearchParams(filtersToParams(newFilters))
  }

  return (
    <div className="flex min-h-screen">
      <FilterSidebar
        filters={filters}
        onChange={handleFiltersChange}
        isOpen={drawerOpen}
        onClose={() => setDrawerOpen(false)}
      />
      <main className="flex-1">
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
          <h1 className="font-bold text-gray-900">BGG Browser</h1>
          <button
            onClick={() => setDrawerOpen(true)}
            className="md:hidden text-sm px-3 py-1.5 border border-gray-300 rounded"
          >Filters</button>
        </div>
        <GameList
          data={data}
          isLoading={isLoading}
          isError={isError}
          page={filters.page ?? 0}
          onPageChange={page => handleFiltersChange({ ...filters, page })}
        />
      </main>
    </div>
  )
}
