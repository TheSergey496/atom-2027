
export class RoleDto {
  id?: number;
  localeName?: string;
  name?: string;
}

export class UserDto {
  archived?: boolean;
  email?: string;
  firstname?: string;
  fullName?: string;
  id?: number;
  lastname?: string;
  password?: string;
  phoneNumber?: string;
  registrationDate?: Date;
  roles?: RoleDto[];
  rolesAsString?: string;
  shortName?: string;
  surname?: string;
}

