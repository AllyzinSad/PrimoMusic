"""Check that Android PNG resources contain a readable compressed pixel stream.

The Android build accepted a corrupted koda_mark.png and Compose crashed when
decoding it on the first frame. Verify both chunk CRCs and compressed data.
"""

from pathlib import Path
import struct
import sys
import zlib


def check(path: Path) -> None:
    data = path.read_bytes()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("missing PNG signature")
    offset = 8
    compressed = bytearray()
    ended = False
    while offset + 12 <= len(data):
        length = struct.unpack_from(">I", data, offset)[0]
        if offset + 12 + length > len(data):
            raise ValueError("truncated chunk")
        chunk_type = data[offset + 4 : offset + 8]
        payload = data[offset + 8 : offset + 8 + length]
        actual_crc = struct.unpack_from(">I", data, offset + 8 + length)[0]
        if zlib.crc32(chunk_type + payload) != actual_crc:
            raise ValueError("bad chunk CRC")
        if chunk_type == b"IDAT":
            compressed.extend(payload)
        offset += 12 + length
        if chunk_type == b"IEND":
            ended = True
            break
    if not ended or not compressed:
        raise ValueError("missing image data or IEND")
    decompressor = zlib.decompressobj()
    decompressor.decompress(compressed)
    decompressor.flush()
    if not decompressor.eof:
        raise ValueError("incomplete pixel stream")


def main() -> int:
    files = sorted(Path("app/src/main/res").rglob("*.png"))
    bad = []
    for path in files:
        try:
            check(path)
        except (ValueError, zlib.error) as exc:
            bad.append(f"{path}: {exc}")
    if bad:
        print("\n".join(bad), file=sys.stderr)
        return 1
    print(f"Validated {len(files)} Android PNG resources")
    return 0


if __name__ == "__main__":
    sys.exit(main())
