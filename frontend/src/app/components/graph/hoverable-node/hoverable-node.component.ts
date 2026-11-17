import { Component, computed, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { map } from 'rxjs/operators';
import { NgDiagramNodeSelectedDirective, NgDiagramNodeTemplate, Node } from 'ng-diagram';
import { EdificeDto, EventDto } from '../../../gen/atom2027-dto';
import { Tooltip, TooltipModule } from '@openng/optimus-ui/tooltip';

type StatusKind = 'accident' | 'repair' | 'normal';
type ResourceKind = 'energy' | 'water';

interface ResourceVm {
  kind: ResourceKind;
  label: string;
  status: StatusKind;
  statusLabel: string;
  durationLabel: string | null;
  tooltip: string;
}

interface EdificeVm {
  title: string;
  subtitle: string;
  criticalIndex: number | null;
  accidentTooltip: string | undefined;
  status: StatusKind;
  resources: ResourceVm[];
}

const STATUS_LABEL: Record<StatusKind, string> = {
  accident: 'Активная авария',
  repair: 'Ремонтные работы',
  normal: 'Норма',
};

const STATUS_SEVERITY: Record<StatusKind, number> = {
  accident: 2,
  repair: 1,
  normal: 0,
};

const RESOURCE_LABEL: Record<ResourceKind, string> = {
  energy: 'Энергия',
  water: 'Вода',
};

@Component({
  selector: 'pro-legacy-hoverable-node',
  hostDirectives: [{ directive: NgDiagramNodeSelectedDirective, inputs: ['node'] }],
  template: `
    <div
      class="node"
      [attr.data-status]="vm().status"
      [class.dimmed]="node().data.dimmed"
      [class.highlighted]="node().data.highlighted"
      (mouseenter)="hoverEnter.emit(node())"
      (mouseleave)="hoverLeave.emit(node())"
    >
      <span class="accent-bar" aria-hidden="true"></span>

      <div class="body" [pTooltip]="vm().accidentTooltip">
        <div class="header">
          @if (vm().criticalIndex !== null) {
            <span class="critical-index" [title]="'Критический индекс: ' + vm().criticalIndex">
              {{ vm().criticalIndex }}
            </span>
          }
          <span class="title" [title]="vm().title">{{ vm().title }}</span>
        </div>

        @if (vm().subtitle) {
          <div class="subtitle" [title]="vm().subtitle">{{ vm().subtitle }}</div>
        }

        @if (vm().resources.length) {
          <div class="resources">
            @for (resource of vm().resources; track resource.kind) {
              <div
                class="resource"
                [attr.data-kind]="resource.kind"
                [attr.data-status]="resource.status"
                [title]="resource.tooltip"
              >
                <span class="icon" aria-hidden="true">
                  @if (resource.kind === 'energy') {
                    <svg viewBox="0 0 24 24" fill="currentColor">
                      <path d="M13.5 2 4 13.2h6.1L9.6 22 20 10.3h-6.4L13.5 2Z" />
                    </svg>
                  } @else {
                    <svg viewBox="0 0 24 24" fill="currentColor">
                      <path
                        d="M12 2.2c3.6 4 6.3 7.4 6.3 10.6a6.3 6.3 0 1 1-12.6 0C5.7 9.6 8.4 6.2 12 2.2Z"
                      />
                    </svg>
                  }
                </span>
                <span class="resource-name">{{ resource.label }}</span>
                <span class="resource-status">{{ resource.statusLabel }}</span>
                @if (resource.durationLabel) {
                  <span class="resource-duration">· {{ resource.durationLabel }}</span>
                }
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .node {
        --accent: #16a34a;
        --accent-soft: #f5fdf8;
        --accent-border: #bbf7d0;
        --accent-ring: rgba(22, 163, 74, 0.35);

        position: relative;
        display: flex;
        align-items: stretch;
        min-width: 200px;
        max-width: 280px;
        overflow: hidden;
        border-radius: 8px;
        border: 1.5px solid var(--accent-border);
        background: var(--accent-soft);
        box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
        cursor: pointer;
        user-select: none;
        transition:
          border-color 120ms ease,
          box-shadow 120ms ease,
          transform 120ms ease,
          opacity 120ms ease;
      }

      .node[data-status='repair'] {
        --accent: #f59e0b;
        --accent-soft: #fffaef;
        --accent-border: #fde68a;
        --accent-ring: rgba(245, 158, 11, 0.35);
      }

      .node[data-status='accident'] {
        --accent: #dc2626;
        --accent-soft: #fff6f6;
        --accent-border: #fecaca;
        --accent-ring: rgba(220, 38, 38, 0.35);
      }

      .node:hover {
        border-color: var(--accent);
        box-shadow: 0 6px 18px -6px rgba(15, 23, 42, 0.25);
        transform: translateY(-1px);
      }

      .node.highlighted {
        box-shadow:
          0 0 0 3px var(--accent-ring),
          0 1px 2px rgba(15, 23, 42, 0.06);
      }

      .node.dimmed {
        opacity: 0.25;
      }

      .accent-bar {
        flex: 0 0 4px;
        background: var(--accent);
      }

      .body {
        display: flex;
        flex: 1;
        flex-direction: column;
        gap: 4px;
        min-width: 0;
        padding: 8px 10px 9px;
      }

      .header {
        display: flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
      }

      .critical-index {
        display: inline-flex;
        flex: none;
        align-items: center;
        justify-content: center;
        min-width: 20px;
        height: 20px;
        padding: 0 5px;
        border-radius: 6px;
        background: rgba(15, 23, 42, 0.07);
        color: #334155;
        font-size: 11px;
        font-weight: 700;
        font-variant-numeric: tabular-nums;
        line-height: 1;
      }

      .title {
        flex: 1 1 auto;
        min-width: 0;
        overflow: hidden;
        color: #0f172a;
        font-size: 13px;
        font-weight: 600;
        line-height: 1.3;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .subtitle {
        overflow: hidden;
        color: #64748b;
        font-size: 11px;
        line-height: 1.3;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .resources {
        display: flex;
        flex-direction: column;
        gap: 3px;
        margin-top: 2px;
        padding-top: 6px;
        border-top: 1px dashed rgba(15, 23, 42, 0.1);
      }

      .resource {
        --icon-color: #64748b;
        --status-color: #475569;

        display: flex;
        align-items: center;
        gap: 5px;
        min-width: 0;
        font-size: 11px;
        line-height: 1.4;
      }

      .resource[data-kind='energy'] {
        --icon-color: #f59e0b;
      }

      .resource[data-kind='water'] {
        --icon-color: #3b82f6;
      }

      .resource[data-status='accident'] {
        --status-color: #dc2626;
      }

      .resource[data-status='repair'] {
        --status-color: #b45309;
      }

      .resource[data-status='normal'] {
        --status-color: #15803d;
      }

      .resource .icon {
        display: inline-flex;
        flex: none;
        width: 13px;
        height: 13px;
        color: var(--icon-color);
      }

      .resource .icon svg {
        display: block;
        width: 100%;
        height: 100%;
      }

      .resource-name {
        flex: 1 1 auto;
        min-width: 0;
        overflow: hidden;
        color: #64748b;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .resource-status {
        flex: none;
        color: var(--status-color);
        font-weight: 600;
        white-space: nowrap;
      }

      .resource-duration {
        flex: none;
        color: var(--status-color);
        font-weight: 500;
        white-space: nowrap;
      }

      .resource[data-status='accident'] .icon {
        animation: resource-pulse 1.6s ease-in-out infinite;
      }

      @keyframes resource-pulse {
        0%,
        100% {
          opacity: 1;
        }
        50% {
          opacity: 0.45;
        }
      }

      @media (prefers-reduced-motion: reduce) {
        .node {
          transition: none;
        }

        .resource[data-status='accident'] .icon {
          animation: none;
        }
      }
    `,
  ],
  imports: [TooltipModule],
})
export class HoverableNodeComponent implements NgDiagramNodeTemplate {
  node = input.required<Node<EdificeNodeData>>();
  hoverEnter = output<Node<EdificeNodeData>>();
  hoverLeave = output<Node<EdificeNodeData>>();

  // Re-emits every 30s so accident durations stay fresh.
  private readonly now = toSignal(interval(30_000).pipe(map(() => Date.now())), {
    initialValue: Date.now(),
  });

  readonly vm = computed<EdificeVm>(() => this.buildVm(this.node().data, this.now()));

  private buildVm(data: EdificeNodeData, now: number): EdificeVm {
    const resources = [
      this.buildResource(
        'energy',
        data.hasEnergy !== false || !!data.energyActiveEvent,
        data.energyActiveEvent,
        now,
      ),
      this.buildResource(
        'water',
        data.hasWater !== false || !!data.waterActiveEvent,
        data.waterActiveEvent,
        now,
      ),
    ].filter((r): r is ResourceVm => r !== null);

    const status = resources.reduce<StatusKind>(
      (worst, r) => (STATUS_SEVERITY[r.status] > STATUS_SEVERITY[worst] ? r.status : worst),
      'normal',
    );

    const address = [data.street, data.houseNumber].filter(Boolean).join(', ');
    const name = data.name?.trim();
    const title = name || address || `Здание #${data.id ?? '—'}`;

    const subtitleParts: string[] = [];
    if (name && address) subtitleParts.push(address);
    if (data.district) subtitleParts.push(data.district);

    return {
      title,
      subtitle: subtitleParts.join(' · '),
      criticalIndex: data.criticalIndex ?? null,
      accidentTooltip: data.accidentTooltip ?? undefined,
      status,
      resources,
    };
  }

  private buildResource(
    kind: ResourceKind,
    enabled: boolean,
    event: EventDto | undefined,
    now: number,
  ): ResourceVm | null {
    if (!enabled) return null;

    const status = this.resolveStatus(event);
    const statusLabel = STATUS_LABEL[status];
    const occurredAt = this.formatDate(event?.occurredAt);

    // Only show duration for active accidents.
    const durationLabel =
      status === 'accident' ? this.formatDuration(event?.occurredAt, now) : null;

    const tooltip = [
      `${RESOURCE_LABEL[kind]}: ${statusLabel}${durationLabel ? ` · ${durationLabel}` : ''}`,
      occurredAt ? `с ${occurredAt}` : null,
    ]
      .filter(Boolean)
      .join(' · ');

    return {
      kind,
      label: RESOURCE_LABEL[kind],
      status,
      statusLabel,
      durationLabel,
      tooltip,
    };
  }

  private resolveStatus(event: EventDto | undefined): StatusKind {
    const name = event?.eventStatus?.name?.trim().toLowerCase() ?? '';
    if (name.includes('авари')) return 'accident';
    if (name.includes('ремонт')) return 'repair';
    return 'normal';
  }

  private formatDate(value: Date | string | undefined): string | null {
    if (!value) return null;
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return null;
    return date.toLocaleString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private formatDuration(value: Date | string | undefined, now: number): string | null {
    if (!value) return null;
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return null;

    const ms = now - date.getTime();
    if (ms < 0) return null;

    const totalMinutes = Math.floor(ms / 60_000);
    if (totalMinutes < 1) return '< 1 мин';

    const days = Math.floor(totalMinutes / 1440);
    const hours = Math.floor((totalMinutes % 1440) / 60);
    const minutes = totalMinutes % 60;

    if (days > 0) return `${days} д ${hours} ч`;
    if (hours > 0) return `${hours} ч ${minutes} мин`;
    return `${minutes} мин`;
  }
}

interface EdificeNodeData extends EdificeDto {
  highlighted?: boolean;
  dimmed?: boolean;
}
