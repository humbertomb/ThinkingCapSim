{
  "name": "Dreame X40",
  "icon": [
    {
      "xi": 0.175,
      "yi": 0.0,
      "xf": 0.169,
      "yf": 0.0453
    },
    {
      "xi": 0.169,
      "yi": 0.0453,
      "xf": 0.1516,
      "yf": 0.0875
    },
    {
      "xi": 0.1516,
      "yi": 0.0875,
      "xf": 0.1237,
      "yf": 0.1237
    },
    {
      "xi": 0.1237,
      "yi": 0.1237,
      "xf": 0.0875,
      "yf": 0.1516
    },
    {
      "xi": 0.0875,
      "yi": 0.1516,
      "xf": 0.0453,
      "yf": 0.169
    },
    {
      "xi": 0.0453,
      "yi": 0.169,
      "xf": 0.0,
      "yf": 0.175
    },
    {
      "xi": 0.0,
      "yi": 0.175,
      "xf": -0.0453,
      "yf": 0.169
    },
    {
      "xi": -0.0453,
      "yi": 0.169,
      "xf": -0.0875,
      "yf": 0.1516
    },
    {
      "xi": -0.0875,
      "yi": 0.1516,
      "xf": -0.1237,
      "yf": 0.1237
    },
    {
      "xi": -0.1237,
      "yi": 0.1237,
      "xf": -0.1516,
      "yf": 0.0875
    },
    {
      "xi": -0.1516,
      "yi": 0.0875,
      "xf": -0.169,
      "yf": 0.0453
    },
    {
      "xi": -0.169,
      "yi": 0.0453,
      "xf": -0.175,
      "yf": 0.0
    },
    {
      "xi": -0.175,
      "yi": 0.0,
      "xf": -0.169,
      "yf": -0.0453
    },
    {
      "xi": -0.169,
      "yi": -0.0453,
      "xf": -0.1516,
      "yf": -0.0875
    },
    {
      "xi": -0.1516,
      "yi": -0.0875,
      "xf": -0.1237,
      "yf": -0.1237
    },
    {
      "xi": -0.1237,
      "yi": -0.1237,
      "xf": -0.0875,
      "yf": -0.1516
    },
    {
      "xi": -0.0875,
      "yi": -0.1516,
      "xf": -0.0453,
      "yf": -0.169
    },
    {
      "xi": -0.0453,
      "yi": -0.169,
      "xf": 0.0,
      "yf": -0.175
    },
    {
      "xi": 0.0,
      "yi": -0.175,
      "xf": 0.0453,
      "yf": -0.169
    },
    {
      "xi": 0.0453,
      "yi": -0.169,
      "xf": 0.0875,
      "yf": -0.1516
    },
    {
      "xi": 0.0875,
      "yi": -0.1516,
      "xf": 0.1237,
      "yf": -0.1237
    },
    {
      "xi": 0.1237,
      "yi": -0.1237,
      "xf": 0.1516,
      "yf": -0.0875
    },
    {
      "xi": 0.1516,
      "yi": -0.0875,
      "xf": 0.169,
      "yf": -0.0453
    },
    {
      "xi": 0.169,
      "yi": -0.0453,
      "xf": 0.175,
      "yf": 0.0
    }
  ],
  "image": "./conf/2dmodels/dreamex40.png",
  "shapeRobot": "./conf/3dmodels/dreamex40.3ds",
  "kinematics": {
    "drive": "tc.vrobot.models.DifferentialDrive",
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
      "cycle": 1,
      "simerror": 0.05,
      "sensors": [
        {
          "rho": 0.035,
          "theta": -0.0,
          "height": 0.1,
          "orientation": 0.0,
          "step": 1,
          "rangemax": 8.0,
          "rangemin": 0.1,
          "cone": 360.0,
          "rays": 360
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
      "xi": 0.124992722266565,
      "yi": 0.12392274159420649,
      "xf": 0.030354747754659742,
      "yf": 0.1728512182126065
    },
    {
      "xi": 0.17070222016007028,
      "yi": 0.0505300266666065,
      "xf": 0.13,
      "yf": 0.12
    },
    {
      "xi": 0.17,
      "yi": -0.05,
      "xf": 0.17070222016007028,
      "yf": 0.0505300266666065
    },
    {
      "xi": 0.12,
      "yi": -0.13,
      "xf": 0.17,
      "yf": -0.05
    },
    {
      "xi": 0.03,
      "yi": -0.17500000000000002,
      "xf": 0.12,
      "yf": -0.13
    }
  ],
  "wheels": [
    {
      "x": 0.0,
      "y": -0.118,
      "z": 0.034,
      "orientation": 0.0,
      "radius": 0.034,
      "width": 0.022,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 84.0
    },
    {
      "x": 0.0,
      "y": 0.118,
      "z": 0.034,
      "orientation": 0.0,
      "radius": 0.034,
      "width": 0.022,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 84.0
    }
  ],
  "groups": [
    {
      "mode": 0,
      "rho": 0.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 1.0,
      "rangemin": 0.18,
      "cone": 45.0
    },
    {
      "mode": 0,
      "rho": 0.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 45.0,
      "elevation": 0.0,
      "rangemax": 1.0,
      "rangemin": 0.18,
      "cone": 45.0
    },
    {
      "mode": 0,
      "rho": 0.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 1.0,
      "rangemin": 0.18,
      "cone": 45.0
    },
    {
      "mode": 0,
      "rho": 0.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": -45.0,
      "elevation": 0.0,
      "rangemax": 1.0,
      "rangemin": 0.18,
      "cone": 45.0
    },
    {
      "mode": 0,
      "rho": 0.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 1.0,
      "rangemin": 0.18,
      "cone": 45.0
    }
  ],
  "fusionmode": 0,
  "scans": [
    {
      "mode": 1,
      "rays": 90,
      "rho": 0.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 180.0
    }
  ],
  "extra": {}
}
