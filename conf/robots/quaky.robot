{
  "name": "Quaky",
  "radius": 0.3,
  "icon": [
    {
      "xi": -0.2325,
      "yi": 0.1525,
      "xf": 0.0925,
      "yf": 0.1525
    },
    {
      "xi": -0.2325,
      "yi": -0.1525,
      "xf": 0.0925,
      "yf": -0.1525
    },
    {
      "xi": -0.2325,
      "yi": 0.1525,
      "xf": -0.2325,
      "yf": -0.1525
    },
    {
      "xi": 0.0925,
      "yi": -0.21,
      "xf": 0.0925,
      "yf": 0.21
    },
    {
      "xi": 0.2625,
      "yi": -0.115,
      "xf": 0.2625,
      "yf": 0.115
    },
    {
      "xi": 0.0925,
      "yi": -0.21,
      "xf": 0.1625,
      "yf": -0.21
    },
    {
      "xi": 0.0925,
      "yi": 0.21,
      "xf": 0.1625,
      "yf": 0.21
    },
    {
      "xi": 0.1625,
      "yi": -0.21,
      "xf": 0.2255,
      "yf": -0.18
    },
    {
      "xi": 0.1625,
      "yi": 0.21,
      "xf": 0.2255,
      "yf": 0.18
    },
    {
      "xi": 0.2255,
      "yi": -0.18,
      "xf": 0.2625,
      "yf": -0.115
    },
    {
      "xi": 0.2255,
      "yi": 0.18,
      "xf": 0.2625,
      "yf": 0.115
    }
  ],
  "kinematics": {
    "drive": "tc.vrobot.models.DifferentialDrive",
    "vmax": 0.0,
    "umax": 0.0,
    "rmax": 0.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "rwheel": 0.0,
    "skid": 1.0,
    "gear": 60.0,
    "pulses": 500.0,
    "odomET": 0.025,
    "odomER": 0.1,
    "odomBias": 0.1
  },
  "sensors": {
    "son": {
      "rangemax": 5.0,
      "rangemin": 0.135,
      "cone": 20.0,
      "rays": 11,
      "simerror": 0.05,
      "sensors": [
        {
          "rho": 0.253783,
          "theta": 55.8403,
          "height": 0.0,
          "orientation": 90.0,
          "step": 0
        },
        {
          "rho": 0.2759579904623166,
          "theta": 44.522852072591476,
          "height": 0.0,
          "orientation": 60.0,
          "step": 0
        },
        {
          "rho": 0.28537678520674753,
          "theta": 31.23923369493647,
          "height": 0.0,
          "orientation": 30.0,
          "step": 0
        },
        {
          "rho": 0.2682,
          "theta": 11.8336,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.2682,
          "theta": -11.8336,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.28530012059813886,
          "theta": -30.429925248406256,
          "height": 0.0,
          "orientation": -30.0,
          "step": 0
        },
        {
          "rho": 0.2761268289917762,
          "theta": -44.77990987070449,
          "height": 0.0,
          "orientation": -60.0,
          "step": 0
        },
        {
          "rho": 0.253783,
          "theta": -55.8403,
          "height": 0.0,
          "orientation": -90.0,
          "step": 0
        },
        {
          "rho": 0.237828,
          "theta": -140.1173,
          "height": 0.0,
          "orientation": -90.0,
          "step": 0
        },
        {
          "rho": 0.237828,
          "theta": 140.1173,
          "height": 0.0,
          "orientation": 90.0,
          "step": 0
        }
      ]
    },
    "ir": {
      "rangemax": 1.2,
      "rangemin": 0.1,
      "cone": 10.0,
      "rays": 5,
      "simerror": 0.05,
      "sensors": [
        {
          "rho": 0.2768429612219323,
          "theta": 44.48778543370271,
          "height": 0.0,
          "orientation": 60.0,
          "step": 0
        },
        {
          "rho": 0.2853767852067473,
          "theta": 31.239233694936495,
          "height": 0.0,
          "orientation": 30.0,
          "step": 0
        },
        {
          "rho": 0.2682,
          "theta": 11.8336,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.2625,
          "theta": 0.0,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.2682,
          "theta": -11.8336,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.28598454263666606,
          "theta": -30.465563255732164,
          "height": 0.0,
          "orientation": -30.0,
          "step": 0
        },
        {
          "rho": 0.2761250181661291,
          "theta": -44.92663319143499,
          "height": 0.0,
          "orientation": -60.0,
          "step": 0
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
      "sensors": []
    },
    "camera": {
      "sensors": []
    }
  },
  "bumpers": [
    {
      "xi": 0.24,
      "yi": 0.195,
      "xf": 0.24,
      "yf": -0.195
    },
    {
      "xi": 0.24,
      "yi": -0.195,
      "xf": -0.23,
      "yf": -0.15
    },
    {
      "xi": -0.23,
      "yi": -0.15,
      "xf": -0.23,
      "yf": 0.15
    },
    {
      "xi": -0.23,
      "yi": 0.15,
      "xf": 0.24,
      "yf": 0.195
    }
  ],
  "wheels": [
    {
      "x": 0.076,
      "y": 0.18125,
      "z": 0.07435,
      "orientation": 0.0,
      "radius": 0.07435,
      "width": 0.024999999999999998,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 100.0
    },
    {
      "x": 0.076,
      "y": -0.18125,
      "z": 0.07435,
      "orientation": 0.0,
      "radius": 0.07435,
      "width": 0.024999999999999998,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 100.0
    }
  ],
  "groups": [
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.22,
      "theta": 120.0,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 30.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.28,
      "theta": 45.0,
      "height": 0.0,
      "orientation": 45.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 30.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.2625,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 30.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.28,
      "theta": -45.0,
      "height": 0.0,
      "orientation": -45.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 30.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.22,
      "theta": -120.0,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.0,
      "cone": 30.0
    }
  ],
  "fused": [
    {
      "mode": 0,
      "rho": 0.253783,
      "theta": 55.8403,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.272617,
      "theta": 44.1826,
      "height": 0.0,
      "orientation": 60.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.280011,
      "theta": 29.9987,
      "height": 0.0,
      "orientation": 30.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.2682,
      "theta": 11.8336,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.2682,
      "theta": -11.8336,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.280011,
      "theta": -29.9987,
      "height": 0.0,
      "orientation": -30.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": -1,
      "rho": 0.272617,
      "theta": -44.1826,
      "height": 0.0,
      "orientation": -60.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": 0,
      "rho": 0.253783,
      "theta": -55.8403,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": 0,
      "rho": 0.237828,
      "theta": -140.1173,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    },
    {
      "mode": 0,
      "rho": 0.237828,
      "theta": 140.1173,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 5.0,
      "rangemin": 0.0,
      "cone": 17.0
    }
  ],
  "fusionmode": 4,
  "extra": {
    "FILTERVIRTU": "anfis5.filter",
    "SENSIBSON": "0.0000001"
  }
}
