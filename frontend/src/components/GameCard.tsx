import type { GameSummary } from '../types/game'

interface Props {
  game: GameSummary
}

export function GameCard({ game }: Props) {
  return (
    <div className="flex gap-4 p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors">
      {game.thumbnailUrl && (
        <img
          src={game.thumbnailUrl}
          alt={game.name}
          className="w-16 h-20 object-cover rounded"
        />
      )}
      <div className="flex-1 flex flex-col justify-between">
        <div>
          <h3 className="font-semibold text-sm">{game.name}</h3>
          {game.yearPublished && (
            <p className="text-xs text-gray-500">Published {game.yearPublished}</p>
          )}
        </div>
        <div className="flex gap-4 text-xs text-gray-600">
          {game.bggRank && <span>Rank: #{game.bggRank}</span>}
          {game.avgRating != null && <span>Rating: {game.avgRating.toFixed(1)}</span>}
          {game.complexity != null && <span>Complexity: {game.complexity.toFixed(1)}</span>}
        </div>
      </div>
    </div>
  )
}
