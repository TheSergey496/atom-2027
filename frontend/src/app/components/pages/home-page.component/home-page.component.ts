import {
  Component,
  computed,
  inject,
  Injector,
  OnDestroy,
  OnInit,
  runInInjectionContext,
  signal,
  Signal,
  viewChild,
} from '@angular/core';
import {
  initializeModel,
  LinkingConfig,
  Model,
  NgDiagramComponent,
  NgDiagramConfig,
  NgDiagramMinimapComponent,
  NgDiagramModelService,
  NgDiagramViewportService,
  Node,
  provideNgDiagram,
  SelectionChangedEvent,
} from 'ng-diagram';
import ELK from 'elkjs/lib/elk.bundled.js';
import { ColoredEdgeComponent } from '../../graph/colored-edge/colored-edge.component';
import { GlobalLoadingService, LocalLoadingService } from '../../../services/loading.service';
import { SplitterModule } from '@openng/optimus-ui/splitter';
import { EdificeDependencyService, EdificeService } from '../../../gen/atom2027-controllers';
import { EdificeDependencyDto, EdificeDto } from '../../../gen/atom2027-dto';
import { lastValueFrom, Subscription } from 'rxjs';
import { HoverableNodeComponent } from '../../graph/hoverable-node/hoverable-node.component';
import { GraphSaveStateService } from '../../../services/graph/save.service';
import { EventsComponent } from '../events/events.component';
import { ResetService } from '../../../services/reset.service';

@Component({
  selector: 'pro-legacy-home-page.component',
  providers: [provideNgDiagram()],
  imports: [NgDiagramComponent, SplitterModule, NgDiagramMinimapComponent, EventsComponent],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css',
})
export class HomePageComponent implements OnInit, OnDestroy {
  private readonly wrapper: Signal<any> = viewChild.required('wrapper');
  private readonly injector = inject(Injector);

  private loadingService?: LocalLoadingService;
  private globalLoadingService = inject(GlobalLoadingService);

  selectedNode: any | null = signal<any | null>(null);
  hoveredNodeId: string | null = null;

  private readonly ngDiagramModelService = inject(NgDiagramModelService);
  private readonly viewportService = inject(NgDiagramViewportService);
  private readonly elk = new ELK();
  private readonly edificeService = inject(EdificeService);
  private readonly edificeDependencyService = inject(EdificeDependencyService);

  private readonly graphSaveService = inject(GraphSaveStateService);
  private readonly resetService = inject(ResetService);

  edificies: EdificeDto[] = [];
  dependencies: EdificeDependencyDto[] = [];

  linkingConfig: Partial<LinkingConfig> = {
    validateConnection: () => false,
  };

  config: NgDiagramConfig = {
    linking: this.linkingConfig,
  };

  protected readonly edgeTemplateMap = new Map([['colored', ColoredEdgeComponent]]);
  protected readonly nodeTemplateMap = new Map([['default', HoverableNodeComponent]]);

  model = initializeModel({ nodes: [], edges: [] });

  private resetSubscription?: Subscription;

  constructor() {
    this.loadingService = new LocalLoadingService(
      computed(() => this.wrapper()?.nativeElement),
      this.injector,
    );
    this.loadingService.beginLoading();
  }

  ngOnDestroy(): void {
    this.resetSubscription?.unsubscribe();
  }

  private pinger?: number;

  async ngOnInit() {
    await this.initGraph();
    this.resetSubscription = this.resetService.subscribe(async () => {
      await this.loadingService?.withLoader(async () => {
        await this.resetData();
      });
    });
    this.loadingService?.stopLoading();
    // this.setupPinger();
  }

  private async applyLayout() {
    const model = this.ngDiagramModelService.getModel();
    const nodes = model.getNodes();
    const edges = model.getEdges();

    const graph = {
      id: 'root',
      layoutOptions: {
        'elk.algorithm': 'layered',
        'elk.direction': 'DOWN',
        'elk.spacing.nodeNode': '80',
      },
      children: nodes.map(n => ({
        id: n.id,
        width: n.size?.width ?? 160,
        height: n.size?.height ?? 60,
      })),
      edges: edges.map(e => ({
        id: e.id,
        sources: [e.source],
        targets: [e.target],
      })),
    };

    const result = await this.elk.layout(graph);
    const placed = new Map<string, any>((result.children ?? []).map((c: any) => [c.id, c]));

    await this.ngDiagramModelService.updateNodes(
      nodes.map(n => ({
        id: n.id,
        position: {
          x: placed.get(n.id)?.x ?? 0,
          y: placed.get(n.id)?.y ?? 0,
        },
      })),
      // { transaction: true },
    );
  }

  protected selectionChange(event: SelectionChangedEvent) {
    this.selectedNode.set((event?.selectedNodes ?? [])[0] ?? null);
  }

  private buildAdjacencyList(): Map<string, string[]> {
    const adj = new Map<string, string[]>();
    for (const edge of this.dependencies) {
      const source = String(edge.sourceEdificeId);
      const target = String(edge.targetEdificeId);
      if (!adj.has(source)) adj.set(source, []);
      adj.get(source)!.push(target);
    }
    return adj;
  }

  private computeDependents(startId: string): Set<string> {
    const adj = this.buildAdjacencyList();
    const visited = new Set<string>();
    const queue: string[] = [startId];

    while (queue.length > 0) {
      const current = queue.shift()!;
      if (visited.has(current)) continue;
      visited.add(current);

      const neighbors = adj.get(current) ?? [];
      for (const neighbor of neighbors) {
        if (!visited.has(neighbor)) {
          queue.push(neighbor);
        }
      }
    }
    visited.delete(startId);
    return visited;
  }

  onNodeHoverEnter(node: Node) {
    this.hoveredNodeId = node.id;
    const dependents = this.computeDependents(node.id);
    this.updateHighlight(dependents).then();
  }

  onNodeHoverLeave() {
    this.hoveredNodeId = null;
    this.updateHighlight(new Set()).then(); // clear all highlights
  }

  private async updateHighlight(dependentIds: Set<string>) {
    const nodes = this.ngDiagramModelService.getModel().getNodes();
    const updates = nodes.map(n => ({
      id: n.id,
      data: {
        ...n.data,
        highlighted: dependentIds.has(n.id),
      },
    }));
    await this.ngDiagramModelService.updateNodes(updates);
  }

  public saveGraphState() {
    this.graphSaveService.save(this.ngDiagramModelService.toJSON());
  }

  public async resetData() {
    clearInterval(this.pinger);
    await this.globalLoadingService.withLoader(async () => {
      const nodeIds = this.ngDiagramModelService.nodes().map(node => node.id);
      if (nodeIds.length > 0) {
        await this.ngDiagramModelService.deleteNodes(nodeIds);
      }
      await lastValueFrom(this.edificeService.resetData());
      await this.initGraph();
      // this.setupPinger();
    });
  }

  private async initGraph() {
    this.edificies = await lastValueFrom(
      this.edificeService.getEdifices(<any>new Date().toISOString()),
    );
    this.dependencies = await lastValueFrom(this.edificeDependencyService.getEdificeDependencies());

    await this.ngDiagramModelService.addNodes(
      this.edificies.map(e => ({
        id: String(e.id),
        position: { x: 0, y: 0 }, // временная позиция, ELK перезапишет
        data: e,
        type: 'default',
        groupId: e.district,
      })),
      { waitForMeasurements: true },
    );
    await this.ngDiagramModelService.addEdges(
      this.dependencies.map(d => ({
        id: String(d.id),
        source: String(d.sourceEdificeId),
        target: String(d.targetEdificeId),
        type: 'colored',
        data: { hasWater: d.hasWater, hasEnergy: d.hasEnergy },
      })),
    );

    await this.applyLayout();
    const model = this.graphSaveService.load();
    if (model != null) {
      runInInjectionContext(
        this.injector,
        () => (this.model = initializeModel(model as Partial<Model>)),
      );
    }
  }

  private setupPinger() {
    this.pinger = setInterval(async () => {
      this.edificies = await lastValueFrom(
        this.edificeService.getEdifices(<any>new Date().toISOString()),
      );
      await this.ngDiagramModelService.updateNodes(
        this.edificies.map(e => ({
          id: String(e.id),
          data: e,
          type: 'default',
          groupId: e.district,
        })),
      );
    }, 1000);
  }
}
