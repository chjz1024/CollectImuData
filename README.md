# CollectImuData
# Use the following script with i/o redirection for parsing the output data
```py
#!/usr/env python3
import sys
import struct
from __future__ import print_function

data: bytes = sys.stdin.buffer.read(21)
while (len(data) != 0):
  # pdb.set_trace()
  # sensor, timestamp, x, y, z = struct.unpack("cqfff", data)
  sensor = data[0:1].decode("ascii")
  # , int.from_bytes(data[1:9],"big")
  timestamp, x, y, z = struct.unpack(">qfff", data[1:21])
  # , int.from_bytes(data[9:13],"big"), int.from_bytes(data[13:17],"big"), int.from_bytes(data[17:21],"big")
  print(f"{sensor}: {timestamp}; {x}, {y}, {z}")
  data: bytes = sys.stdin.buffer.read(21)
```
