
export class EdificeDependencyDto {
  hasEnergy?: boolean;
  hasWater?: boolean;
  id?: number;
  sourceEdificeId?: number;
  targetEdificeId?: number;
}

export class EdificeDto {
  district?: string;
  hasEnergy?: boolean;
  hasWater?: boolean;
  houseNumber?: string;
  id?: number;
  name?: string;
  street?: string;
}

