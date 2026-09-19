{
  "name": "pioneer3",
  "radius": 0.25,
  "icon": [
    {
      "xi": 0.255,
      "yi": -0.07,
      "xf": 0.255,
      "yf": 0.07
    },
    {
      "xi": 0.255,
      "yi": 0.07,
      "xf": 0.18,
      "yf": 0.19
    },
    {
      "xi": 0.18,
      "yi": 0.19,
      "xf": 0.0,
      "yf": 0.19
    },
    {
      "xi": 0.0,
      "yi": 0.19,
      "xf": 0.0,
      "yf": 0.16
    },
    {
      "xi": 0.0,
      "yi": 0.16,
      "xf": -0.18,
      "yf": 0.16
    },
    {
      "xi": -0.18,
      "yi": 0.16,
      "xf": -0.255,
      "yf": 0.07
    },
    {
      "xi": -0.255,
      "yi": 0.07,
      "xf": -0.255,
      "yf": -0.07
    },
    {
      "xi": -0.255,
      "yi": -0.07,
      "xf": -0.18,
      "yf": -0.16
    },
    {
      "xi": -0.18,
      "yi": -0.16,
      "xf": 0.0,
      "yf": -0.16
    },
    {
      "xi": 0.0,
      "yi": -0.16,
      "xf": 0.0,
      "yf": -0.19
    },
    {
      "xi": 0.0,
      "yi": -0.19,
      "xf": 0.18,
      "yf": -0.19
    },
    {
      "xi": 0.18,
      "yi": -0.19,
      "xf": 0.255,
      "yf": -0.07
    },
    {
      "xi": 0.23,
      "yi": 0.0,
      "xf": 0.13,
      "yf": -0.1
    },
    {
      "xi": 0.23,
      "yi": 0.0,
      "xf": 0.13,
      "yf": 0.1
    },
    {
      "xi": 0.13,
      "yi": -0.1,
      "xf": 0.13,
      "yf": 0.1
    },
    {
      "xi": 0.20475,
      "yi": 0.1495,
      "xf": 0.23,
      "yf": 0.15
    },
    {
      "xi": 0.23,
      "yi": 0.15,
      "xf": 0.23,
      "yf": 0.25
    },
    {
      "xi": 0.23,
      "yi": 0.25,
      "xf": 0.025,
      "yf": 0.25
    },
    {
      "xi": 0.025,
      "yi": 0.25,
      "xf": 0.025,
      "yf": 0.19
    },
    {
      "xi": 0.20475,
      "yi": -0.15,
      "xf": 0.23,
      "yf": -0.15
    },
    {
      "xi": 0.23,
      "yi": -0.15,
      "xf": 0.23,
      "yf": -0.25
    },
    {
      "xi": 0.23,
      "yi": -0.25,
      "xf": 0.025,
      "yf": -0.25
    },
    {
      "xi": 0.025,
      "yi": -0.25,
      "xf": 0.025,
      "yf": -0.19
    },
    {
      "xi": -0.18825,
      "yi": 0.15,
      "xf": -0.23,
      "yf": 0.15
    },
    {
      "xi": -0.23,
      "yi": 0.15,
      "xf": -0.23,
      "yf": 0.25
    },
    {
      "xi": -0.23,
      "yi": 0.25,
      "xf": -0.025,
      "yf": 0.25
    },
    {
      "xi": -0.025,
      "yi": 0.25,
      "xf": -0.025,
      "yf": 0.16
    },
    {
      "xi": -0.18875,
      "yi": -0.1495,
      "xf": -0.23,
      "yf": -0.15
    },
    {
      "xi": -0.23,
      "yi": -0.15,
      "xf": -0.23,
      "yf": -0.25
    },
    {
      "xi": -0.23,
      "yi": -0.25,
      "xf": -0.025,
      "yf": -0.25
    },
    {
      "xi": -0.025,
      "yi": -0.25,
      "xf": -0.025,
      "yf": -0.16
    }
  ],
  "image": "./conf/2dmodels/pioneer3.png",
  "shapeRobot": "./conf/3dmodels/pioneer3at.3ds",
  "kinematics": {
    "drive": "tc.vrobot.models.SkidSteerDrive",
    "lamax": 0.0,
    "ldmax": 0.0,
    "rwheel": 0.0,
    "skid": 1.5,
    "gear": 71.0,
    "pulses": 2000.0,
    "dtime": 175,
    "odomET": 0.0,
    "odomER": 0.0,
    "odomBias": 0.0
  },
  "sensors": {
    "son": {
      "rangemax": 10.0,
      "rangemin": 0.135,
      "cone": 20.0,
      "rays": 11,
      "cycle": 4,
      "simmode": 2,
      "simerror": 0.05,
      "sensors": [
        {
          "rho": 0.161245154965971,
          "theta": 60.25511870305778,
          "height": 0.25,
          "orientation": 90.0,
          "step": 1
        },
        {
          "rho": 0.21783,
          "theta": 31.86,
          "height": 0.25,
          "orientation": 50.0,
          "step": 2
        },
        {
          "rho": 0.23409,
          "theta": 19.98,
          "height": 0.25,
          "orientation": 30.0,
          "step": 3
        },
        {
          "rho": 0.2413,
          "theta": 5.95,
          "height": 0.25,
          "orientation": 10.0,
          "step": 4
        },
        {
          "rho": 0.2413,
          "theta": -5.95,
          "height": 0.25,
          "orientation": -10.0,
          "step": 1
        },
        {
          "rho": 0.23409,
          "theta": -19.98,
          "height": 0.25,
          "orientation": -30.0,
          "step": 2
        },
        {
          "rho": 0.21783,
          "theta": -31.86,
          "height": 0.25,
          "orientation": -50.0,
          "step": 3
        },
        {
          "rho": 0.17204650534085256,
          "theta": -54.46232220802562,
          "height": 0.25,
          "orientation": -90.0,
          "step": 4
        },
        {
          "rho": 0.18439088914585774,
          "theta": -130.6012946450045,
          "height": 0.25,
          "orientation": -90.0,
          "step": 1
        },
        {
          "rho": 0.21783,
          "theta": -148.14,
          "height": 0.25,
          "orientation": -130.0,
          "step": 2
        },
        {
          "rho": 0.23409,
          "theta": -160.02,
          "height": 0.25,
          "orientation": -150.0,
          "step": 3
        },
        {
          "rho": 0.2413,
          "theta": -174.05,
          "height": 0.25,
          "orientation": -170.0,
          "step": 4
        },
        {
          "rho": 0.2413,
          "theta": 174.05,
          "height": 0.25,
          "orientation": 170.0,
          "step": 1
        },
        {
          "rho": 0.23409,
          "theta": 160.02,
          "height": 0.25,
          "orientation": 150.0,
          "step": 2
        },
        {
          "rho": 0.21783,
          "theta": 148.14,
          "height": 0.25,
          "orientation": 130.0,
          "step": 3
        },
        {
          "rho": 0.18439088914585774,
          "theta": 130.6012946450045,
          "height": 0.25,
          "orientation": 90.0,
          "step": 4
        }
      ]
    },
    "ir": {
      "simerror": 0.05,
      "sensors": []
    },
    "lrf": {
      "cycle": 6,
      "simerror": 0.05,
      "sensors": [
        {
          "rho": 0.2,
          "theta": 0.0,
          "height": 0.35,
          "orientation": 0.0,
          "step": 4,
          "driver": "devices.drivers.laser.LMS200.LMS200",
          "driverParams": "/dev/tty.usbserial",
          "rangemax": 82.0,
          "rangemin": 0.01,
          "cone": 180.0,
          "rays": 361
        }
      ]
    },
    "lsb": {
      "simerror": 0.05,
      "sensors": []
    },
    "trk": {
      "sensors": []
    },
    "vis": {
      "sensors": []
    }
  },
  "bumpers": [
    {
      "xi": 0.26,
      "yi": 0.26,
      "xf": 0.26,
      "yf": -0.26
    },
    {
      "xi": 0.26,
      "yi": -0.26,
      "xf": -0.26,
      "yf": -0.26
    },
    {
      "xi": -0.26,
      "yi": -0.26,
      "xf": -0.26,
      "yf": 0.26
    },
    {
      "xi": -0.26,
      "yi": 0.26,
      "xf": 0.26,
      "yf": 0.26
    }
  ],
  "wheels": [
    {
      "x": -0.134,
      "y": 0.1905,
      "z": 0.1105,
      "orientation": 0.0,
      "radius": 0.1105,
      "width": 0.087,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 61.0
    },
    {
      "x": 0.134,
      "y": 0.1905,
      "z": 0.1105,
      "orientation": 0.0,
      "radius": 0.1105,
      "width": 0.087,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 61.0
    },
    {
      "x": 0.134,
      "y": -0.1905,
      "z": 0.1105,
      "orientation": 0.0,
      "radius": 0.1105,
      "width": 0.087,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 61.0
    },
    {
      "x": -0.134,
      "y": -0.1905,
      "z": 0.1105,
      "orientation": 0.0,
      "radius": 0.1105,
      "width": 0.087,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 61.0
    }
  ],
  "groups": [
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.1,
      "theta": 120.0,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 50.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.21,
      "theta": 45.0,
      "height": 0.0,
      "orientation": 45.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 50.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.21,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 50.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.21,
      "theta": -45.0,
      "height": 0.0,
      "orientation": -45.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 50.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.1,
      "theta": -120.0,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 50.0
    }
  ],
  "fused": [
    {
      "mode": -1,
      "rho": 0.19474,
      "theta": 41.88,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.21783,
      "theta": 31.86,
      "height": 0.0,
      "orientation": 50.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.23409,
      "theta": 19.98,
      "height": 0.0,
      "orientation": 30.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.2413,
      "theta": 5.95,
      "height": 0.0,
      "orientation": 10.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.2413,
      "theta": -5.95,
      "height": 0.0,
      "orientation": -10.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.23409,
      "theta": -19.98,
      "height": 0.0,
      "orientation": -30.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.21783,
      "theta": -31.86,
      "height": 0.0,
      "orientation": -50.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.19474,
      "theta": -41.88,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.19474,
      "theta": -138.12,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.21783,
      "theta": -148.14,
      "height": 0.0,
      "orientation": -130.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.23409,
      "theta": -160.02,
      "height": 0.0,
      "orientation": -150.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.2413,
      "theta": -174.05,
      "height": 0.0,
      "orientation": -170.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.2413,
      "theta": 174.05,
      "height": 0.0,
      "orientation": 170.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.23409,
      "theta": 160.02,
      "height": 0.0,
      "orientation": 150.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.21783,
      "theta": 148.14,
      "height": 0.0,
      "orientation": 130.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.19474,
      "theta": 138.12,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    }
  ],
  "fusionmode": 0,
  "scans": [
    {
      "mode": 0,
      "rays": 90,
      "rho": 0.2,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 180.0
    }
  ],
  "extra": {
    "MAXDSIG": "1",
    "SERBRATE": "9600",
    "dsiglen0": "0.25",
    "GPS0": "devices.drivers.gps.Garmin.Garmin|/dev/tty.usbserial2",
    "ERRORLRFGAUSS": "0.0009",
    "SENSIBSON": "0.0000001",
    "dsigfeat0": "0.0",
    "V3DCOLORR": "0.6",
    "dsigrho0": "0.0",
    "V3DCOLORB": "0.0",
    "V3DCOLORG": "0.6",
    "SERPORT": "/dev/tty.usbserial",
    "MAXGPS": "0"
  }
}
