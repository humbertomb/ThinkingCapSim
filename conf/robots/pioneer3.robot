{
  "name": "Pioneer-3 AT",
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
      "xf": 0.24439933031450173,
      "yf": 0.14687104813976298
    },
    {
      "xi": -0.24475878431632758,
      "yi": 0.14815515196045906,
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
      "xf": -0.24464011630643004,
      "yf": -0.14672486706692642
    },
    {
      "xi": 0.24441863999627625,
      "yi": -0.14687307005321407,
      "xf": 0.255,
      "yf": -0.07
    },
    {
      "xi": 0.24439933031450173,
      "yi": 0.14687104813976298,
      "xf": 0.24434462452781833,
      "yf": 0.23411194191825652
    },
    {
      "xi": 0.24434462452781833,
      "yi": 0.23411194191825652,
      "xf": -0.24444734871486704,
      "yf": 0.23405546934052374
    },
    {
      "xi": 0.24441863999627625,
      "yi": -0.14687307005321407,
      "xf": 0.24435620689613502,
      "yf": -0.23391332525473915
    },
    {
      "xi": 0.24435620689613502,
      "yi": -0.23391332525473915,
      "xf": -0.24423862362192056,
      "yf": -0.23398261050031258
    },
    {
      "xi": -0.24475878431632758,
      "yi": 0.14815515196045906,
      "xf": -0.24444734871486704,
      "yf": 0.23405546934052374
    },
    {
      "xi": -0.24464011630643004,
      "yi": -0.14672486706692642,
      "xf": -0.24423862362192056,
      "yf": -0.23398261050031258
    }
  ],
  "image": "./conf/2dmodels/pioneer3.png",
  "shapeRobot": "./conf/3dmodels/pioneer3at.3ds",
  "kinematics": {
    "drive": "tc.vrobot.models.SkidSteerDrive",
    "vmax": 0.0,
    "umax": 0.0,
    "rmax": 0.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "rwheel": 0.0,
    "skid": 1.5,
    "gear": 71.0,
    "pulses": 2000.0,
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
    "camera": {
      "sensors": []
    }
  },
  "bumpers": [
    {
      "xi": 0.24375749400520802,
      "yi": 0.14422099948939976,
      "xf": 0.2455581886816693,
      "yf": -0.14569084342086397
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
      "mode": 1,
      "rho": 0.0,
      "theta": 90.0,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.25,
      "cone": 45.0
    },
    {
      "mode": 1,
      "rho": 0.0,
      "theta": 45.0,
      "height": 0.0,
      "orientation": 45.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.25,
      "cone": 45.0
    },
    {
      "mode": 1,
      "rho": 0.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.25,
      "cone": 45.0
    },
    {
      "mode": 1,
      "rho": 0.0,
      "theta": -45.0,
      "height": 0.0,
      "orientation": -45.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.25,
      "cone": 45.0
    },
    {
      "mode": 1,
      "rho": 0.0,
      "theta": -90.0,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.25,
      "cone": 45.0
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
