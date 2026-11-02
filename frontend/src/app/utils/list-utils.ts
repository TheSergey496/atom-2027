export class ListUtils {
  public static removeIf<T>(
    list: T[],
    predicate: (item: T, index: number, array: T[]) => boolean,
  ): T[] {
    if (list == undefined) {
      return list;
    }

    for (let i = list.length - 1; i >= 0; i--) {
      if (predicate(list[i], i, list)) {
        list.splice(i, 1);
      }
    }

    return list;
  }
}