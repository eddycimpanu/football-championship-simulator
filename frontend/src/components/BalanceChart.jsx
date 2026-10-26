import { useEffect, useRef, useState } from 'react';
import { agentStyle, bettorKey } from '../agents.js';
import { formatMoney } from '../betting.jsx';

const PAD = { top: 18, right: 54, bottom: 34, left: 58 };
const STEPS = [5, 10, 20, 25, 50, 100, 200, 250, 500, 1000];

// A round number of credits between the grid lines, so there are about five of them.
function niceTicks(min, max) {
  const step = STEPS.find((s) => (max - min) / s <= 6) ?? 1000;
  const first = Math.ceil(min / step) * step;
  const ticks = [];
  for (let v = first; v <= max; v += step) ticks.push(v);
  return ticks;
}

// Pushes the end labels apart so two bettors with almost the same balance don't overlap.
function spreadLabels(labels, gap, H) {
  const sorted = [...labels].sort((a, b) => a.y - b.y);
  for (let i = 1; i < sorted.length; i++) {
    if (sorted[i].y - sorted[i - 1].y < gap) sorted[i].y = sorted[i - 1].y + gap;
  }
  const overflow = sorted.at(-1).y - (H - PAD.bottom);
  if (overflow > 0) sorted.forEach((l) => { l.y -= overflow; });
  return sorted;
}

// The balance of every bettor after each round, with the 1,000 credits they started with as the baseline.
export default function BalanceChart({ bettors, roundsPlayed, startingBalance, highlight, onHighlight }) {
  const [hoverRound, setHoverRound] = useState(null);
  // The chart is drawn at its real width in pixels, so the text stays readable on a phone
  const box = useRef(null);
  const [W, setWidth] = useState(900);
  useEffect(() => {
    const observer = new ResizeObserver(([entry]) => setWidth(Math.max(280, Math.round(entry.contentRect.width))));
    observer.observe(box.current);
    return () => observer.disconnect();
  }, []);
  const H = W < 600 ? 260 : 340;

  const values = bettors.flatMap((b) => b.history.map((p) => Number(p.balance)));
  values.push(startingBalance);
  const low = Math.min(...values);
  const high = Math.max(...values);
  const pad = Math.max((high - low) * 0.08, 10);
  const yMin = low - pad;
  const yMax = high + pad;

  const plotW = W - PAD.left - PAD.right;
  const plotH = H - PAD.top - PAD.bottom;
  const x = (round) => PAD.left + (roundsPlayed === 0 ? 0 : (round / roundsPlayed) * plotW);
  const y = (balance) => PAD.top + (1 - (balance - yMin) / (yMax - yMin)) * plotH;

  const yTicks = niceTicks(yMin, yMax);
  const xStep = [1, 2, 5, 10].find((step) => (plotW / roundsPlayed) * step >= 46) ?? 10;
  const xTicks = [];
  for (let r = 0; r <= roundsPlayed; r += xStep) xTicks.push(r);

  // The highlighted bettor is drawn last, on top of the others
  const ordered = [...bettors].sort((a, b) => {
    const rank = (s) => (bettorKey(s.bettor) === highlight ? 2 : s.bettor.kind === 'HUMAN' ? 1 : 0);
    return rank(a) - rank(b);
  });

  const labels = spreadLabels(
    bettors.map((b) => ({
      key: bettorKey(b.bettor),
      y: y(Number(b.history.at(-1).balance)),
    })),
    19,
    H,
  );

  function onMove(event) {
    const box = event.currentTarget.getBoundingClientRect();
    const px = ((event.clientX - box.left) / box.width) * W;
    const round = Math.round(((px - PAD.left) / plotW) * roundsPlayed);
    setHoverRound(Math.max(0, Math.min(roundsPlayed, round)));
  }

  const tooltip = hoverRound == null ? null : bettors
    .map((b) => {
      const now = Number(b.history[hoverRound].balance);
      const before = hoverRound > 0 ? Number(b.history[hoverRound - 1].balance) : now;
      return { key: bettorKey(b.bettor), name: b.bettor.name, now, change: now - before };
    })
    .sort((a, b) => b.now - a.now);
  const tooltipLeft = hoverRound == null ? 0 : (x(hoverRound) / W) * 100;

  return (
    <div className="balance-chart" ref={box}>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        width={W}
        height={H}
        className="balance-chart__svg"
        role="img"
        aria-label={`Balance of every bettor after each of the ${roundsPlayed} rounds played`}
        onMouseMove={onMove}
        onMouseLeave={() => setHoverRound(null)}
      >
        {yTicks.map((t) => (
          <g key={t}>
            <line className="balance-chart__grid" x1={PAD.left} x2={W - PAD.right} y1={y(t)} y2={y(t)} />
            <text className="balance-chart__tick" x={PAD.left - 10} y={y(t) + 4} textAnchor="end">
              {t.toLocaleString('en-GB')}
            </text>
          </g>
        ))}
        {xTicks.map((r) => (
          <text key={r} className="balance-chart__tick" x={x(r)} y={H - 10} textAnchor="middle">
            {r === 0 ? 'Start' : `R${r}`}
          </text>
        ))}

        <line
          className="balance-chart__baseline"
          x1={PAD.left}
          x2={W - PAD.right}
          y1={y(startingBalance)}
          y2={y(startingBalance)}
        />

        {hoverRound != null && (
          <line className="balance-chart__cross" x1={x(hoverRound)} x2={x(hoverRound)} y1={PAD.top} y2={H - PAD.bottom} />
        )}

        {ordered.map((b) => {
          const key = bettorKey(b.bettor);
          const { colour, dash } = agentStyle(key);
          const dimmed = highlight && highlight !== key;
          const points = b.history.map((p) => `${x(p.round).toFixed(1)},${y(Number(p.balance)).toFixed(1)}`);
          return (
            <g key={key} className={`balance-chart__series${dimmed ? ' balance-chart__series--dim' : ''}`}>
              <polyline
                points={points.join(' ')}
                fill="none"
                stroke={colour}
                strokeWidth={key === 'HUMAN' || highlight === key ? 3 : 2}
                strokeDasharray={dash}
                strokeLinejoin="round"
                strokeLinecap="round"
              />
              {hoverRound != null && (
                <circle
                  cx={x(hoverRound)}
                  cy={y(Number(b.history[hoverRound].balance))}
                  r="4.5"
                  fill={colour}
                  className="balance-chart__dot"
                />
              )}
            </g>
          );
        })}

        {labels.map((l) => {
          const { colour, letter } = agentStyle(l.key);
          const dimmed = highlight && highlight !== l.key;
          return (
            <g
              key={l.key}
              className={`balance-chart__label${dimmed ? ' balance-chart__series--dim' : ''}`}
              transform={`translate(${W - PAD.right + 22}, ${l.y})`}
              onMouseEnter={() => onHighlight?.(l.key)}
              onMouseLeave={() => onHighlight?.(null)}
            >
              <circle r="9" fill={colour} />
              <text y="4" textAnchor="middle">{letter}</text>
            </g>
          );
        })}
      </svg>

      {tooltip && (
        <div
          className={`chart-tip${tooltipLeft > 60 ? ' chart-tip--left' : ''}`}
          style={{ left: `${tooltipLeft}%` }}
        >
          <div className="chart-tip__title">{hoverRound === 0 ? 'Start of the season' : `After round ${hoverRound}`}</div>
          <ul>
            {tooltip.map((t) => (
              <li key={t.key}>
                <span className="chart-tip__dot" style={{ background: agentStyle(t.key).colour }} />
                <span className="chart-tip__name">{t.name}</span>
                <span className="chart-tip__value">{formatMoney(t.now)}</span>
                {hoverRound > 0 && (
                  <span className={`chart-tip__change ${t.change > 0 ? 'up' : t.change < 0 ? 'down' : ''}`}>
                    {t.change > 0 ? '+' : ''}{t.change === 0 ? '·' : formatMoney(t.change)}
                  </span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
