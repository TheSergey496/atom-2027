
export class EdificeDependencyDto {
  hasEnergy?: boolean;
  hasWater?: boolean;
  id?: number;
  sourceEdificeId?: number;
  targetEdificeId?: number;
}

export class EdificeDto {
  activeEvent?: EventDto;
  criticalIndex?: number;
  district?: string;
  hasEnergy?: boolean;
  hasWater?: boolean;
  houseNumber?: string;
  id?: number;
  name?: string;
  street?: string;
}

export class EventDto {
  edificeId?: number;
  eventStatusId?: number;
  eventStatusName?: string;
  id?: number;
  occurredAt?: Date;
  resourceType?: string;
}

