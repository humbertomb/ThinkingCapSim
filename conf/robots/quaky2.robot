{
  "name": "quaky2",
  "radius": 0.25,
  "icon": [
    {
      "xi": 0.202,
      "yi": -0.097,
      "xf": 0.202,
      "yf": 0.097
    },
    {
      "xi": 0.202,
      "yi": 0.097,
      "xf": 0.17,
      "yf": 0.17
    },
    {
      "xi": 0.17,
      "yi": 0.17,
      "xf": 0.097,
      "yf": 0.202
    },
    {
      "xi": 0.097,
      "yi": 0.202,
      "xf": -0.097,
      "yf": 0.202
    },
    {
      "xi": -0.097,
      "yi": 0.202,
      "xf": -0.17,
      "yf": 0.17
    },
    {
      "xi": -0.17,
      "yi": 0.17,
      "xf": -0.202,
      "yf": 0.097
    },
    {
      "xi": -0.202,
      "yi": 0.097,
      "xf": -0.202,
      "yf": -0.097
    },
    {
      "xi": -0.202,
      "yi": -0.097,
      "xf": -0.17,
      "yf": -0.17
    },
    {
      "xi": -0.17,
      "yi": -0.17,
      "xf": -0.097,
      "yf": -0.202
    },
    {
      "xi": -0.097,
      "yi": -0.202,
      "xf": 0.097,
      "yf": -0.202
    },
    {
      "xi": 0.097,
      "yi": -0.202,
      "xf": 0.17,
      "yf": -0.17
    },
    {
      "xi": 0.17,
      "yi": -0.17,
      "xf": 0.202,
      "yf": -0.097
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
      "xi": 0.202,
      "yi": -0.15,
      "xf": 0.202,
      "yf": 0.15
    },
    {
      "xi": 0.202,
      "yi": 0.15,
      "xf": 0.242,
      "yf": 0.15
    },
    {
      "xi": 0.242,
      "yi": 0.15,
      "xf": 0.242,
      "yf": 0.0
    },
    {
      "xi": 0.242,
      "yi": 0.0,
      "xf": 0.242,
      "yf": -0.15
    },
    {
      "xi": 0.242,
      "yi": -0.15,
      "xf": 0.202,
      "yf": -0.15
    },
    {
      "xi": 0.242,
      "yi": 0.15,
      "xf": 0.262,
      "yf": 0.15
    },
    {
      "xi": 0.262,
      "yi": 0.15,
      "xf": 0.297,
      "yf": 0.17
    },
    {
      "xi": 0.242,
      "yi": -0.15,
      "xf": 0.262,
      "yf": -0.15
    },
    {
      "xi": 0.262,
      "yi": -0.15,
      "xf": 0.297,
      "yf": -0.17
    }
  ],
  "shapeRobot": "./conf/3dmodels/quaky2.3ds",
  "kinematics": {
    "drive": "tc.vrobot.models.DifferentialDrive",
    "vmax": 0.42,
    "rmax": 110.0,
    "maxmotor": 0.42,
    "maxsteer": 0.0,
    "samax": 0.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "length": 0.0,
    "base": 0.365,
    "rwheel": 0.0,
    "wheel": 0.1487,
    "gear": 71.0,
    "pulses": 2000.0,
    "dtime": 100,
    "odomET": 0.0,
    "odomER": 0.0,
    "odomBias": 0.0
  },
  "sensors": {
    "son": {
      "rangemax": 5.0,
      "rangemin": 0.135,
      "cone": 20.0,
      "rays": 11,
      "cycle": 2,
      "sensors": [
        {
          "rho": 0.20774,
          "theta": 13.501,
          "height": 0.0,
          "orientation": 0.0,
          "step": 1
        },
        {
          "rho": 0.22697,
          "theta": 34.9506,
          "height": 0.0,
          "orientation": 22.5,
          "step": 2
        },
        {
          "rho": 0.22697,
          "theta": 55.0493,
          "height": 0.0,
          "orientation": 67.5,
          "step": 1
        },
        {
          "rho": 0.20774,
          "theta": 76.4989,
          "height": 0.0,
          "orientation": 90.0,
          "step": 2
        },
        {
          "rho": 0.20774,
          "theta": 103.501,
          "height": 0.0,
          "orientation": 90.0,
          "step": 1
        },
        {
          "rho": 0.22697,
          "theta": 124.9506,
          "height": 0.0,
          "orientation": 112.5,
          "step": 2
        },
        {
          "rho": 0.22697,
          "theta": 145.0493,
          "height": 0.0,
          "orientation": 157.5,
          "step": 1
        },
        {
          "rho": 0.20774,
          "theta": 166.4989,
          "height": 0.0,
          "orientation": 180.0,
          "step": 2
        },
        {
          "rho": 0.20774,
          "theta": -166.4989,
          "height": 0.0,
          "orientation": -180.0,
          "step": 1
        },
        {
          "rho": 0.22697,
          "theta": -145.0493,
          "height": 0.0,
          "orientation": -157.5,
          "step": 2
        },
        {
          "rho": 0.22697,
          "theta": -124.9506,
          "height": 0.0,
          "orientation": -112.5,
          "step": 1
        },
        {
          "rho": 0.20774,
          "theta": -103.501,
          "height": 0.0,
          "orientation": -90.0,
          "step": 2
        },
        {
          "rho": 0.20774,
          "theta": -76.4989,
          "height": 0.0,
          "orientation": -90.0,
          "step": 1
        },
        {
          "rho": 0.22697,
          "theta": -55.0493,
          "height": 0.0,
          "orientation": -67.5,
          "step": 2
        },
        {
          "rho": 0.22697,
          "theta": -34.9506,
          "height": 0.0,
          "orientation": -22.5,
          "step": 1
        },
        {
          "rho": 0.20774,
          "theta": -13.501,
          "height": 0.0,
          "orientation": 0.0,
          "step": 2
        }
      ]
    },
    "ir": {
      "rangemax": 0.8,
      "rangemin": 0.1,
      "cone": 10.0,
      "rays": 5,
      "cycle": 2,
      "sensors": [
        {
          "rho": 0.20774,
          "theta": 13.501,
          "height": 0.0,
          "orientation": 0.0,
          "step": 1
        },
        {
          "rho": 0.22697,
          "theta": 34.9506,
          "height": 0.0,
          "orientation": 22.5,
          "step": 2
        },
        {
          "rho": 0.22697,
          "theta": 55.0493,
          "height": 0.0,
          "orientation": 67.5,
          "step": 1
        },
        {
          "rho": 0.20774,
          "theta": 76.4989,
          "height": 0.0,
          "orientation": 90.0,
          "step": 2
        },
        {
          "rho": 0.20774,
          "theta": 103.501,
          "height": 0.0,
          "orientation": 90.0,
          "step": 1
        },
        {
          "rho": 0.22697,
          "theta": 124.9506,
          "height": 0.0,
          "orientation": 112.5,
          "step": 2
        },
        {
          "rho": 0.22697,
          "theta": 145.0493,
          "height": 0.0,
          "orientation": 157.5,
          "step": 1
        },
        {
          "rho": 0.20774,
          "theta": 166.4989,
          "height": 0.0,
          "orientation": 180.0,
          "step": 2
        },
        {
          "rho": 0.20774,
          "theta": -166.4989,
          "height": 0.0,
          "orientation": -180.0,
          "step": 1
        },
        {
          "rho": 0.22697,
          "theta": -145.0493,
          "height": 0.0,
          "orientation": -157.5,
          "step": 2
        },
        {
          "rho": 0.22697,
          "theta": -124.9506,
          "height": 0.0,
          "orientation": -112.5,
          "step": 1
        },
        {
          "rho": 0.20774,
          "theta": -103.501,
          "height": 0.0,
          "orientation": -90.0,
          "step": 2
        },
        {
          "rho": 0.20774,
          "theta": -76.4989,
          "height": 0.0,
          "orientation": -90.0,
          "step": 1
        },
        {
          "rho": 0.22697,
          "theta": -55.0493,
          "height": 0.0,
          "orientation": -67.5,
          "step": 2
        },
        {
          "rho": 0.22697,
          "theta": -34.9506,
          "height": 0.0,
          "orientation": -22.5,
          "step": 1
        },
        {
          "rho": 0.20774,
          "theta": -13.501,
          "height": 0.0,
          "orientation": 0.0,
          "step": 2
        }
      ]
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
      "cycle": 1,
      "sensors": [
        {
          "rho": 0.0,
          "theta": 0.0,
          "height": 0.35,
          "orientation": 0.0,
          "step": 1,
          "driver": "devices.drivers.vision.quaky2.Quaky2Vis|5,7000,10.0.0.1:8000",
          "hfov": 60.0
        }
      ]
    }
  },
  "bumpers": [
    {
      "xi": 0.21,
      "yi": 0.21,
      "xf": 0.21,
      "yf": -0.21
    },
    {
      "xi": 0.21,
      "yi": -0.21,
      "xf": -0.21,
      "yf": -0.21
    },
    {
      "xi": -0.21,
      "yi": -0.21,
      "xf": -0.21,
      "yf": 0.21
    },
    {
      "xi": -0.21,
      "yi": 0.21,
      "xf": 0.21,
      "yf": 0.21
    }
  ],
  "extra": {
    "CAN_S_BUMID": "250",
    "MAXDSIG": "1",
    "grouplen4": "0.10",
    "grouplen2": "0.21",
    "grouplen3": "0.21",
    "grouplen0": "0.10",
    "grouplen1": "0.21",
    "MODEVIRTU": "4",
    "RANGEVIRTU": "6.0",
    "dsiglen0": "0.25",
    "RANGEGROUP": "0.75",
    "CAN_OCR": "250",
    "virtulen14": "0.22697",
    "CONEVIRTU": "17",
    "virtulen15": "0.20774",
    "virtulen10": "0.22697",
    "virtulen11": "0.20774",
    "virtulen12": "0.20774",
    "virtulen13": "0.22697",
    "CAN_S_IRID": "208",
    "CAN_S_FIREID": "160",
    "CAN_M_ODOMID": "98",
    "virtufeat10": "-112.5",
    "CAN_CDR": "192",
    "virtufeat11": "-90",
    "virtufeat12": "-90",
    "virtufeat13": "-67.5",
    "virtufeat14": "-22.5",
    "virtufeat15": "0",
    "ERRORVIS": "0",
    "groupfeat0": "90.0",
    "groupmode2": "4",
    "CAN_SINGLE_FILTER": "false",
    "groupmode3": "4",
    "groupmode4": "4",
    "groupfeat2": "0.0",
    "groupfeat1": "45.0",
    "groupfeat4": "-90.0",
    "groupfeat3": "-45.0",
    "grouprho3": "-45.0",
    "CAN_S_SONID": "192",
    "grouprho2": "0.0",
    "MAXGROUP": "5",
    "grouprho4": "-120.0",
    "CAN_BRATE": "500",
    "groupmode0": "4",
    "groupmode1": "4",
    "grouprho1": "45.0",
    "grouprho0": "120.0",
    "MODESON": "2",
    "CAN_M_SETID": "96",
    "virturho0": "13.5010",
    "virturho6": "145.0493",
    "virturho5": "124.9506",
    "virturho8": "-166.4989",
    "virturho7": "166.4989",
    "virturho2": "55.0493",
    "virturho1": "34.9506",
    "virturho4": "103.5010",
    "virturho3": "76.4989",
    "virturho9": "-145.0493",
    "CONEGROUP": "50",
    "CAN_DEV": "/dev/can1",
    "CAN_M_CFGID": "97",
    "SENSIBSON": "0.0000001",
    "ERRORSON": "0.05",
    "virturho12": "-76.4989",
    "virturho13": "-55.0493",
    "virturho10": "-124.9506",
    "virturho11": "-103.5010",
    "virturho14": "-34.9506",
    "virturho15": "-13.5010",
    "dsigfeat0": "0.0",
    "virtufeat2": "67.5",
    "virtulen0": "0.20774",
    "virtufeat3": "90",
    "virtufeat4": "90",
    "virtufeat5": "112.5",
    "virtulen3": "0.20774",
    "virtufeat6": "157.5",
    "virtulen4": "0.20774",
    "virtufeat7": "180",
    "virtulen1": "0.22697",
    "virtufeat8": "-180",
    "virtulen2": "0.22697",
    "virtufeat9": "-157.5",
    "virtulen7": "0.20774",
    "virtulen8": "0.20774",
    "virtulen5": "0.22697",
    "virtulen6": "0.22697",
    "V3DCOLORR": "0.6",
    "RAYVIRTU": "15",
    "virtufeat0": "0",
    "virtulen9": "0.22697",
    "virtufeat1": "22.5",
    "dsigrho0": "0.0",
    "LCD": "/dev/ttyS0",
    "V3DCOLORB": "0.0",
    "ERRORIR": "0.05",
    "V3DCOLORG": "0.6",
    "MAXVIRTU": "16",
    "MODEIR": "2",
    "DEBUG": "false",
    "DEBUG_CAN": "false"
  }
}
