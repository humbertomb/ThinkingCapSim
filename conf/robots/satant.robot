{
  "name": "satant",
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
    "dtime": 100,
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
      "sensors": [
        {
          "rho": 1.6,
          "theta": 15.0,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0,
          "driver": "devices.drivers.radar.Fujitsu.Fujitsu|/dev/ttyS0",
          "rangemax": 140.0,
          "rangemin": 5.0,
          "cone": 16.0,
          "objects": 8
        }
      ]
    },
    "vis": {
      "sensors": []
    }
  },
  "bumpers": [],
  "extra": {
    "RAYRAD": "16",
    "CAN_ADDRESS": "mimics4.inf.um.es",
    "REC_PORT": "10005",
    "CAPTORS": "devices.drivers.captors.UDP.UDPCaptors|10005",
    "grouplen0": "1.6",
    "COMPASS0_OFFSET": "-180.0",
    "MAXENCS": "4",
    "CONEGROUP": "16.0",
    "BRAKE30_T3": "1500",
    "BRAKE30_T1": "1000",
    "RANGEGROUP": "50.00",
    "BRAKE30_T2": "1000",
    "CAN_PORT": "10001",
    "MAXCOMPASS": "1",
    "COMPASS0": "devices.drivers.compass.TCM2.TCM2|/dev/ttyS3",
    "KMSPD": "30",
    "RADAR0": "devices.drivers.radar.Fujitsu.Fujitsu|/dev/ttyS0",
    "KMOTOR": "70",
    "BRAKE20_T1": "1000",
    "BRAKE20_T2": "2000",
    "BRAKE20_T3": "1500",
    "GPS0": "devices.drivers.gps.Novatel.Novatel|/dev/ttyS1",
    "groupfeat0": "0.0",
    "MAXRADAR": "1",
    "MAXGPS": "1",
    "KSTEER": "150",
    "MAXGROUP": "1",
    "BRAKE40_T1": "1000",
    "groupmode0": "6",
    "BRAKE40_T2": "500",
    "BRAKE40_T3": "1500",
    "grouprho0": "15.0"
  }
}
