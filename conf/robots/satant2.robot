{
  "name": "satant2",
  "radius": 0.1,
  "icon": [
    {
      "xi": 1.5,
      "yi": -0.5,
      "xf": 1.5,
      "yf": 0.5
    },
    {
      "xi": 1.5,
      "yi": 0.5,
      "xf": 1.2,
      "yf": 1.0
    },
    {
      "xi": 1.2,
      "yi": 1.0,
      "xf": 0.5,
      "yf": 1.0
    },
    {
      "xi": 0.5,
      "yi": 1.0,
      "xf": 0.5,
      "yf": 0.5
    },
    {
      "xi": 0.5,
      "yi": 0.5,
      "xf": -1.0,
      "yf": 0.6
    },
    {
      "xi": -1.0,
      "yi": 0.6,
      "xf": -1.0,
      "yf": 1.0
    },
    {
      "xi": -1.0,
      "yi": 1.0,
      "xf": -2.0,
      "yf": 1.0
    },
    {
      "xi": -2.0,
      "yi": 1.0,
      "xf": -2.0,
      "yf": -1.0
    },
    {
      "xi": -2.0,
      "yi": -1.0,
      "xf": -1.0,
      "yf": -1.0
    },
    {
      "xi": -1.0,
      "yi": -1.0,
      "xf": -1.0,
      "yf": -0.6
    },
    {
      "xi": -1.0,
      "yi": -0.6,
      "xf": 0.5,
      "yf": -0.5
    },
    {
      "xi": 0.5,
      "yi": -0.5,
      "xf": 0.5,
      "yf": -1.0
    },
    {
      "xi": 0.5,
      "yi": -1.0,
      "xf": 1.2,
      "yf": -1.0
    },
    {
      "xi": 1.2,
      "yi": -1.0,
      "xf": 1.5,
      "yf": -0.5
    },
    {
      "xi": 0.4,
      "yi": -0.2,
      "xf": 0.4,
      "yf": 0.2
    },
    {
      "xi": 0.4,
      "yi": 0.2,
      "xf": 0.0,
      "yf": 0.4
    },
    {
      "xi": 0.0,
      "yi": 0.4,
      "xf": -0.8,
      "yf": 0.4
    },
    {
      "xi": -0.8,
      "yi": 0.4,
      "xf": -0.8,
      "yf": -0.4
    },
    {
      "xi": -0.8,
      "yi": -0.4,
      "xf": 0.0,
      "yf": -0.4
    },
    {
      "xi": 0.0,
      "yi": -0.4,
      "xf": 0.4,
      "yf": -0.2
    },
    {
      "xi": 1.5,
      "yi": 0.0,
      "xf": 1.3,
      "yf": 0.2
    },
    {
      "xi": 1.3,
      "yi": 0.2,
      "xf": 1.3,
      "yf": -0.2
    },
    {
      "xi": 1.3,
      "yi": -0.2,
      "xf": 1.5,
      "yf": 0.0
    }
  ],
  "kinematics": {
    "drive": "tc.vrobot.models.AckermanDrive",
    "vmax": 40.0,
    "rmax": 30.0,
    "maxmotor": 40.0,
    "maxsteer": 45.0,
    "samax": 0.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "length": 2.34,
    "base": 0.0,
    "rwheel": 0.0,
    "wheel": 0.0,
    "gear": 0.0,
    "pulses": 0.0,
    "dtime": 1000,
    "odomET": 0.0,
    "odomER": 0.0,
    "odomBias": 0.0
  },
  "sensors": {
    "son": {
      "sensors": []
    },
    "ir": {
      "sensors": []
    },
    "lrf": {
      "sensors": []
    },
    "lsb": {
      "sensors": []
    },
    "trk": {
      "sensors": []
    },
    "vis": {
      "sensors": []
    }
  },
  "bumpers": [],
  "extra": {
    "COMPASS0_OFFSET": "-180.0",
    "MAXCOMPASS": "1",
    "COMPASS0": "devices.drivers.compass.TCM2.TCM2|/dev/ttyS3",
    "GPS0": "devices.drivers.gps.Novatel.Novatel|/dev/ttyS1",
    "MAXGPS": "1"
  }
}
