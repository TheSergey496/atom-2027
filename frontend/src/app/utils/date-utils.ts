export class DateUtils {
  public static toLocalDate(date: Date | null) {
    if (date == null) {
      return null;
    }

    return new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  }

  public static toUtcDate(date: Date | null) {
    if (date == null) {
      return null;
    }

    return new Date(date.getTime() + date.getTimezoneOffset() * 60_000);
  }
}