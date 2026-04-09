import { render } from '@testing-library/react'
import { ComplexityStars } from './ComplexityStars'

test('renders 5 stars with correct filled count', () => {
  render(<ComplexityStars complexity={3.5} />)
  const filled = document.querySelectorAll('[data-testid="star-filled"]')
  const empty = document.querySelectorAll('[data-testid="star-empty"]')
  expect(filled).toHaveLength(4) // Math.round(3.5) = 4
  expect(empty).toHaveLength(1)
})

test('renders nothing for null complexity', () => {
  const { container } = render(<ComplexityStars complexity={null} />)
  expect(container).toBeEmptyDOMElement()
})
