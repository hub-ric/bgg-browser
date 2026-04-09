import type { GameFilters } from '../types/game'

interface Props {
  filters: GameFilters
  onChange: (filters: GameFilters) => void
  isOpen: boolean
  onClose: () => void
}

export function FilterSidebar({ filters, onChange, isOpen, onClose }: Props) {
  const update = (patch: Partial<GameFilters>) => onChange({ ...filters, ...patch, page: 0 })

  const content = (
    <div className="flex flex-col gap-4 p-4">
      <h2 className="font-bold text-gray-800 text-sm uppercase tracking-wide">Filters</h2>

      <div>
        <label className="block text-xs text-gray-500 mb-1">Name</label>
        <input
          type="text"
          value={filters.name ?? ''}
          onChange={e => update({ name: e.target.value || undefined })}
          placeholder="Search games…"
          className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        />
      </div>

      <div>
        <label className="block text-xs text-gray-500 mb-1">Players</label>
        <div className="flex gap-2 items-center">
          <input type="number" min={1} max={10}
            value={filters.minPlayers ?? ''}
            onChange={e => update({ minPlayers: e.target.value ? +e.target.value : undefined })}
            placeholder="Min"
            className="w-16 border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <span className="text-gray-400">–</span>
          <input type="number" min={1} max={10}
            value={filters.maxPlayers ?? ''}
            onChange={e => update({ maxPlayers: e.target.value ? +e.target.value : undefined })}
            placeholder="Max"
            className="w-16 border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
        </div>
      </div>

      <div>
        <label className="block text-xs text-gray-500 mb-1">
          Complexity ({filters.minComplexity ?? 1}–{filters.maxComplexity ?? 5})
        </label>
        <div className="flex gap-2 items-center">
          <input type="range" min={1} max={5} step={0.5}
            value={filters.minComplexity ?? 1}
            onChange={e => update({ minComplexity: +e.target.value })}
            className="flex-1"
          />
          <input type="range" min={1} max={5} step={0.5}
            value={filters.maxComplexity ?? 5}
            onChange={e => update({ maxComplexity: +e.target.value })}
            className="flex-1"
          />
        </div>
      </div>

      <div>
        <label className="block text-xs text-gray-500 mb-1">Max Rank</label>
        <select
          value={filters.maxRank ?? ''}
          onChange={e => update({ maxRank: e.target.value ? +e.target.value : undefined })}
          className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        >
          <option value="">All ranks</option>
          <option value="100">Top 100</option>
          <option value="500">Top 500</option>
          <option value="1000">Top 1000</option>
          <option value="5000">Top 5000</option>
        </select>
      </div>

      <div>
        <label className="block text-xs text-gray-500 mb-1">Sort by</label>
        <select
          value={`${filters.sort ?? 'rank'}-${filters.sortDir ?? 'asc'}`}
          onChange={e => {
            const [sort, sortDir] = e.target.value.split('-') as [GameFilters['sort'], GameFilters['sortDir']]
            update({ sort, sortDir })
          }}
          className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
        >
          <option value="rank-asc">Rank (best first)</option>
          <option value="rating-desc">Score (highest first)</option>
          <option value="complexity-asc">Complexity (lightest first)</option>
          <option value="complexity-desc">Complexity (heaviest first)</option>
          <option value="name-asc">Name (A–Z)</option>
        </select>
      </div>

      <button
        onClick={() => onChange({ page: 0, size: 20 })}
        className="text-sm text-blue-600 hover:underline text-left"
      >
        Reset filters
      </button>
    </div>
  )

  return (
    <>
      {/* Desktop sidebar */}
      <aside className="hidden md:block w-56 flex-shrink-0 border-r border-gray-200 bg-gray-50 min-h-screen">
        {content}
      </aside>

      {/* Mobile drawer */}
      {isOpen && (
        <div className="fixed inset-0 z-40 flex md:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={onClose} />
          <div className="relative w-72 bg-white shadow-xl overflow-y-auto">
            <button
              onClick={onClose}
              className="absolute top-3 right-3 text-gray-500 hover:text-gray-900 text-xl"
            >✕</button>
            {content}
          </div>
        </div>
      )}
    </>
  )
}
