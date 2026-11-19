import {CheckStatusRecord, CheckTypeRecord} from './entities-enums';

export class BrigadeDto {
  events?: EventDto[];
  id?: number;
  name?: string;
}

export class BrigadeEventDto {
  actualResolutionTimeStr?: string;
  averageResolutionTimeStr?: string;
  brigade?: BrigadeDto;
  edifice?: EdificeDto;
  event?: EventDto;
  expectedEndTime?: any;
  id?: number;
  orderNumber?: number;
  startDate?: any;
}

export class CheckDto {
  checkId?: number;
  checkStatus?: CheckStatusRecord;
  checkType?: CheckTypeRecord;
  comment?: string;
  edifice?: EdificeDto;
  planDate?: Date;
  protocolDate?: Date;
  responsible?: EmployeeDto;
}

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

export class EmployeeDto {
  firstName?: string;
  fullName?: string;
  id?: number;
  lastName?: string;
  middleName?: string;
  qualifications?: string;
  startDateExperience?: Date;
}

export class EmployeeQualificationDto {
  employeeId?: number;
  id?: number;
  proff?: number;
  resourceType?: string;
}

export class EventDto {
  brigadeId?: number;
  brigadeOrderNumber?: number;
  edifice?: EdificeDto;
  edificeId?: number;
  eventStatus?: EventStatusDto;
  eventStatusId?: number;
  id?: number;
  isArchived?: boolean;
  occurredAt?: Date;
  resourceType?: string;
}

export class EventStatusDto {
  id?: number;
  name?: string;
}

