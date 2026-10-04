{
  "name": "ATVR",
  "icon": [
    {
      "xi": 0.51,
      "yi": -0.1925,
      "xf": 0.51,
      "yf": 0.1925
    },
    {
      "xi": -0.39,
      "yi": -0.1925,
      "xf": -0.39,
      "yf": 0.1925
    },
    {
      "xi": -0.35138080999586363,
      "yi": 0.3178973953670299,
      "xf": 0.34938300619384394,
      "yf": 0.3178973953670299
    },
    {
      "xi": -0.35138080999586363,
      "yi": -0.31738749565187,
      "xf": 0.34938300619384394,
      "yf": -0.31738749565187
    },
    {
      "xi": -0.39,
      "yi": 0.1925,
      "xf": -0.35138080999586363,
      "yf": 0.3178973953670299
    },
    {
      "xi": -0.39,
      "yi": -0.1925,
      "xf": -0.35138080999586363,
      "yf": -0.31738749565187
    },
    {
      "xi": 0.51,
      "yi": 0.1925,
      "xf": 0.34938300619384394,
      "yf": 0.3178973953670299
    },
    {
      "xi": 0.34938300619384394,
      "yi": -0.31738749565187,
      "xf": 0.51,
      "yf": -0.1925
    }
  ],
  "kinematics": {
    "drive": "tc.vrobot.models.SkidSteerDrive",
    "vmax": 0.0,
    "umax": 0.0,
    "rmax": 0.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "rwheel": 0.0,
    "skid": 1.0,
    "gear": 0.0,
    "pulses": 0.0,
    "odomET": 5.0E-4,
    "odomER": 1.0E-4,
    "odomBias": 1.0E-4
  },
  "sensors": {
    "son": {
      "rangemax": 10.0,
      "rangemin": 0.135,
      "cone": 20.0,
      "rays": 11,
      "cycle": 1,
      "simerror": 0.05,
      "sensors": [
        {
          "rho": 0.34438,
          "theta": 154.17901,
          "height": 0.0,
          "orientation": 180.0,
          "step": 1
        },
        {
          "rho": 0.33144,
          "theta": 146.0702,
          "height": 0.0,
          "orientation": 90.0,
          "step": 1
        },
        {
          "rho": 0.1978,
          "theta": 69.274444,
          "height": 0.0,
          "orientation": 90.0,
          "step": 1
        },
        {
          "rho": 0.21468,
          "theta": 56.97613,
          "height": 0.0,
          "orientation": 75.0,
          "step": 1
        },
        {
          "rho": 0.24905,
          "theta": 46.95254,
          "height": 0.0,
          "orientation": 60.0,
          "step": 1
        },
        {
          "rho": 0.28901,
          "theta": 37.2664,
          "height": 0.0,
          "orientation": 45.0,
          "step": 1
        },
        {
          "rho": 0.31885,
          "theta": 19.79888,
          "height": 0.0,
          "orientation": 30.0,
          "step": 1
        },
        {
          "rho": 0.30992,
          "theta": 10.22217,
          "height": 0.0,
          "orientation": 15.0,
          "step": 1
        },
        {
          "rho": 0.313,
          "theta": 0.0,
          "height": 0.0,
          "orientation": 0.0,
          "step": 1
        },
        {
          "rho": 0.30922,
          "theta": -10.22217,
          "height": 0.0,
          "orientation": -15.0,
          "step": 1
        },
        {
          "rho": 0.31885,
          "theta": -19.79888,
          "height": 0.0,
          "orientation": -30.0,
          "step": 1
        },
        {
          "rho": 0.34438,
          "theta": -154.17901,
          "height": 0.0,
          "orientation": -180.0,
          "step": 1
        },
        {
          "rho": 0.33144,
          "theta": -146.0702,
          "height": 0.0,
          "orientation": -90.0,
          "step": 1
        },
        {
          "rho": 0.1978,
          "theta": -69.27444,
          "height": 0.0,
          "orientation": -90.0,
          "step": 1
        },
        {
          "rho": 0.21468,
          "theta": -56.97613,
          "height": 0.0,
          "orientation": -75.0,
          "step": 1
        },
        {
          "rho": 0.24905,
          "theta": -46.95254,
          "height": 0.0,
          "orientation": -60.0,
          "step": 1
        },
        {
          "rho": 0.28901,
          "theta": -37.2664,
          "height": 0.0,
          "orientation": -45.0,
          "step": 1
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
          "rho": 0.35,
          "theta": 0.0,
          "height": 0.0,
          "orientation": 0.0,
          "step": 4,
          "rangemax": 82.0,
          "rangemin": 0.01,
          "cone": 180.0,
          "rays": 181
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
      "xi": 0.520724822456032,
      "yi": -0.35000000000000003,
      "xf": 0.520724822456032,
      "yf": 0.35000000000000003
    }
  ],
  "wheels": [
    {
      "x": -0.19,
      "y": 0.27,
      "z": 0.16,
      "orientation": 0.0,
      "radius": 0.16,
      "width": 0.097,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 60.0
    },
    {
      "x": 0.19,
      "y": 0.27,
      "z": 0.16,
      "orientation": 0.0,
      "radius": 0.16,
      "width": 0.097,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 60.0
    },
    {
      "x": -0.19,
      "y": -0.27,
      "z": 0.16,
      "orientation": 0.0,
      "radius": 0.16,
      "width": 0.097,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 60.0
    },
    {
      "x": 0.19,
      "y": -0.27,
      "z": 0.16,
      "orientation": 0.0,
      "radius": 0.16,
      "width": 0.097,
      "steerable": false,
      "maxsteer": 0.0,
      "maxturning": 0.0,
      "traction": true,
      "maxrpm": 60.0
    }
  ],
  "groups": [
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.0,
      "theta": 90.0,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.2,
      "cone": 45.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.0,
      "theta": 45.0,
      "height": 0.0,
      "orientation": 45.0,
      "elevation": 0.0,
      "rangemax": 1.0,
      "rangemin": 0.4,
      "cone": 45.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.0,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 1.25,
      "rangemin": 0.5,
      "cone": 45.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.0,
      "theta": -45.0,
      "height": 0.0,
      "orientation": -45.0,
      "elevation": 0.0,
      "rangemax": 1.0,
      "rangemin": 0.4,
      "cone": 45.0
    },
    {
      "mode": 4,
      "base": 0.3,
      "rho": 0.0,
      "theta": -90.0,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 0.75,
      "rangemin": 0.2,
      "cone": 45.0
    }
  ],
  "fused": [
    {
      "mode": -1,
      "rho": 0.34438,
      "theta": 154.17901,
      "height": 0.0,
      "orientation": 180.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.33144,
      "theta": 146.0702,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.1978,
      "theta": 69.274444,
      "height": 0.0,
      "orientation": 90.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.21468,
      "theta": 56.97613,
      "height": 0.0,
      "orientation": 75.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.24905,
      "theta": 46.95254,
      "height": 0.0,
      "orientation": 60.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.28901,
      "theta": 37.2664,
      "height": 0.0,
      "orientation": 45.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.31885,
      "theta": 19.79888,
      "height": 0.0,
      "orientation": 30.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.30992,
      "theta": 10.22217,
      "height": 0.0,
      "orientation": 15.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.313,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.30922,
      "theta": -10.22217,
      "height": 0.0,
      "orientation": -15.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.31885,
      "theta": -19.79888,
      "height": 0.0,
      "orientation": -30.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.34438,
      "theta": -154.17901,
      "height": 0.0,
      "orientation": -180.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.33144,
      "theta": -146.0702,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.1978,
      "theta": -69.27444,
      "height": 0.0,
      "orientation": -90.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.21468,
      "theta": -56.97613,
      "height": 0.0,
      "orientation": -75.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.24905,
      "theta": -46.95254,
      "height": 0.0,
      "orientation": -60.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    },
    {
      "mode": -1,
      "rho": 0.28901,
      "theta": -37.2664,
      "height": 0.0,
      "orientation": -45.0,
      "elevation": 0.0,
      "rangemax": 10.0,
      "rangemin": 0.0,
      "cone": 20.0
    }
  ],
  "fusionmode": 0,
  "scans": [
    {
      "mode": 0,
      "rays": 90,
      "rho": 0.35,
      "theta": 0.0,
      "height": 0.0,
      "orientation": 0.0,
      "elevation": 0.0,
      "rangemax": 30.0,
      "rangemin": 0.0,
      "cone": 180.0
    }
  ],
  "extra": {
    "CAMERA0_SERVER": "50000",
    "ERRORLRFGAUSS": "0.0009",
    "SENSIBSON": "0.0000001"
  }
}
