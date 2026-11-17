import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  OnInit,
  signal,
  Signal,
  viewChild,
} from '@angular/core';
import {
  initializeModel,
  LinkingConfig,
  NgDiagramComponent,
  NgDiagramConfig, NgDiagramMinimapComponent,
  NgDiagramModelService,
  NgDiagramService,
  SelectionChangedEvent,
} from 'ng-diagram';
import { provideNgDiagram } from 'ng-diagram';
import ELK from 'elkjs/lib/elk.bundled.js';
import { ColoredEdgeComponent } from '../../graph/colored-edge/colored-edge.component';
import { LocalLoadingService } from '../../../services/loading.service';
import { CardModule } from '@openng/optimus-ui/card';
import { SplitterModule } from '@openng/optimus-ui/splitter';
import { EdificeDependencyService, EdificeService } from '../../../gen/atom2027-controllers';
import { EdificeDependencyDto, EdificeDto } from '../../../gen/atom2027-dto';
import { lastValueFrom } from 'rxjs';

@Component({
  selector: 'pro-legacy-home-page.component',
  providers: [provideNgDiagram()],
  imports: [NgDiagramComponent, SplitterModule, NgDiagramMinimapComponent],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css',
})
export class HomePageComponent implements OnInit {
  private readonly wrapper: Signal<any> = viewChild.required('wrapper');
  private readonly injector = inject(Injector);

  private loadingService?: LocalLoadingService;

  selectedNode: any | null = signal<any | null>(null);

  private readonly ngDiagramModelService = inject(NgDiagramModelService);
  private readonly ngDiagramService = inject(NgDiagramService);
  private readonly elk = new ELK();
  private readonly edificeService = inject(EdificeService);
  private readonly edificeDependencyService = inject(EdificeDependencyService);

  edificies: EdificeDto[] = [];
  dependencies: EdificeDependencyDto[] = [];

  linkingConfig: Partial<LinkingConfig> = {
    validateConnection: () => false,
  };

  config: NgDiagramConfig = {
    linking: this.linkingConfig,
  };

  protected readonly edgeTemplateMap = new Map([['colored', ColoredEdgeComponent]]);

  model = initializeModel({ nodes: [], edges: [] });

  constructor() {
    this.loadingService = new LocalLoadingService(
      computed(() => this.wrapper()?.nativeElement),
      this.injector,
    );
    this.loadingService.beginLoading();
  }

  async ngOnInit() {
    this.edificies = await lastValueFrom(
      this.edificeService.getEdifices(<any>new Date().toISOString()),
    );
    this.dependencies = await lastValueFrom(this.edificeDependencyService.getEdificeDependencies());

    await this.ngDiagramModelService.addNodes(
      this.edificies.map(e => ({
        id: String(e.id),
        position: { x: 0, y: 0 }, // временная позиция, ELK перезапишет
        data: e,
        groupId: e.district,
      })),
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
    this.loadingService?.stopLoading();
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
}
