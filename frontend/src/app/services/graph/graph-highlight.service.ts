import { Injectable, inject, signal, Service } from '@angular/core';
import { NgDiagramModelService, Node } from 'ng-diagram';
import { EdificeDependencyDto } from '../../gen/atom2027-dto';

export type HighlightDirection = 'forward' | 'reverse' | 'both';
export type HighlightRole = 'source' | 'dependent';

/**
 * NgDiagramComponent has no (hoverEnter)/(hoverLeave) outputs, and outputs of
 * components created through nodeTemplateMap are never bound. Hover is therefore
 * captured on the node host (HoverableNodeComponent) and routed through this
 * service, which diffs state and writes only changed nodes back to the model.
 */
@Service()
export class GraphHighlightService {
  private readonly modelService = inject(NgDiagramModelService);

  private readonly outgoing = new Map<string, string[]>();
  private readonly incoming = new Map<string, string[]>();
  private readonly names = new Map<string, string>();
  private readonly applied = new Map<string, { role: HighlightRole | null; dimmed: boolean }>();

  private hoveredId: string | null = null;
  private pinnedId: string | null = null;
  private writeQueue: Promise<void> = Promise.resolve();

  readonly activeId = signal<string | null>(null);
  readonly direction = signal<HighlightDirection>('forward');
  readonly dimOthers = signal(true);
  readonly enabled = signal(true);

  setGraph(nodes: Node[], dependencies: EdificeDependencyDto[]): void {
    this.reset();
    for (const node of nodes) {
      const name = (node.data as { name?: string } | undefined)?.name;
      this.names.set(node.id, name ?? node.id);
    }
    for (const dep of dependencies) {
      const source = String(dep.sourceEdificeId);
      const target = String(dep.targetEdificeId);
      this.push(this.outgoing, source, target);
      this.push(this.incoming, target, source);
    }
  }

  reset(): void {
    this.outgoing.clear();
    this.incoming.clear();
    this.names.clear();
    this.applied.clear();
    this.hoveredId = null;
    this.pinnedId = null;
    this.activeId.set(null);
  }

  hover(id: string): void {
    if (!this.names.has(id)) return;
    this.hoveredId = id;
    this.apply();
  }

  unhover(): void {
    this.hoveredId = null;
    this.apply();
  }

  /** Selection pins a highlight; hover previews temporarily override it. */
  pin(id: string | null): void {
    this.pinnedId = id !== null && this.names.has(id) ? id : null;
    this.apply();
  }

  setDirection(direction: HighlightDirection): void {
    this.direction.set(direction);
    this.apply();
  }

  setDimOthers(value: boolean): void {
    this.dimOthers.set(value);
    this.apply();
  }

  setEnabled(value: boolean): void {
    this.enabled.set(value);
    this.apply();
  }

  nameOf(id: string): string {
    return this.names.get(id) ?? id;
  }

  outgoingIds(id: string): string[] {
    return this.sorted(this.outgoing.get(id) ?? []);
  }

  incomingIds(id: string): string[] {
    return this.sorted(this.incoming.get(id) ?? []);
  }

  reachableIds(id: string): string[] {
    if (!this.names.has(id)) return [];
    return this.sorted([...this.computeReachable(id)]);
  }

  private computeReachable(start: string): Set<string> {
    const direction = this.direction();
    const visited = new Set<string>([start]);
    const queue: string[] = [start];
    while (queue.length > 0) {
      const current = queue.shift()!;
      const neighbors: string[] = [];
      if (direction !== 'reverse') neighbors.push(...(this.outgoing.get(current) ?? []));
      if (direction !== 'forward') neighbors.push(...(this.incoming.get(current) ?? []));
      for (const neighbor of neighbors) {
        if (!visited.has(neighbor)) {
          visited.add(neighbor);
          queue.push(neighbor);
        }
      }
    }
    visited.delete(start);
    return visited;
  }

  private apply(): void {
    const active = this.enabled() ? (this.hoveredId ?? this.pinnedId) : null;
    this.activeId.set(active);

    const roles = new Map<string, HighlightRole>();
    if (active !== null) {
      roles.set(active, 'source');
      for (const id of this.computeReachable(active)) {
        if (id !== active) roles.set(id, 'dependent');
      }
    }

    const updates: Partial<Node>[] = [];
    for (const node of this.modelService.getModel().getNodes()) {
      const role = roles.get(node.id) ?? null;
      const dimmed = active !== null && this.dimOthers() && role === null;
      const previous = this.applied.get(node.id);
      if (previous?.role === role && previous?.dimmed === dimmed) continue;
      this.applied.set(node.id, { role, dimmed });
      updates.push({
        id: node.id,
        data: { ...node.data, highlightRole: role, dimmed },
      });
    }
    if (updates.length > 0) this.enqueue(updates);
  }

  private enqueue(updates: Partial<Node>[]): void {
    this.writeQueue = this.writeQueue
      .then(() => this.modelService.updateNodes(updates as unknown as any))
      .then(() => undefined)
      .catch(error => console.error('Graph highlight update failed', error));
  }

  private sorted(ids: string[]): string[] {
    return [...ids].sort((a, b) => this.nameOf(a).localeCompare(this.nameOf(b)));
  }

  private push(map: Map<string, string[]>, key: string, value: string): void {
    const list = map.get(key);
    if (list) list.push(value);
    else map.set(key, [value]);
  }
}
