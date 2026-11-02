export function getField(row: any, fieldPath: string): any {
  const path = (fieldPath ?? '').split('.');
  let currentLevel = row ?? {};
  for (let fieldName of path) {
    currentLevel = currentLevel[fieldName];
    if (currentLevel == null) {
      return null;
    }
  }

  return currentLevel;
}
