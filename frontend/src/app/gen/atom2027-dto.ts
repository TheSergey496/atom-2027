
export class EdificeDependencyDto {
  hasEnergy?: boolean;
  hasWater?: boolean;
  id?: number;
  sourceEdificeId?: number;
  targetEdificeId?: number;
}

export class EdificeDto {
  criticalIndex?: number;
  district?: string;
  energyActiveEvent?: EventDto;
  hasEnergy?: boolean;
  hasWater?: boolean;
  houseNumber?: string;
  id?: number;
  name?: string;
  street?: string;
  waterActiveEvent?: EventDto;
}

export class EventDto {
  edifice?: EdificeDto;
  edificeId?: number;
  eventStatusId?: number;
  eventStatusName?: string;
  id?: number;
  occurredAt?: Date;
  resourceType?: string;
}

