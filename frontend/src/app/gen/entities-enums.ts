
export const CheckStatusRecord = {
  SCHEDULED: 'SCHEDULED',
  CANCELED: 'CANCELED',
  FINISHED: 'FINISHED',
} as const;
export type CheckStatusRecord = typeof CheckStatusRecord;


export const CheckTypeRecord = {
  PLAN: 'PLAN',
  REPEATED: 'REPEATED',
  NON_PLAN: 'NON_PLAN',
} as const;
export type CheckTypeRecord = typeof CheckTypeRecord;


