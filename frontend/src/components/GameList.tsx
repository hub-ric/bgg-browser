import type { GamesPage } from '../types/game'
import { GameCard } from './GameCard'
import { Pagination } from './Pagination'

interface Props {
  data: GamesPage | undefined
  isLoading: boolean
  isError: boolean
  page: number
  onPageChange: (page: number) => void
}

export function GameList({ data, isLoading, isError, page, onPageChange }: Props) {
  if (isLoading) {
    return (
      <div className="flex flex-col gap-3 p-4">
        {Array.from({ length: 8 }, (_, i) => (
          <div key={i} className="h-20 bg-gray-100 rounded-lg animate-pulse" />
        ))}
      </div>
    )
  }

  if (isError) {
    return <p className="p-8 text-center text-red-500">Failed to load games. Please try again.</p>
  }

  if (!data || data.content.length === 0) {
    return <p className="p-8 text-center text-gray-500">No games match your filters.</p>
  }

  return (
    <div className="flex flex-col">
      <p className="px-4 pt-4 pb-2 text-xs text-gray-500">
        {data.totalElements} game{data.totalElements !== 1 ? 's' : ''} found
      </p>
      <div className="flex flex-col gap-2 px-4">
        {data.content.map(game => <GameCard key={game.id} game={game} />)}
      </div>
      <Pagination page={page} totalPages={data.totalPages} onPageChange={onPageChange} />
    </div>
  )
}
