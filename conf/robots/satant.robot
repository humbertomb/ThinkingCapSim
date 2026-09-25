{
  "name": "Satant",
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
    "vmax": 0.0,
    "umax": 0.0,
    "rmax": 0.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "rwheel": 0.0,
    "skid": 1.0,
    "gear": 0.0,
    "pulses": 0.0,
    "odomET": 0.0,
    "odomER": 0.0,
    "odomBias": 0.0
  },
  "sensors": {
    "son": {
      "simerror": 0.05,
      "sensors": []
    },
    "ir": {
      "simerror": 0.05,
      "sensors": []
    },
    "lrf": {
      "simerror": 0.05,
      "sensors": []
    },
    "lsb": {
      "simerror": 0.05,
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
          "rays": 16,
          "objects": 8
        }
      ]
    },
    "vis": {
      "sensors": []
    },
    "camera": {
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
  "groups": [
    {
      "mode": 6,
      "base": 0.3,
      "rho": 1.6,
      "theta": 15.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 50.0,
      "rangemin": 0.0,
      "cone": 16.0
    }
  ],
  "fusionmode": 0,
  "extra": {
    "CAN_ADDRESS": "mimics4.inf.um.es",
    "REC_PORT": "10005",
    "CAPTORS": "devices.drivers.captors.UDP.UDPCaptors|10005",
    "COMPASS0_OFFSET": "-180.0",
    "MAXENCS": "4",
    "BRAKE30_T3": "1500",
    "BRAKE30_T1": "1000",
    "BRAKE30_T2": "1000",
    "CAN_PORT": "10001",
    "MAXCOMPASS": "1",
    "COMPASS0": "devices.drivers.compass.TCM2.TCM2|/dev/ttyS3",
    "KMSPD": "30",
    "KMOTOR": "70",
    "BRAKE20_T1": "1000",
    "BRAKE20_T2": "2000",
    "BRAKE20_T3": "1500",
    "GPS0": "devices.drivers.gps.Novatel.Novatel|/dev/ttyS1",
    "MAXGPS": "1",
    "KSTEER": "150",
    "BRAKE40_T1": "1000",
    "BRAKE40_T2": "500",
    "BRAKE40_T3": "1500"
  }
}
