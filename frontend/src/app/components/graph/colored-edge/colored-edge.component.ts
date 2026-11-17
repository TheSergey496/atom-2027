import { Component, computed, input, OnDestroy, signal } from '@angular/core';
import { Edge, NgDiagramBaseEdgeComponent, NgDiagramBaseEdgeLabelComponent, NgDiagramEdgeTemplate } from 'ng-diagram';

@Component({
  selector: 'app-colored-edge',
  imports: [NgDiagramBaseEdgeComponent, NgDiagramBaseEdgeLabelComponent],
  template: `
    <ng-diagram-base-edge
      class="animated-flow"
      [edge]="edge()"
      [stroke]="strokeColor()"
      [strokeWidth]="2"
    >
      <ng-diagram-base-edge-label [id]="'flow-marker'" [positionOnEdge]="markerPosition()">
        <div class="flow-marker" [style.background-color]="strokeColor()"></div>
      </ng-diagram-base-edge-label>
    </ng-diagram-base-edge>
  `,
  styles: `
    .flow-marker {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
  `,
})
export class ColoredEdgeComponent implements NgDiagramEdgeTemplate, OnDestroy {
  edge = input.required<Edge<any>>();
  strokeColor = computed(() => {
    if (this.edge().data?.hasWater && this.edge().data?.hasEnergy) {
      return '#000000'
    } else if (this.edge().data?.hasWater) {
      return '#3b82f6';
    } else {
      return '#f59e0b';
    }
  }
  );

  private readonly SPEED_PX_PER_TICK = 3;
  private readonly VIRTUAL_LENGTH = 2000;

  intervalId: any;
  private currentPx = 0;
  markerPosition = signal(0);

  constructor() {
    // Запускаем анимацию: каждые 50мс сдвигаем маркер
    this.intervalId = setInterval(() => {
      this.markerPosition.update(pos => (pos >= 1 ? 0 : pos + 0.02));
    }, 50);
  }

  ngOnDestroy() {
    clearInterval(this.intervalId);
  }
}
