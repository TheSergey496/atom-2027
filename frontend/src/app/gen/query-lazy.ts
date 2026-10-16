
export const MatchModeRecord = {
  startsWith: 'startsWith',
  contains: 'contains',
  notContains: 'notContains',
  endsWith: 'endsWith',
  equals: 'equals',
  notEquals: 'notEquals',
  lt: 'lt',
  lte: 'lte',
  gt: 'gt',
  gte: 'gte',
  dateIs: 'dateIs',
  dateIsNot: 'dateIsNot',
  dateBefore: 'dateBefore',
  dateAfter: 'dateAfter',
} as const;
export type MatchModeRecord = typeof MatchModeRecord;


export class PageQuery {
  filters?: {[key: string] :PageQuery$FilterMeta};
  first?: number;
  multiSortMeta?: PageQuery$SortMeta[];
  rows?: number;
  sortField?: string;
  sortOrder?: number;
}

export class PageQuery$FilterMeta {
  matchMode?: MatchModeRecord;
  value?: string;
}

export class PageQuery$SortMeta {
  field?: string;
  order?: number;
}

export class PageResponse <T> {
  items?: T[];
  total?: number;
}

