
export class EdificeDependencyDto {
  hasEnergy?: boolean;
  hasWater?: boolean;
  id?: number;
  sourceEdificeId?: number;
  targetEdificeId?: number;
}

export class EdificeDto {
  accidentTooltip?: string;
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
  eventStatus?: EventStatusDto;
  eventStatusId?: number;
  id?: number;
  occurredAt?: Date;
  resourceType?: string;
}

export class EventStatusDto {
  id?: number;
  name?: string;
}

