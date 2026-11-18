import {
  Component,
  computed,
  inject,
  input,
  model,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { BrigadeService } from '../../../gen/atom2027-controllers';
import { lastValueFrom } from 'rxjs';
import { BrigadeEventDto } from '../../../gen/atom2027-dto';

const TIME_FORMATTER = new Intl.DateTimeFormat('ru-RU', {
  hour: '2-digit',
  minute: '2-digit',
});

/** Шаги сетки в миллисекундах. */
const TICK_STEPS = [
  60_000,
  2 * 60_000,
  5 * 60_000,
  10 * 60_000,
  15 * 60_000,
  30 * 60_000,
  3_600_000,
  2 * 3_600_000,
  3 * 3_600_000,
  6 * 3_600_000,
  12 * 3_600_000,
  24 * 3_600_000,
];

const MAX_TICKS = 8;

// brigade-waterfall.component.ts

function parseDate(value: unknown): number {
  if (value == null) return NaN;

  // Already a Date
  if (value instanceof Date) return value.getTime();

  // Numeric epoch (ms or seconds)
  if (typeof value === 'number') {
    // Heuristic: values < 1e12 are almost certainly seconds
    return value < 1e12 ? value * 1000 : value;
  }

  // String — trim microseconds, then parse
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) return NaN;

    // ISO without timezone → treat as local time (browser already does this for "YYYY-MM-DDTHH:mm:ss")
    // ISO with microseconds → strip to ms first: "…01.314445" -> "…01.314"
    const normalized = trimmed.replace(/(\.\d{3})\d+/, '$1');

    const ms = Date.parse(normalized);
    if (Number.isFinite(ms)) return ms;

    // Last resort: try original string
    const fallback = Date.parse(trimmed);
    return Number.isFinite(fallback) ? fallback : NaN;
  }

  return NaN;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function formatDuration(ms: number): string {
  const totalMinutes = Math.round(ms / 60_000);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (hours > 0 && minutes > 0) return `${hours} ч ${minutes} мин`;
  if (hours > 0) return `${hours} ч`;
  if (minutes > 0) return `${minutes} мин`;
  return `${Math.max(1, Math.round(ms / 1000))} с`;
}

@Component({
  imports: [],
  selector: 'pro-legacy-brigade-events',
  styleUrl: './brigade-events.component.css',
  templateUrl: './brigade-events.component.html',
})
export class BrigadeEventsComponent implements OnInit, OnDestroy {
  private readonly brigadeService = inject(BrigadeService);

  async ngOnInit() {
    const events = await lastValueFrom(this.brigadeService.getBrigadeEvents());
    console.log(events);
    this.events.set(events ?? []);
  }

  ngOnDestroy(): void {}

  readonly events = model<readonly BrigadeEventDto[]>([]);

  private readonly hovered = signal<{ bar: WaterfallBar; x: number; y: number } | null>(null);

  /** Общий временной диапазон диаграммы. */
  readonly range = computed(() => {
    const list = this.events();
    if (!list.length) {
      return null;
    }

    let start = Number.POSITIVE_INFINITY;
    let end = Number.NEGATIVE_INFINITY;

    for (const item of list) {
      const s = parseDate(item.startDate);
      const e = parseDate(item.expectedEndTime);
      if (Number.isFinite(s)) start = Math.min(start, s);
      if (Number.isFinite(e)) end = Math.max(end, e);
    }

    if (!Number.isFinite(start) || !Number.isFinite(end)) {
      return null;
    }
    if (end <= start) {
      end = start + 60_000;
    }

    return { start, end, span: end - start };
  });

  /** Дорожки бригад сверху вниз. */
  readonly lanes = computed<WaterfallLane[]>(() => {
    const range = this.range();
    if (!range) {
      return [];
    }

    const lanes = new Map<number, WaterfallLane>();

    for (const item of this.events()) {
      const rawStart = parseDate(item.startDate);
      if (!Number.isFinite(rawStart)) {
        continue;
      }

      const rawEnd = parseDate(item.expectedEndTime);
      const end = Number.isFinite(rawEnd) && rawEnd > rawStart ? rawEnd : rawStart + 60_000;

      const brigadeId = item.brigade?.id ?? -1;
      let lane = lanes.get(brigadeId);
      if (!lane) {
        lane = {
          brigadeId,
          brigadeName: item.brigade?.name ?? 'Без бригады',
          bars: [],
        };
        lanes.set(brigadeId, lane);
      }

      const rawType = item.event?.resourceType;
      const isWater = rawType === 'water' || (rawType == null && item.edifice?.hasWater === true);
      const resourceType: ResourceType = isWater ? 'water' : 'energy';

      const left = clamp(((rawStart - range.start) / range.span) * 100, 0, 99.4);
      const width = clamp(((end - rawStart) / range.span) * 100, 0.6, 100 - left);

      const startLabel = TIME_FORMATTER.format(new Date(rawStart));
      const endLabel = TIME_FORMATTER.format(new Date(end));
      const durationLabel = formatDuration(end - rawStart);
      const edificeName = item.edifice?.name ?? `Объект #${item.event?.edificeId ?? '—'}`;

      lane.bars.push({
        id: item.id ?? 0,
        orderNumber: item.orderNumber ?? 1,
        left,
        width,
        startMs: rawStart,
        endMs: end,
        resourceType,
        startLabel,
        endLabel,
        durationLabel,
        ariaLabel: `${item.brigade?.name ?? 'Бригада'}: ${edificeName}, ${startLabel} — ${endLabel}, ${durationLabel}`,
        tooltip: {
          eventId: item.event?.id ?? -1,
          edificeName,
          district: item.edifice?.district ?? '—',
          street: item.edifice?.street ?? '—',
          houseNumber: item.edifice?.houseNumber ?? '—',
          criticalIndex: item.edifice?.criticalIndex ?? null,
        },
      });
    }

    return [...lanes.values()]
      .map(lane => ({
        ...lane,
        bars: [...lane.bars].sort((a, b) => a.startMs - b.startMs || a.orderNumber - b.orderNumber),
      }))
      .sort((a, b) => a.brigadeId - b.brigadeId);
  });

  /** Деления шкалы времени. */
  readonly ticks = computed<WaterfallTick[]>(() => {
    const range = this.range();
    if (!range) {
      return [];
    }

    const step =
      TICK_STEPS.find(s => range.span / s <= MAX_TICKS) ?? TICK_STEPS[TICK_STEPS.length - 1];
    const first = Math.ceil(range.start / step) * step;

    const ticks: WaterfallTick[] = [];
    for (let time = first; time <= range.end; time += step) {
      ticks.push({
        time,
        left: ((time - range.start) / range.span) * 100,
        label: TIME_FORMATTER.format(new Date(time)),
      });
    }
    return ticks;
  });

  readonly brigadeCount = computed(() => this.lanes().length);
  readonly totalEvents = computed(() => this.events().length);

  readonly tooltipStyle = computed<Record<string, string>>((() => {
    const hover = this.hovered();
    if (!hover) {
      return {};
    }

    const offset = 16;
    const boxWidth = 300;
    const boxHeight = 180;
    const vw = typeof window !== 'undefined' ? window.innerWidth : 1920;
    const vh = typeof window !== 'undefined' ? window.innerHeight : 1080;

    return {
      left: `${Math.min(hover.x + offset, Math.max(8, vw - boxWidth - 8))}px`,
      top: `${Math.min(hover.y + offset, Math.max(8, vh - boxHeight - 8))}px`,
    };
  }) as unknown as any);

  onBarHover(event: MouseEvent, bar: WaterfallBar): void {
    this.hovered.set({ bar, x: event.clientX, y: event.clientY });
  }

  onBarFocus(event: FocusEvent, bar: WaterfallBar): void {
    const rect = (event.target as HTMLElement).getBoundingClientRect();
    this.hovered.set({ bar, x: rect.left + rect.width / 2, y: rect.bottom });
  }

  onBarLeave(): void {
    this.hovered.set(null);
  }

  protected readonly hoveredBar = computed(() => this.hovered()?.bar ?? null);
}

// brigade-waterfall.models.ts
export type ResourceType = 'energy' | 'water';

export interface Brigade {
  id: number;
  name: string;
  events?: unknown;
}

export interface Edifice {
  id: number;
  name: string;
  hasWater?: boolean | null;
  hasEnergy?: boolean | null;
  district?: string | null;
  street?: string | null;
  houseNumber?: string | null;
  criticalIndex?: number | null;
}

export interface EventInfo {
  id: number;
  edificeId?: number | null;
  resourceType?: ResourceType | null;
  occurredAt?: string | null;
  eventStatusId?: number | null;
}

/** Полоса события, готовая к отрисовке. */
export interface WaterfallBar {
  id: number;
  orderNumber: number;
  /** Позиция в процентах от начала шкалы. */
  left: number;
  /** Ширина в процентах от длины шкалы. */
  width: number;
  startMs: number;
  endMs: number;
  resourceType: ResourceType;
  startLabel: string;
  endLabel: string;
  durationLabel: string;
  ariaLabel: string;
  tooltip: {
    eventId: number;
    edificeName: string;
    district: string;
    street: string;
    houseNumber: string;
    criticalIndex: number | null;
  };
}

/** Горизонтальная дорожка бригады. */
export interface WaterfallLane {
  brigadeId: number;
  brigadeName: string;
  bars: WaterfallBar[];
}

export interface WaterfallTick {
  time: number;
  left: number;
  label: string;
}
