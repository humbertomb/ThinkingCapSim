{
  "name": "satant",
  "radius": 2.0,
  "icon": [
    {
      "xi": 1.8000000000000003,
      "yi": -0.5,
      "xf": 1.8000000000000003,
      "yf": 0.5
    },
    {
      "xi": 1.8000000000000003,
      "yi": 0.5,
      "xf": 1.5000000000000002,
      "yf": 1.0
    },
    {
      "xi": 1.5000000000000002,
      "yi": 1.0,
      "xf": 0.7999999999999999,
      "yf": 1.0
    },
    {
      "xi": 0.7999999999999999,
      "yi": 1.0,
      "xf": 0.7999999999999999,
      "yf": 0.5
    },
    {
      "xi": 0.7999999999999999,
      "yi": 0.5,
      "xf": -0.7000000000000001,
      "yf": 0.6
    },
    {
      "xi": -0.7000000000000001,
      "yi": 0.6,
      "xf": -0.7000000000000001,
      "yf": 1.0
    },
    {
      "xi": -0.7000000000000001,
      "yi": 1.0,
      "xf": -1.6999999999999997,
      "yf": 1.0
    },
    {
      "xi": -1.6999999999999997,
      "yi": 1.0,
      "xf": -1.6999999999999997,
      "yf": -1.0
    },
    {
      "xi": -1.6999999999999997,
      "yi": -1.0,
      "xf": -0.7000000000000001,
      "yf": -1.0
    },
    {
      "xi": -0.7000000000000001,
      "yi": -1.0,
      "xf": -0.7000000000000001,
      "yf": -0.6
    },
    {
      "xi": -0.7000000000000001,
      "yi": -0.6,
      "xf": 0.7999999999999999,
      "yf": -0.5
    },
    {
      "xi": 0.7999999999999999,
      "yi": -0.5,
      "xf": 0.7999999999999999,
      "yf": -1.0
    },
    {
      "xi": 0.7999999999999999,
      "yi": -1.0,
      "xf": 1.5000000000000002,
      "yf": -1.0
    },
    {
      "xi": 1.5000000000000002,
      "yi": -1.0,
      "xf": 1.8000000000000003,
      "yf": -0.5
    },
    {
      "xi": 0.7,
      "yi": -0.2,
      "xf": 0.7,
      "yf": 0.2
    },
    {
      "xi": 0.7,
      "yi": 0.2,
      "xf": 0.30000000000000004,
      "yf": 0.4
    },
    {
      "xi": 0.30000000000000004,
      "yi": 0.4,
      "xf": -0.5000000000000001,
      "yf": 0.4
    },
    {
      "xi": -0.5000000000000001,
      "yi": 0.4,
      "xf": -0.5000000000000001,
      "yf": -0.4
    },
    {
      "xi": -0.5000000000000001,
      "yi": -0.4,
      "xf": 0.30000000000000004,
      "yf": -0.4
    },
    {
      "xi": 0.30000000000000004,
      "yi": -0.4,
      "xf": 0.7,
      "yf": -0.2
    },
    {
      "xi": 1.8000000000000003,
      "yi": 0.0,
      "xf": 1.6000000000000003,
      "yf": 0.2
    },
    {
      "xi": 1.6000000000000003,
      "yi": 0.2,
      "xf": 1.6000000000000003,
      "yf": -0.2
    },
    {
      "xi": 1.6000000000000003,
      "yi": -0.2,
      "xf": 1.8000000000000003,
      "yf": 0.0
    }
  ],
  "kinematics": {
    "drive": "tc.vrobot.models.AckermanDrive",
    "vmax": 40.0,
    "rmax": 30.0,
    "samax": 0.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "rwheel": 0.0,
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
          "rho": 1.891372198494391,
          "theta": 12.6471990910225,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0,
          "driver": "devices.drivers.radar.Fujitsu.Fujitsu",
          "driverParams": "/dev/ttyS0",
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
  "wheels": [
    {
      "x": 1.1700000000000002,
      "y": 0.75,
      "z": 0.296,
      "orientation": 0.0,
      "radius": 0.296,
      "width": 0.205,
      "steerable": true,
      "maxsteer": 45.0,
      "maxturning": 30.0,
      "traction": false,
      "maxrpm": 0.0
    },
    {
      "x": -1.1699999999999997,
      "y": -0.75,
      "z": 0.296,
      "orientation": 0.0,
      "radius": 0.296,
      "width": 0.205,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 1288.0
    },
    {
      "x": 1.1700000000000002,
      "y": -0.75,
      "z": 0.296,
      "orientation": 0.0,
      "radius": 0.296,
      "width": 0.205,
      "steerable": true,
      "maxsteer": 45.0,
      "maxturning": 30.0,
      "traction": false,
      "maxrpm": 0.0
    },
    {
      "x": -1.1699999999999997,
      "y": 0.75,
      "z": 0.296,
      "orientation": 0.0,
      "radius": 0.296,
      "width": 0.205,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 1288.0
    }
  ],
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
