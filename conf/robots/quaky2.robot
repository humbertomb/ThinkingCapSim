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
      "xi": 0.21,
      "yi": 0.11,
      "xf": 0.17200000000000001,
      "yf": 0.178
    },
    {
      "xi": 0.17200000000000001,
      "yi": 0.178,
      "xf": 0.10200000000000001,
      "yf": 0.218
    },
    {
      "xi": 0.10200000000000001,
      "yi": 0.218,
      "xf": -0.108,
      "yf": 0.216
    },
    {
      "xi": -0.108,
      "yi": 0.216,
      "xf": -0.178,
      "yf": 0.17400000000000002
    },
    {
      "xi": -0.178,
      "yi": 0.17400000000000002,
      "xf": -0.216,
      "yf": 0.106
    },
    {
      "xi": -0.216,
      "yi": 0.106,
      "xf": -0.214,
      "yf": -0.11
    },
    {
      "xi": -0.214,
      "yi": -0.11,
      "xf": -0.17400000000000002,
      "yf": -0.178
    },
    {
      "xi": -0.17400000000000002,
      "yi": -0.178,
      "xf": -0.10400000000000001,
      "yf": -0.218
    },
    {
      "xi": -0.10400000000000001,
      "yi": -0.218,
      "xf": 0.106,
      "yf": -0.216
    },
    {
      "xi": 0.106,
      "yi": -0.216,
      "xf": 0.17400000000000002,
      "yf": -0.17400000000000002
    },
    {
      "xi": 0.17400000000000002,
      "yi": -0.17400000000000002,
      "xf": 0.214,
      "yf": -0.106
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
    "maxsteer": 0.0,
    "samax": 0.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "length": 0.0,
    "rwheel": 0.0,
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
          "rho": 0.21998636321372286,
          "theta": 14.478429238377414,
          "height": 0.2575,
          "orientation": 0.0,
          "step": 1
        },
        {
          "rho": 0.2386566571457834,
          "theta": 35.621402690266684,
          "height": 0.2575,
          "orientation": 22.5,
          "step": 2
        },
        {
          "rho": 0.2407405242164269,
          "theta": 56.17791478250606,
          "height": 0.2575,
          "orientation": 67.5,
          "step": 1
        },
        {
          "rho": 0.22322186272854186,
          "theta": 77.58255420992374,
          "height": 0.2575,
          "orientation": 90.0,
          "step": 2
        },
        {
          "rho": 0.22224535990656824,
          "theta": 102.47288212465472,
          "height": 0.2575,
          "orientation": 90.0,
          "step": 1
        },
        {
          "rho": 0.24111564512024447,
          "theta": 125.21088960276339,
          "height": 0.2575,
          "orientation": 112.5,
          "step": 2
        },
        {
          "rho": 0.24194796287356213,
          "theta": 144.93511655600776,
          "height": 0.2575,
          "orientation": 157.5,
          "step": 1
        },
        {
          "rho": 0.22289235069871743,
          "theta": 165.7143749534052,
          "height": 0.2575,
          "orientation": 180.0,
          "step": 2
        },
        {
          "rho": 0.22192341021172146,
          "theta": -165.65066795705286,
          "height": 0.2575,
          "orientation": -180.0,
          "step": 1
        },
        {
          "rho": 0.24110163831878043,
          "theta": -144.79382735844757,
          "height": 0.2575,
          "orientation": -157.5,
          "step": 2
        },
        {
          "rho": 0.2421811423991448,
          "theta": -124.74476745383777,
          "height": 0.2575,
          "orientation": -112.5,
          "step": 1
        },
        {
          "rho": 0.22273358525377354,
          "theta": -102.44510319673192,
          "height": 0.2575,
          "orientation": -90.0,
          "step": 2
        },
        {
          "rho": 0.22224535990656813,
          "theta": -77.52711787534535,
          "height": 0.2575,
          "orientation": -90.0,
          "step": 1
        },
        {
          "rho": 0.23995416228938393,
          "theta": -55.1840331960241,
          "height": 0.2575,
          "orientation": -67.5,
          "step": 2
        },
        {
          "rho": 0.23947024867402633,
          "theta": -35.48205083905124,
          "height": 0.2575,
          "orientation": -22.5,
          "step": 1
        },
        {
          "rho": 0.2209547464980103,
          "theta": -14.413597701184932,
          "height": 0.2575,
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
          "rho": 0.21998636321372286,
          "theta": 14.478429238377414,
          "height": 0.2125,
          "orientation": 0.0,
          "step": 1
        },
        {
          "rho": 0.2386566571457834,
          "theta": 35.621402690266684,
          "height": 0.2125,
          "orientation": 22.5,
          "step": 2
        },
        {
          "rho": 0.24074052421642675,
          "theta": 56.17791478250606,
          "height": 0.2125,
          "orientation": 67.5,
          "step": 1
        },
        {
          "rho": 0.2232218627285419,
          "theta": 77.5825542099237,
          "height": 0.2125,
          "orientation": 90.0,
          "step": 2
        },
        {
          "rho": 0.22224535990656824,
          "theta": 102.47288212465472,
          "height": 0.2125,
          "orientation": 90.0,
          "step": 1
        },
        {
          "rho": 0.24110163831878043,
          "theta": 125.20617264155243,
          "height": 0.2125,
          "orientation": 112.5,
          "step": 2
        },
        {
          "rho": 0.24191940806805892,
          "theta": 144.93036958204323,
          "height": 0.2125,
          "orientation": 157.5,
          "step": 1
        },
        {
          "rho": 0.22289235069871746,
          "theta": 165.7143749534052,
          "height": 0.2125,
          "orientation": 180.0,
          "step": 2
        },
        {
          "rho": 0.22192341021172146,
          "theta": -165.65066795705286,
          "height": 0.2125,
          "orientation": -180.0,
          "step": 1
        },
        {
          "rho": 0.24110163831878043,
          "theta": -144.79382735844757,
          "height": 0.2125,
          "orientation": -157.5,
          "step": 2
        },
        {
          "rho": 0.24216729754448682,
          "theta": -124.74004444357857,
          "height": 0.2125,
          "orientation": -112.5,
          "step": 1
        },
        {
          "rho": 0.22273358525377368,
          "theta": -102.44510319673206,
          "height": 0.2125,
          "orientation": -90.0,
          "step": 2
        },
        {
          "rho": 0.22224535990656813,
          "theta": -77.52711787534535,
          "height": 0.2125,
          "orientation": -90.0,
          "step": 1
        },
        {
          "rho": 0.23995416228938393,
          "theta": -55.1840331960241,
          "height": 0.2125,
          "orientation": -67.5,
          "step": 2
        },
        {
          "rho": 0.23947024867402633,
          "theta": -35.48205083905124,
          "height": 0.2125,
          "orientation": -22.5,
          "step": 1
        },
        {
          "rho": 0.2209547464980103,
          "theta": -14.413597701184939,
          "height": 0.2125,
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
          "rho": 0.15000000000000002,
          "theta": -0.0,
          "height": 0.43,
          "orientation": 0.0,
          "elevation": -15.0,
          "step": 1,
          "driver": "devices.drivers.vision.quaky2.Quaky2Vis",
          "driverParams": "5,7000,10.0.0.1:8000",
          "rangemax": 11.668769833972743,
          "hfov": 60.0,
          "vfov": 50.0
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
  "wheels": [
    {
      "x": 0.0,
      "y": -0.1825,
      "z": 0.07435,
      "orientation": 0.0,
      "radius": 0.07435,
      "width": 0.038,
      "steerable": false,
      "maxsteer": 0.0,
      "traction": true,
      "maxrpm": 54.0
    },
    {
      "x": 0.0,
      "y": 0.1825,
      "z": 0.07435,
      "orientation": -2.2579142478972982E-14,
      "radius": 0.07435,
      "width": 0.038,
      "steerable": false,
      "maxsteer": 0.0,
      "traction": true,
      "maxrpm": 54.0
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
