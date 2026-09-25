{
  "name": "iFork",
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
    "vmax": 0.0,
    "umax": 0.0,
    "rmax": 0.0,
    "lamax": 0.5,
    "ldmax": 0.2,
    "rwheel": 0.0,
    "skid": 1.0,
    "gear": 0.0,
    "pulses": 0.0,
    "odomET": 0.002,
    "odomER": 0.005,
    "odomBias": 0.001
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
      "cycle": 6,
      "simmode": 2,
      "simerror": 0.05,
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
      "simmode": 2,
      "simerror": 0.05,
      "sensors": [
        {
          "rho": 0.6113382381516156,
          "theta": 0.0,
          "height": 2.681921007325325,
          "orientation": 0.0,
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
    },
    "camera": {
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
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": false,
      "maxrpm": 0.0
    },
    {
      "x": 0.0,
      "y": 0.425,
      "z": 0.1,
      "orientation": 0.0,
      "radius": 0.1,
      "width": 0.082,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": false,
      "maxrpm": 0.0
    },
    {
      "x": 1.004,
      "y": 0.0,
      "z": 0.125,
      "orientation": -3.1532500071902073E-15,
      "radius": 0.125,
      "width": 0.082,
      "steerable": true,
      "maxsteer": 60.0,
      "maxturning": 42.0,
      "traction": true,
      "maxrpm": 191.0
    }
  ],
  "groups": [
    {
      "mode": 6,
      "base": 0.9,
      "rho": 1.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 2.0,
      "rangemin": 0.0,
      "cone": 45.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 1.2,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 45.0,
      "elevation": 0.0,
      "rangemax": 4.0,
      "rangemin": 0.0,
      "cone": 45.0
    },
    {
      "mode": 6,
      "base": 0.75,
      "rho": 1.8,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 4.0,
      "rangemin": 0.0,
      "cone": 45.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 1.2,
      "theta": 0.0,
      "height": 0.0,
      "orientation": -45.0,
      "elevation": 0.0,
      "rangemax": 4.0,
      "rangemin": 0.0,
      "cone": 45.0
    },
    {
      "mode": 6,
      "base": 0.9,
      "rho": 1.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 2.0,
      "rangemin": 0.0,
      "cone": 45.0
    }
  ],
  "fusionmode": 0,
  "scans": [
    {
      "mode": 0,
      "rays": 90,
      "rho": 1.135,
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
    "CAN_LIGHTSONID": "1800",
    "CAN_SECURITYID": "200",
    "CAN_OCR": "250",
    "CAN_FORKID": "1400",
    "CAN_DEBUGID": "111",
    "CAN_MOTID": "800",
    "CAN_CDR": "192",
    "CAN_ODOM_MOTID": "1000",
    "ERRORLRFGAUSS": "0.0009",
    "CAN_SINGLE_FILTER": "false",
    "LAYER_1": "ZoneB",
    "LAYER_0": "ZoneA",
    "LAYER_4": "RoomA",
    "LAYER_3": "ZoneD",
    "LAYER_2": "ZoneC",
    "CAN_BRATE": "500",
    "CAN_ACTIVEID": "400",
    "CAN_BRAKEID": "300",
    "CAN_DEV": "/dev/can1",
    "INITLAYER": "0",
    "CAN_HORNID": "1600",
    "ERRORLSB": "0.05",
    "ERRORLSBGAUSS": "0.5",
    "MAXLAYER": "5",
    "LSB0_OFFSET": "180.0",
    "DEBUG": "false",
    "CAN_LIGHTSOFFID": "2000",
    "CAN_ODOM_POSID": "1200",
    "DEBUG_CAN": "false"
  }
}
