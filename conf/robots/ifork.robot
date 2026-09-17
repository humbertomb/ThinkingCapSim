{
  "name": "ifork",
  "radius": 1.0,
  "icon": [
    {
      "xi": -1.1199,
      "yi": 0.2718,
      "xf": -1.1199,
      "yf": 0.2182
    },
    {
      "xi": -1.1199,
      "yi": 0.2718,
      "xf": -0.9207,
      "yf": 0.295
    },
    {
      "xi": -0.9207,
      "yi": 0.195,
      "xf": -1.1199,
      "yf": 0.2182
    },
    {
      "xi": -0.9207,
      "yi": -0.295,
      "xf": -1.1199,
      "yf": -0.2718
    },
    {
      "xi": -1.1199,
      "yi": -0.2182,
      "xf": -0.9207,
      "yf": -0.195
    },
    {
      "xi": -1.1199,
      "yi": -0.2182,
      "xf": -1.1199,
      "yf": -0.2718
    },
    {
      "xi": 1.375,
      "yi": 0.0,
      "xf": 1.3643,
      "yf": -0.0775
    },
    {
      "xi": 1.3643,
      "yi": 0.0775,
      "xf": 1.375,
      "yf": 0.0
    },
    {
      "xi": -0.086,
      "yi": 0.295,
      "xf": -0.086,
      "yf": 0.49
    },
    {
      "xi": -0.0885,
      "yi": -0.195,
      "xf": -0.0885,
      "yf": 0.195
    },
    {
      "xi": 0.468,
      "yi": 0.5,
      "xf": 1.0932,
      "yf": 0.5
    },
    {
      "xi": 1.0932,
      "yi": 0.5,
      "xf": 1.219,
      "yf": 0.3742
    },
    {
      "xi": 1.219,
      "yi": 0.3742,
      "xf": 1.219,
      "yf": -0.3742
    },
    {
      "xi": 1.0932,
      "yi": -0.5,
      "xf": 0.468,
      "yf": -0.5
    },
    {
      "xi": 1.219,
      "yi": -0.3742,
      "xf": 1.0932,
      "yf": -0.5
    },
    {
      "xi": 0.143,
      "yi": 0.375,
      "xf": 0.143,
      "yf": 0.49
    },
    {
      "xi": 0.143,
      "yi": -0.49,
      "xf": 0.143,
      "yf": -0.375
    },
    {
      "xi": -0.086,
      "yi": 0.49,
      "xf": 0.143,
      "yf": 0.49
    },
    {
      "xi": 0.143,
      "yi": -0.49,
      "xf": -0.086,
      "yf": -0.49
    },
    {
      "xi": 0.468,
      "yi": 0.375,
      "xf": 0.143,
      "yf": 0.375
    },
    {
      "xi": 0.468,
      "yi": 0.375,
      "xf": 0.468,
      "yf": 0.5
    },
    {
      "xi": 0.468,
      "yi": -0.5,
      "xf": 0.468,
      "yf": -0.3752
    },
    {
      "xi": 0.468,
      "yi": -0.3752,
      "xf": 0.143,
      "yf": -0.375
    },
    {
      "xi": -0.086,
      "yi": -0.295,
      "xf": -0.086,
      "yf": -0.49
    },
    {
      "xi": -0.086,
      "yi": 0.295,
      "xf": -0.9207,
      "yf": 0.295
    },
    {
      "xi": -0.9207,
      "yi": 0.195,
      "xf": -0.0885,
      "yf": 0.195
    },
    {
      "xi": -0.0885,
      "yi": -0.195,
      "xf": -0.9207,
      "yf": -0.195
    },
    {
      "xi": -0.9207,
      "yi": -0.295,
      "xf": -0.086,
      "yf": -0.295
    },
    {
      "xi": 1.3643,
      "yi": 0.0775,
      "xf": 1.219,
      "yf": 0.0775
    },
    {
      "xi": 1.219,
      "yi": -0.0775,
      "xf": 1.3643,
      "yf": -0.0775
    }
  ],
  "image": "./conf/2dmodels/ifork.png",
  "shapeRobot": "./conf/3dmodels/ifork.3ds",
  "shapeActuator": "./conf/3dmodels/ifork.lift.3ds",
  "kinematics": {
    "drive": "tc.vrobot.models.TricycleDrive",
    "vmax": 2.5,
    "rmax": 72.0,
    "maxmotor": 2.5,
    "maxsteer": 60.0,
    "samax": 42.0,
    "lamax": 0.5,
    "ldmax": 0.2,
    "length": 1.004,
    "base": 0.0,
    "rwheel": 0.0,
    "wheel": 0.0,
    "gear": 0.0,
    "pulses": 0.0,
    "dtime": 115,
    "odomET": 0.002,
    "odomER": 0.005,
    "odomBias": 0.001
  },
  "sensors": {
    "son": {
      "sensors": []
    },
    "ir": {
      "sensors": []
    },
    "lrf": {
      "cycle": 6,
      "sensors": [
        {
          "rho": 1.3063429017276762,
          "theta": 0.0,
          "height": 0.2652819949202241,
          "orientation": 0.0,
          "step": 4,
          "driver": "devices.drivers.laser.PLS.PLS",
          "driverParams": "/dev/ttyS0",
          "rangemax": 82.0,
          "rangemin": 0.01,
          "cone": 180.0,
          "rays": 361
        }
      ]
    },
    "lsb": {
      "cycle": 6,
      "sensors": [
        {
          "rho": 0.6113382381516156,
          "theta": -0.2576315714419668,
          "height": 2.681921007325325,
          "orientation": 2.8,
          "step": 1,
          "driver": "devices.drivers.beacon.nav200.NAV200",
          "driverParams": "/dev/ttyS1",
          "rangemax": 30.0,
          "rangemin": 0.01,
          "cone": 360.0,
          "rays": 361,
          "reflect": 10.0,
          "beacons": 20
        }
      ]
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
      "xi": -0.1,
      "yi": 0.5,
      "xf": -0.1,
      "yf": -0.5
    },
    {
      "xi": -0.1,
      "yi": -0.5,
      "xf": 1.35,
      "yf": -0.5
    },
    {
      "xi": 1.35,
      "yi": -0.5,
      "xf": 1.35,
      "yf": 0.5
    },
    {
      "xi": 1.35,
      "yi": 0.5,
      "xf": -0.1,
      "yf": 0.5
    }
  ],
  "wheels": [
    {
      "x": 0.0,
      "y": -0.425,
      "z": 0.10084648477721295,
      "orientation": 0.0,
      "radius": 0.1,
      "width": 0.082,
      "turnable": false,
      "traction": false
    },
    {
      "x": 0.0,
      "y": 0.425,
      "z": 0.1,
      "orientation": 0.0,
      "radius": 0.1,
      "width": 0.082,
      "turnable": false,
      "traction": false
    },
    {
      "x": 1.004,
      "y": 0.0,
      "z": 0.125,
      "orientation": -3.1532500071902073E-15,
      "radius": 0.125,
      "width": 0.082,
      "turnable": true,
      "traction": true
    }
  ],
  "extra": {
    "grouplen4": "1.0",
    "grouplen2": "1.8",
    "grouplen3": "1.2",
    "grouplen0": "1.0",
    "grouplen1": "1.2",
    "CAN_LIGHTSONID": "1800",
    "CONESCAN": "180",
    "RAYSCAN": "90",
    "CAN_SECURITYID": "200",
    "RANGESCAN": "10",
    "RANGEGROUP": "4.00",
    "CAN_OCR": "250",
    "CAN_FORKID": "1400",
    "CAN_DEBUGID": "111",
    "CAN_MOTID": "800",
    "CAN_CDR": "192",
    "scanrho": "0.0",
    "CAN_ODOM_MOTID": "1000",
    "groupfeat0": "90.0",
    "ERRORLRFGAUSS": "0.0009",
    "CAN_SINGLE_FILTER": "false",
    "groupmode2": "6",
    "groupmode3": "4",
    "groupmode4": "6",
    "LAYER_1": "ZoneB",
    "LAYER_0": "ZoneA",
    "groupfeat2": "0.0",
    "groupfeat1": "45.0",
    "LAYER_4": "RoomA",
    "LAYER_3": "ZoneD",
    "groupfeat4": "-90.0",
    "LAYER_2": "ZoneC",
    "groupfeat3": "-45.0",
    "grouprho3": "0.0",
    "groupbase2": "0.75",
    "grouprho2": "0.0",
    "CAN_BRATE": "500",
    "grouprho4": "0.0",
    "groupbase0": "0.9",
    "MAXGROUP": "5",
    "MODELSB": "2",
    "groupbase4": "0.9",
    "groupmode0": "6",
    "groupmode1": "4",
    "grouprho1": "0.0",
    "grouprho0": "0.0",
    "MODELRF": "2",
    "CAN_ACTIVEID": "400",
    "CAN_BRAKEID": "300",
    "grouprng4": "2.0",
    "CAN_DEV": "/dev/can1",
    "CONEGROUP": "45.0",
    "ERRORLRF": "0.05",
    "INITLAYER": "0",
    "CAN_HORNID": "1600",
    "grouprng0": "2.0",
    "scanmode": "0",
    "ERRORLSB": "0.05",
    "scanlen": "1.135",
    "ERRORLSBGAUSS": "0.5",
    "MAXLAYER": "5",
    "LSB0_OFFSET": "180.0",
    "scanfeat": "0.0",
    "DEBUG": "false",
    "CAN_LIGHTSOFFID": "2000",
    "CAN_ODOM_POSID": "1200",
    "DEBUG_CAN": "false"
  }
}
