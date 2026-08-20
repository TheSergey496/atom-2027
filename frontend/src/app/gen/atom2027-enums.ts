export const RoleLocaleEnum = {
  ADMIN: 'Администратор',
  USER: 'Пользователь',
} as const;
export type RoleLocaleEnum = typeof RoleLocaleEnum;

export const RoleRecord = {
  ADMIN: 'ADMIN',
  USER: 'USER',
} as const;
export type RoleRecord = typeof RoleRecord;
