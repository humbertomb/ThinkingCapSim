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
    "lamax": 0.0,
    "ldmax": 0.0,
    "rwheel": 0.0,
    "skid": 1.0,
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
      "simmode": 2,
      "simerror": 0.05,
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
      "simmode": 2,
      "simerror": 0.05,
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
      "simerror": 0.05,
      "sensors": []
    },
    "lsb": {
      "simerror": 0.05,
      "sensors": []
    },
    "trk": {
      "sensors": []
    },
    "vis": {
      "cycle": 1,
      "sensors": [
        {
          "rho": 0.12536540809227956,
          "theta": -0.0,
          "height": 0.39937321006067117,
          "orientation": 0.0,
          "elevation": -23.0,
          "step": 1,
          "driver": "devices.drivers.vision.quaky2.Quaky2Vis",
          "driverParams": "5,7000,10.0.0.1:8000",
          "rangemax": 11.668769833972743,
          "hfov": 70.0,
          "vfov": 43.0
        }
      ]
    },
    "camera": {
      "sensors": [
        {
          "rho": 0.12536540809227956,
          "theta": 0.0,
          "height": 0.40070480962325067,
          "orientation": 0.0,
          "elevation": -25.0,
          "step": 0,
          "rangemax": 10.0,
          "hfov": 70.0,
          "vfov": 43.0,
          "framerate": 5.0,
          "resolution": "800x600"
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
      "maxturning": 0.0,
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
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 54.0
    }
  ],
  "groups": [
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.20384852240668122,
      "theta": 104.00807143739657,
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
      "rho": 0.24823666925837046,
      "theta": 45.33743638103079,
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
      "rho": 0.24277787086820887,
      "theta": -44.70099762041784,
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
      "rho": 0.2115730753843907,
      "theta": -103.75562084558092,
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
      "rho": 0.20774,
      "theta": 13.501,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.22697,
      "theta": 34.9506,
      "height": 0.0,
      "orientation": 22.5,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.22697,
      "theta": 55.0493,
      "height": 0.0,
      "orientation": 67.5,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.20774,
      "theta": 76.4989,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.20774,
      "theta": 103.501,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.22697,
      "theta": 124.9506,
      "height": 0.0,
      "orientation": 112.5,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.22697,
      "theta": 145.0493,
      "height": 0.0,
      "orientation": 157.5,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.20774,
      "theta": 166.4989,
      "height": 0.0,
      "orientation": 180.0,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.20774,
      "theta": -166.4989,
      "height": 0.0,
      "orientation": -180.0,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.22697,
      "theta": -145.0493,
      "height": 0.0,
      "orientation": -157.5,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.22697,
      "theta": -124.9506,
      "height": 0.0,
      "orientation": -112.5,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.20774,
      "theta": -103.501,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.20774,
      "theta": -76.4989,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.22697,
      "theta": -55.0493,
      "height": 0.0,
      "orientation": -67.5,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.22697,
      "theta": -34.9506,
      "height": 0.0,
      "orientation": -22.5,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.20774,
      "theta": -13.501,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 6.0,
      "rangemin": 0.0,
      "cone": 17.0
    }
  ],
  "fusionmode": 4,
  "extra": {
    "CAN_S_BUMID": "250",
    "MAXDSIG": "1",
    "dsiglen0": "0.25",
    "CAN_OCR": "250",
    "CAN_S_IRID": "208",
    "CAN_S_FIREID": "160",
    "CAN_M_ODOMID": "98",
    "CAN_CDR": "192",
    "ERRORVIS": "0",
    "CAN_SINGLE_FILTER": "false",
    "CAN_S_SONID": "192",
    "CAN_BRATE": "500",
    "CAN_M_SETID": "96",
    "CAN_DEV": "/dev/can1",
    "CAN_M_CFGID": "97",
    "SENSIBSON": "0.0000001",
    "dsigfeat0": "0.0",
    "V3DCOLORR": "0.6",
    "dsigrho0": "0.0",
    "LCD": "/dev/ttyS0",
    "V3DCOLORB": "0.0",
    "V3DCOLORG": "0.6",
    "DEBUG": "false",
    "DEBUG_CAN": "false"
  }
}
