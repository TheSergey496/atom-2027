export class CustomFilterMatchMode {
  public static readonly nullReservedFilterWord = '__nullReservedWord';
  public static readonly NULLABLE_EQUALS = 'nullableEquals';
  public static readonly NULLABLE_NOT_EQUALS = 'nullableNotEquals';

  public static nullableEqualsFilter(
    defaultEquals: Function,
  ): (value: any, filter: any, filterLocale?: any) => boolean {
    return (value, filter, filterLocale) => {
      if (filter === CustomFilterMatchMode.nullReservedFilterWord) {
        return value == null;
      }
      return defaultEquals(value, filter, filterLocale);
    };
  }

  public static nullableNotEqualsFilter(
    defaultNotEquals: Function,
  ): (value: any, filter: any, filterLocale?: any) => boolean {
    return (value, filter, filterLocale) => {
      if (filter === CustomFilterMatchMode.nullReservedFilterWord) {
        return value != null;
      }
      return defaultNotEquals(value, filter, filterLocale);
    };
  }
}
