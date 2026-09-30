const textEncoder = new TextEncoder();

export async function hashAndBase64(string: string, algorithm = 'SHA-256') {
  const msgUint8 = textEncoder.encode(string);
  const hashBuffer = await crypto.subtle.digest(algorithm, msgUint8);

  const uint8Array = new Uint8Array(hashBuffer);

  const binaryString = String.fromCharCode(...uint8Array);

  return btoa(binaryString);
}
