#!/usr/bin/env python3
"""Verify 16 KiB PT_LOAD ELF alignment of native libraries in APK and AAB.

Jetpack Compose graphics packages libandroidx.graphics.path.so transitively:
the absence of source NDK usage is NOT evidence of no native code.
Checks PT_LOAD p_align and offset/virtual address congruence.
Package zipalign is checked separately in the Android CI workflow.
"""
from __future__ import annotations
import argparse
from pathlib import Path
import struct
import zipfile

PAGE = 16384
PT_LOAD = 1

def check_elf(filename: str, data: bytes) -> None:
    if len(data) < 64 or data[:4] != b"\x7fELF":
        raise ValueError(f"{filename}: invalid ELF header")
    cls, order = data[4], data[5]
    if cls not in (1, 2) or order not in (1, 2):
        raise ValueError(f"{filename}: unsupported ELF class/endianness")
    e = "<" if order == 1 else ">"
    if cls == 2:
        phoff = struct.unpack_from(e+"Q", data, 32)[0]
        phentsize, phnum = struct.unpack_from(e+"HH", data, 54)
        minimum = 56
    else:
        phoff = struct.unpack_from(e+"I", data, 28)[0]
        phentsize, phnum = struct.unpack_from(e+"HH", data, 42)
        minimum = 32
    if not phnum or phentsize < minimum or phoff + phentsize * phnum > len(data):
        raise ValueError(f"{filename}: invalid program header table")
    found = 0
    for i in range(phnum):
        off = phoff + phentsize * i
        p_type = struct.unpack_from(e+"I", data, off)[0]
        if p_type != PT_LOAD:
            continue
        found += 1
        if cls == 2:
            p_offset = struct.unpack_from(e+"Q", data, off+8)[0]
            p_vaddr = struct.unpack_from(e+"Q", data, off+16)[0]
            p_align = struct.unpack_from(e+"Q", data, off+48)[0]
        else:
            p_offset = struct.unpack_from(e+"I", data, off+4)[0]
            p_vaddr = struct.unpack_from(e+"I", data, off+8)[0]
            p_align = struct.unpack_from(e+"I", data, off+28)[0]
        if p_align < PAGE or p_offset % PAGE != p_vaddr % PAGE:
            raise ValueError(f"{filename}: PT_LOAD[{i}] page alignment {p_align}, "
                             f"offset={p_offset:#x}, vaddr={p_vaddr:#x}; requires 16 KiB")
    if not found:
        raise ValueError(f"{filename}: no PT_LOAD segments")
    print(f"PASS: {filename} ({found} PT_LOAD segments, 16 KiB aligned)")

def check_archive(filename: str) -> int:
    count = 0
    with zipfile.ZipFile(filename) as archive:
        for entry in archive.namelist():
            if entry.endswith(".so"):
                check_elf(f"{filename}:{entry}", archive.read(entry))
                count += 1
    print(f"PASS: {filename} scanned {count} native libraries")
    return count

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("artifacts", nargs="+", type=Path)
    args = parser.parse_args()
    for artifact in args.artifacts:
        check_archive(str(artifact))
