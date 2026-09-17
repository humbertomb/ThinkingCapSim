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
      "sensors": []
    },
    "lrf": {
      "cycle": 6,
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
  "extra": {
    "MAXDSIG": "1",
    "grouplen4": "0.10",
    "grouplen2": "0.21",
    "grouplen3": "0.21",
    "grouplen0": "0.10",
    "grouplen1": "0.21",
    "SERBRATE": "9600",
    "CONESCAN": "180",
    "MODEVIRTU": "0",
    "RAYSCAN": "90",
    "RANGEVIRTU": "5.0",
    "RANGESCAN": "10",
    "dsiglen0": "0.25",
    "RANGEGROUP": "0.75",
    "virtulen14": "0.21783",
    "CONEVIRTU": "17",
    "virtulen15": "0.19474",
    "virtulen10": "0.23409",
    "virtulen11": "0.24130",
    "virtulen12": "0.24130",
    "virtulen13": "0.23409",
    "virtufeat10": "-150",
    "virtufeat11": "-170",
    "virtufeat12": "170",
    "virtufeat13": "150",
    "virtufeat14": "130",
    "virtufeat15": "90",
    "scanrho": "0.0",
    "GPS0": "devices.drivers.gps.Garmin.Garmin|/dev/tty.usbserial2",
    "groupfeat0": "90.0",
    "ERRORLRFGAUSS": "0.0009",
    "groupmode2": "4",
    "groupmode3": "4",
    "groupmode4": "4",
    "groupfeat2": "0.0",
    "groupfeat1": "45.0",
    "groupfeat4": "-90.0",
    "groupfeat3": "-45.0",
    "grouprho3": "-45.0",
    "grouprho2": "0.0",
    "MAXGROUP": "5",
    "grouprho4": "-120.0",
    "groupmode0": "4",
    "groupmode1": "4",
    "grouprho1": "45.0",
    "grouprho0": "120.0",
    "MODESON": "2",
    "MODELRF": "2",
    "virturho0": "41.88",
    "virturho6": "-31.86",
    "virturho5": "-19.98",
    "virturho8": "-138.12",
    "virturho7": "-41.88",
    "virturho2": "19.98",
    "virturho1": "31.86",
    "virturho4": "-5.95",
    "virturho3": "5.95",
    "CONEGROUP": "50",
    "ERRORLRF": "0.05",
    "virturho9": "-148.14",
    "SENSIBSON": "0.0000001",
    "ERRORSON": "0.05",
    "scanmode": "0",
    "virturho12": "174.05",
    "virturho13": "160.02",
    "virturho10": "-160.02",
    "virturho11": "-174.05",
    "virturho14": "148.14",
    "virturho15": "138.12",
    "dsigfeat0": "0.0",
    "virtufeat2": "30",
    "virtulen0": "0.19474",
    "virtufeat3": "10",
    "virtufeat4": "-10",
    "virtufeat5": "-30",
    "virtufeat6": "-50",
    "virtulen3": "0.24130",
    "virtulen4": "0.24130",
    "virtufeat7": "-90",
    "virtufeat8": "-90",
    "virtulen1": "0.21783",
    "scanlen": "0.200",
    "virtulen2": "0.23409",
    "virtufeat9": "-130",
    "virtulen7": "0.19474",
    "virtulen8": "0.19474",
    "virtulen5": "0.23409",
    "virtulen6": "0.21783",
    "V3DCOLORR": "0.6",
    "RAYVIRTU": "15",
    "virtulen9": "0.21783",
    "virtufeat0": "90",
    "virtufeat1": "50",
    "dsigrho0": "0.0",
    "V3DCOLORB": "0.0",
    "V3DCOLORG": "0.6",
    "MAXVIRTU": "16",
    "SERPORT": "/dev/tty.usbserial",
    "scanfeat": "0.0",
    "MAXGPS": "0"
  }
}
